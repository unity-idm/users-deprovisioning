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

	public void verifyUsers(Set<UnityUser> extractUnityUsers)
	{
		log.info("Starting verification of " + extractUnityUsers.size() + " users");
		Map<String, SAMLIdpInfo> attributeQueryAddressesAsMap = null;
		try
		{
			attributeQueryAddressesAsMap = samlMetadaMan.getAttributeQueryAddressesAsMap();
		} catch (Exception e)
		{
			log.error("Can not get saml metadata", e);
			return;
		}

		for (UnityUser user : extractUnityUsers)
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

			SAMLIdpInfo samlIdpInfo = attributeQueryAddressesAsMap.get(identity.get().getRemoteIdp());
			if (samlIdpInfo == null)
			{

				log.debug("Can not find attribute query service address for IDP entity "
						+ identity.get().getRemoteIdp() + ". Skip user verification "
						+ user.entityId);
				continue;
			}

			if (!onlineVerificator.verify(user, identity.get(), samlIdpInfo))
			{
				processOnlineUnverifiedUser(user, samlIdpInfo);
			}
		}

		log.info("Verification of " + extractUnityUsers.size() + "users complete");

	}

	private void processOnlineUnverifiedUser(UnityUser user, SAMLIdpInfo idpInfo)
	{
		log.debug("Process online unverified user " + user.entityId);

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime firstHomeIdpVerificationFailure = now;

		if (user.firstHomeIdpVerificationFailure == null
				|| user.firstHomeIdpVerificationFailure.isBefore(user.lastAuthenticationTime))
		{

			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.FIRST_HOME_IDP_VERIFICATION_FAILURE_ATTRIBUTE, now));
		} else
		{
			firstHomeIdpVerificationFailure = user.firstHomeIdpVerificationFailure;
		}

		// Online only verification time
		if (now.isEqual(firstHomeIdpVerificationFailure) || (now.isAfter(firstHomeIdpVerificationFailure) && now
				.isBefore(firstHomeIdpVerificationFailure.plus(config.onlineOnlyVerificationPeriod))))
		{
			log.debug("Skip further verification of user " + user.entityId
					+ "(is in onlineOnlyVerificationPeriod)");
			return;
		}

		if (offlineVerificator.verify(user, idpInfo.technicalAdminEmail))
		{

			return;
		}

		userStatusUpdater.changeUserStatusIfNeeded(user, EntityState.onlyLoginPermitted, idpInfo);
	}
}
