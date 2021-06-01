/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.saml.metadata.SAMLMetadataManager;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.LocalDateTimeAttribute;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
public class UserVerificator
{
	private static final Logger log = LogManager.getLogger(UserVerificator.class);

	private final SAMLMetadataManager samlMetadaMan;
	private final UnityApiClient unityClient;
	private final OfflineVerificator offlineVerificator;

	private final DeprovisioningConfiguration config;
	private final UserStatusUpdater userStatusUpdater;
	private final OnlineVerificator onlineVerificator;

	@Autowired
	UserVerificator(SAMLMetadataManager samlMetadaMan, UnityApiClient unityClient,
			OfflineVerificator offlineVerificator, DeprovisioningConfiguration config,
			UserStatusUpdater userStatusUpdater, OnlineVerificator onlineVerificator)
	{

		this.samlMetadaMan = samlMetadaMan;
		this.unityClient = unityClient;
		this.offlineVerificator = offlineVerificator;
		this.config = config;
		this.userStatusUpdater = userStatusUpdater;
		this.onlineVerificator = onlineVerificator;
	}

	public void verifyUsers(Set<UnityUser> extractedUnityUsers)
	{
		log.info("Starting verification of {} users", extractedUnityUsers.size());

		Optional<Map<String, SAMLIdpInfo>> IdpsAsMap = getIDPs();
		if (IdpsAsMap.isEmpty())
			return;

		for (UnityUser user : extractedUnityUsers)
		{
			log.info("Verify user {}", user);

			Optional<Identity> onlineVerifiableIdentity = user
					.getIdentifierIdentityByProfile(config.inputProfilesForOnlineProcessing);
			Optional<Identity> offlineOnlyVerifiableIdentity = user
					.getIdentifierIdentityByProfile(config.inputProfilesForOfflineProcessingOnly);

			if (onlineVerifiableIdentity.isEmpty())
			{
				if (offlineOnlyVerifiableIdentity.isEmpty())
				{
					log.info("Skipping user {} without identifier identity from online or offline processing profiles: {}, {}",
							user, config.inputProfilesForOnlineProcessing,
							config.inputProfilesForOfflineProcessingOnly);
					continue;
				}

				else
				{
					log.info("User {} with identifier identity from offline only verification profile {}",
							user,
							offlineOnlyVerifiableIdentity.get().getTranslationProfile());

					offlineVerificator.verify(user, offlineOnlyVerifiableIdentity.get(),
							config.fallbackOfflineVerificationAdminEmail);
					continue;
				}
			}
			log.debug("User identity associated with online processing profile {}: {}",
					onlineVerifiableIdentity.get().getTranslationProfile(),
					onlineVerifiableIdentity.get());
			SAMLIdpInfo samlIdpInfo = getSamlIdpInfoForUser(IdpsAsMap.get(), user,
					onlineVerifiableIdentity.get()).orElse(null);
			if (samlIdpInfo == null)
			{
				log.info("Can not find IDP with id {} in SAML metadata . Go to offline user verification {} {}",
						onlineVerifiableIdentity.get().getRemoteIdp(), user,
						onlineVerifiableIdentity);
				offlineVerificator.verify(user, onlineVerifiableIdentity.get(),
						config.fallbackOfflineVerificationAdminEmail);
				continue;
			}

			if (samlIdpInfo.attributeQueryServiceUrl.isEmpty())
			{
				log.info("Attribute query service for IDP  {} is not available, only offline verification is possible",
						samlIdpInfo.id);
				offlineVerificator.verify(user, onlineVerifiableIdentity.get(),
						getTechnicalAdminEmailFallbackToDefault(samlIdpInfo));
			} else
			{

				if (!onlineVerificator.verify(user, onlineVerifiableIdentity.get(), samlIdpInfo))
				{
					processOnlineUnverifiedUser(user, onlineVerifiableIdentity.get(), samlIdpInfo);
				}
			}
		}

		log.info("Verification of " + extractedUnityUsers.size() + " users complete");

	}

	private Optional<SAMLIdpInfo> getSamlIdpInfoForUser(Map<String, SAMLIdpInfo> attributeQueryAddressesAsMap,
			UnityUser user, Identity identity)
	{
		SAMLIdpInfo samlIdpInfo = attributeQueryAddressesAsMap.get(identity.getRemoteIdp());
		return Optional.ofNullable(samlIdpInfo);

	}

	private Optional<Map<String, SAMLIdpInfo>> getIDPs()
	{
		try
		{
			return Optional.of(samlMetadaMan.getIDPsAsMap());
		} catch (Exception e)
		{
			log.error("Can not get saml metadata", e);

		}
		return Optional.empty();
	}

	private void processOnlineUnverifiedUser(UnityUser user, Identity identity, SAMLIdpInfo idpInfo)
	{
		log.info("Process online unverified user {} {}", user, identity);

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime firstHomeIdpVerificationFailure = now;

		if (!updateFirstHomeIdpVerificationFailureAttributeIfNeeded(now, user))
		{
			firstHomeIdpVerificationFailure = user.firstHomeIdpVerificationFailure;
		}

		if (checkIsInOnlineOnlyVerificationTime(now, firstHomeIdpVerificationFailure, user, identity))
		{
			return;
		}

		if (offlineVerificator.verify(user, identity, getTechnicalAdminEmailFallbackToDefault(idpInfo)))
		{

			return;
		}

		userStatusUpdater.changeUserStatusIfNeeded(user, identity, EntityState.onlyLoginPermitted, idpInfo);
	}

	String getTechnicalAdminEmailFallbackToDefault(SAMLIdpInfo samlIdpInfo)
	{
		return samlIdpInfo.technicalAdminEmail == null || samlIdpInfo.technicalAdminEmail.isEmpty()
				? config.fallbackOfflineVerificationAdminEmail
				: samlIdpInfo.technicalAdminEmail;
	}

	private boolean updateFirstHomeIdpVerificationFailureAttributeIfNeeded(LocalDateTime now, UnityUser user)
	{
		if (user.firstHomeIdpVerificationFailure == null
				|| user.firstHomeIdpVerificationFailure.isBefore(user.lastAuthenticationTime))
		{

			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.FIRST_HOME_IDP_VERIFICATION_FAILURE_ATTRIBUTE, now));
		} else
		{
			return false;
		}
		return true;
	}

	private boolean checkIsInOnlineOnlyVerificationTime(LocalDateTime now,
			LocalDateTime firstHomeIdpVerificationFailure, UnityUser user, Identity identity)
	{
		if (now.isEqual(firstHomeIdpVerificationFailure) || (now.isAfter(firstHomeIdpVerificationFailure) && now
				.isBefore(firstHomeIdpVerificationFailure.plus(config.onlineOnlyVerificationPeriod))))
		{
			log.info("Skip further verification of user {} {} (is in onlineOnlyVerificationPeriod)", user,
					identity);
			return true;
		}
		return false;
	}
}
