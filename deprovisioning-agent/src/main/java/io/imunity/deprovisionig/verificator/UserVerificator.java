/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.time.LocalDateTime;
import java.util.List;
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

			IdentitiesFromSingleIdp onlineVerifiableIdentities;
			try
			{
				 onlineVerifiableIdentities = new IdentitiesFromSingleIdp(
						 user
							.getIdentifiersIdentityByProfile(config.inputProfilesForOnlineProcessing), IdpsAsMap.get());
			} catch (IllegalArgumentException e)
			{
				log.warn("Skipping user {} with multiple remote IDPs associated with online processing profile: {}",
						user, config.inputProfilesForOnlineProcessing);
				continue;
			}
			Optional<Identity> offlineOnlyVerifiableIdentity = user
					.getIdentifierIdentityByProfile(config.inputProfilesForOfflineProcessingOnly);

			if (onlineVerifiableIdentities.isEmpty())
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

					
					processOfflineOnlyVerification(user,
							new IdentitiesFromSingleIdp(List.of(offlineOnlyVerifiableIdentity.get()), IdpsAsMap.get()));
					continue;
				}
			}
			
			log.debug("User identities associated with online processing profile {}: {}",
					onlineVerifiableIdentities.translationProfile(),
					onlineVerifiableIdentities.identities);
			
			if (onlineVerifiableIdentities.idpInfo == null)
			{
				log.info("Can not find IDP with id {} in SAML metadata . Go to offline user verification {} {}",
						onlineVerifiableIdentities.remoteIdp(), user,
						onlineVerifiableIdentities.identities);
				processOfflineOnlyVerification(user, onlineVerifiableIdentities);
				continue;
			}

			if (onlineVerifiableIdentities.idpInfo.attributeQueryServiceUrl.isEmpty())
			{
				log.info("Attribute query service for IDP  {} is not available, only offline verification is possible",
						onlineVerifiableIdentities.idpInfo.id);
				processOfflineOnlyVerification(user, onlineVerifiableIdentities);
				
			} else
			{
				OnlineVerificationStatus onlineVerificationStatus =  onlineVerificator.verify(user, onlineVerifiableIdentities);
				
				if (!onlineVerificationStatus.equals(OnlineVerificationStatus.success))
				{
					processOnlineUnverifiedUser(user, onlineVerifiableIdentities);
				}
			}
		}

		log.info("Verification of " + extractedUnityUsers.size() + " users complete");

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

	private void processOnlineUnverifiedUser(UnityUser user, IdentitiesFromSingleIdp identitiesFromOneIdp)
	{
		log.info("Process online unverified user {} {}", user, identitiesFromOneIdp.identities);

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime firstHomeIdpVerificationFailure = now;

		if (!updateFirstHomeIdpVerificationFailureAttributeIfNeeded(now, user))
		{
			firstHomeIdpVerificationFailure = user.firstHomeIdpVerificationFailure;
		}

		if (checkIsInOnlineOnlyVerificationTime(now, firstHomeIdpVerificationFailure, user, identitiesFromOneIdp.identities))
		{
			return;
		}

		processOfflineOnlyVerification(user, identitiesFromOneIdp);
	}
	
	private void processOfflineOnlyVerification(UnityUser user, IdentitiesFromSingleIdp identitiesFromOneIdp)
	{
		if (offlineVerificator.verify(user, identitiesFromOneIdp))
		{
			return;
		}

		userStatusUpdater.changeUserStatusIfNeeded(user, identitiesFromOneIdp, EntityState.onlyLoginPermitted);
	}
	
	private boolean updateFirstHomeIdpVerificationFailureAttributeIfNeeded(LocalDateTime now, UnityUser user)
	{
		if (user.firstHomeIdpVerificationFailure == null
				|| user.firstHomeIdpVerificationFailure.isBefore(user.getLastAuthenticationTimeFallbackToDefault()))
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
			LocalDateTime firstHomeIdpVerificationFailure, UnityUser user, List<Identity> identities)
	{
		if (now.isEqual(firstHomeIdpVerificationFailure) || (now.isAfter(firstHomeIdpVerificationFailure) && now
				.isBefore(firstHomeIdpVerificationFailure.plus(config.onlineOnlyVerificationPeriod))))
		{
			log.info("Skip further verification of user {} {} (is in onlineOnlyVerificationPeriod)", user,
					identities);
			return true;
		}
		return false;
	}
}
