/*
a	a * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.SAMLConstants.SubStatus;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.exceptions.SAMLErrorResponseException;
import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.TimesConfiguration;
import io.imunity.deprovisionig.common.exception.SAMLException;
import io.imunity.deprovisionig.common.saml.AttributeQueryClient;
import io.imunity.deprovisionig.hook.GroovyHookExecutor;
import io.imunity.deprovisionig.saml.StatusAttributeExtractor;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.saml.metadata.SAMLMetadataManager;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.LocalDateTimeAttribute;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
public class MainUserVerificator
{
	private static final Logger log = LogManager.getLogger(MainUserVerificator.class);

	private final AttributeQueryClient samlAttrQueryClient;
	private final SAMLMetadataManager samlMetadaMan;
	private final UnityApiClient unityClient;
	private final OfflineVerificator offlineVerificator;
	private final GroovyHookExecutor groovyHook;

	private final String relatedTranslationProfile;
	private final TimesConfiguration timesConfig;

	@Autowired
	public MainUserVerificator(AttributeQueryClient samlAttrQueryClient, SAMLMetadataManager samlMetadaMan,
			UnityApiClient unityClient, TimesConfiguration timesConfig,
			OfflineVerificator offlineVerificator, GroovyHookExecutor groovyHook,
			@Value("${unity.identifier.relatedProfile}") String relatedTranslationProfile)
	{
		this.samlAttrQueryClient = samlAttrQueryClient;
		this.samlMetadaMan = samlMetadaMan;
		this.unityClient = unityClient;
		this.timesConfig = timesConfig;
		this.offlineVerificator = offlineVerificator;
		this.groovyHook = groovyHook;
		this.relatedTranslationProfile = relatedTranslationProfile;
	}

	public void verifyUsers(Set<UnityUser> extractUnityUsers)
	{
		log.info("Starting verify " + extractUnityUsers.size() + " users");
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

			log.debug("Online verify user " + user.entityId);
			Optional<Identity> identity = user.getIdentifierIdentityByProfile(relatedTranslationProfile);
			if (identity.isEmpty())
			{
				log.debug("Skip user " + user.entityId + " without identifier identity from profile "
						+ relatedTranslationProfile);
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

			Optional<List<ParsedAttribute>> attributes;
			try
			{
				attributes = samlAttrQueryClient.getAttributes(samlIdpInfo.attributeQueryServiceUrl,
						identity.get().getValue());

			} catch (SAMLException e)
			{
				SAMLErrorResponseException cause = (SAMLErrorResponseException) e.getCause();
				if (SubStatus.STATUS2_UNKNOWN_PRINCIPIAL.equals(cause.getSamlSubErrorId()))
				{
					changeUserStatusIfNeeded(user, EntityState.toRemove, samlIdpInfo);
				} else
				{
					processOnlineUnverifiedUser(user, samlIdpInfo);

				}
				continue;
			} catch (Exception e)
			{
				processOnlineUnverifiedUser(user, samlIdpInfo);
				continue;
			}

			changeUserStatusIfNeeded(user, StatusAttributeExtractor
					.getStatusFromAttributesOrFallbackToUserStatus(user, attributes), samlIdpInfo);
			updateLastSuccessVerificationTime(user);
			log.debug("Online verification successfull for user " + user.entityId);

		}
		log.info("Verification of " + extractUnityUsers.size() + "users complete");

	}

	private void updateLastSuccessVerificationTime(UnityUser user)
	{
		unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
				.of(Constans.LAST_SUCCESS_HOME_IDP_VERIFICATION_ATTRIBUTE, LocalDateTime.now()));

		if (user.firstHomeIdpVerificationFailure != null)
		{
			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.FIRST_HOME_IDP_VERIFICATION_FAILURE_ATTRIBUTE, null));
		}

		if (user.firstOfflineVerificationAttempt != null)
		{
			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.FIRST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE, null));
		}
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
		if (now.isEqual(firstHomeIdpVerificationFailure) || (now.isAfter(firstHomeIdpVerificationFailure)
				&& now.isBefore(firstHomeIdpVerificationFailure
						.plus(timesConfig.onlineOnlyVerificationPeriod))))
		{
			log.debug("Skip further verification of user " + user.entityId + "(is in onlineOnlyVerificationPeriod)");
			return;
		}

		if (offlineVerificator.verify(user, idpInfo.technicalAdminEmail))
		{

			return;
		}

		changeUserStatusIfNeeded(user, EntityState.onlyLoginPermitted, idpInfo);
	}

	private void changeUserStatusIfNeeded(UnityUser user, EntityState newStatus, SAMLIdpInfo idpInfo)
	{
		if (user.entityState.equals(newStatus))
		{
			log.debug("User status of " + user.entityId + " remains unchanged (" + user.entityState + ")");
			return;
		}

		log.debug("Change user status of " + user.entityId + " to " + newStatus.toString());

		Instant time = null;
		if (newStatus.equals(EntityState.onlyLoginPermitted))
		{
			time = getRemoveTime();
			unityClient.scheduleRemoveUserWithLoginPermit(user.entityId, time.toEpochMilli());
		} else if (newStatus.equals(EntityState.toRemove))
		{
			time = getRemoveTime();
			unityClient.setUserStatus(user.entityId, EntityState.disabled);
			unityClient.scheduleRemoveUser(user.entityId, time.toEpochMilli());

		} else
		{
			unityClient.setUserStatus(user.entityId, newStatus);
		}

		groovyHook.runHook(user, newStatus, idpInfo, time);
	}

	private Instant getRemoveTime()
	{
		return Instant.now().plus(timesConfig.removeUserCompletlyPeriod);
	}

}
