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
	UserVerificator(SAMLMetadataManager samlMetadaMan,
			UnityApiClient unityClient, OfflineVerificator offlineVerificator,
			DeprovisioningConfiguration config, UserStatusUpdater userStatusUpdater,
			OnlineVerificator onlineVerificator)
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
		log.info("Starting verification of " + extractedUnityUsers.size() + " users");
		
		Optional<Map<String, SAMLIdpInfo>> attributeQueryAddressesAsMap = getAttributeQueryAddresses();
		if (attributeQueryAddressesAsMap.isEmpty())
			return;
	
		for (UnityUser user : extractedUnityUsers)
		{
			log.debug("Verify user " + user.entityId);
			Optional<Identity> identity = user
					.getIdentifierIdentityByProfile(config.relatedTranslationProfile);
			if (identity.isEmpty())
			{
				log.debug("Skippin user " + user.entityId + " without identifier identity from profile "
						+ config.relatedTranslationProfile);
				continue;
			}
			log.debug("Associated identity " + identity.get());
			Optional<SAMLIdpInfo> samlIdpInfo = getSamlIdpInfoForUser(attributeQueryAddressesAsMap.get(), user, identity.get());
			if (samlIdpInfo.isEmpty())
			{
				continue;
			}
			
			if (samlIdpInfo.get().attributeQueryServiceUrl.isEmpty())
			{
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

	private Optional<SAMLIdpInfo> getSamlIdpInfoForUser(Map<String, SAMLIdpInfo> attributeQueryAddressesAsMap, UnityUser user, Identity identity)
	{
		SAMLIdpInfo samlIdpInfo = attributeQueryAddressesAsMap.get(identity.getRemoteIdp());
		if (samlIdpInfo == null)
		{
			log.debug("Can not find attribute query service address for IDP entity "
					+ identity.getRemoteIdp() + ". Skip user verification "
					+ user.entityId);
		}
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
		log.debug("Process online unverified user " + identity);

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime firstHomeIdpVerificationFailure = now;

		if (!updateFirstHomeIdpVerificationFailureAttributeIfNeeded(now, user))
		{
			firstHomeIdpVerificationFailure = user.firstHomeIdpVerificationFailure;
		}

		if (checkIsInOnlineOnlyVerificationTime(now, firstHomeIdpVerificationFailure, user))
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
	
	private boolean checkIsInOnlineOnlyVerificationTime(LocalDateTime now, LocalDateTime firstHomeIdpVerificationFailure, UnityUser user)
	{
		if (now.isEqual(firstHomeIdpVerificationFailure) || (now.isAfter(firstHomeIdpVerificationFailure) && now
				.isBefore(firstHomeIdpVerificationFailure.plus(config.onlineOnlyVerificationPeriod))))
		{
			log.debug("Skip further verification of user " + user.entityId
					+ "(is in onlineOnlyVerificationPeriod)");
			return true;
		}
		return false;
	}
}
