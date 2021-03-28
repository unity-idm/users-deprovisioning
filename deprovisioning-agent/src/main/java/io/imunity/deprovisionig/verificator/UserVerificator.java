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

		Optional<Map<String, SAMLIdpInfo>> attributeQueryAddressesAsMap = getAttributeQueryAddresses();
		if (attributeQueryAddressesAsMap.isEmpty())
			return;

		for (UnityUser user : extractedUnityUsers)
		{
			log.info("Verify user {}", user);
			Optional<Identity> identity = user
					.getIdentifierIdentityByProfile(config.relatedTranslationProfile);
			if (identity.isEmpty())
			{
				log.info("Skipping user {} without identifier identity from profile {}", user.entityId,
						config.relatedTranslationProfile);
				continue;
			}
			log.info("User identity associated with profile {}: {}", config.relatedTranslationProfile,
					identity.get());
			Optional<SAMLIdpInfo> samlIdpInfo = getSamlIdpInfoForUser(attributeQueryAddressesAsMap.get(),
					user, identity.get());
			if (samlIdpInfo.isEmpty())
			{
				log.warn("Can not find IDP with id {} in SAML metadata . Skip user verification {} {}",
						identity.get().getRemoteIdp(), user, identity);
				continue;
			}

			if (samlIdpInfo.get().attributeQueryServiceUrl.isEmpty())
			{

				log.warn("Attribute query service for IDP  {} is not available, only offline verification is possible",
						samlIdpInfo.get().id);
				offlineVerificator.verify(user, identity.get(), samlIdpInfo.get().technicalAdminEmail);
			} else
			{

				if (!onlineVerificator.verify(user, identity.get(), samlIdpInfo.get()))
				{
					processOnlineUnverifiedUser(user, identity.get(), samlIdpInfo.get());
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

	private Optional<Map<String, SAMLIdpInfo>> getAttributeQueryAddresses()
	{
		try
		{
			return Optional.of(samlMetadaMan.getAttributeQueryAddressesAsMap());
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

		if (offlineVerificator.verify(user, identity, idpInfo.technicalAdminEmail))
		{

			return;
		}

		userStatusUpdater.changeUserStatusIfNeeded(user, identity, EntityState.onlyLoginPermitted, idpInfo);
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
