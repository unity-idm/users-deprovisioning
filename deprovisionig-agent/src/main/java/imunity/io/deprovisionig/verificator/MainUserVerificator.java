/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package imunity.io.deprovisionig.verificator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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
import imunity.io.deprovisionig.Constans;
import imunity.io.deprovisionig.TimesConfiguration;
import imunity.io.deprovisionig.exception.SAMLException;
import imunity.io.deprovisionig.hook.GroovyHookExecutor;
import imunity.io.deprovisionig.saml.AttributeQueryClient;
import imunity.io.deprovisionig.saml.metadata.SAMLIdpInfo;
import imunity.io.deprovisionig.saml.metadata.SAMLMetadataManager;
import imunity.io.deprovisionig.unity.UnityApiClient;
import imunity.io.deprovisionig.unity.types.Attribute;
import imunity.io.deprovisionig.unity.types.EntityState;
import imunity.io.deprovisionig.unity.types.Identity;
import imunity.io.deprovisionig.unity.types.UnityUser;

@Component
public class MainUserVerificator
{
	private static final Logger log = LogManager.getLogger(MainUserVerificator.class);
	private static final String SAML_STATUS_ATTRIBUTE_NAME = "1.3.6.1.4.1.25178.1.2.19";
	private static final String SAML_STATUS_ATTRIBUTE_SCHAC_PREFIX = "urn:schac:userStatus:";

	private final AttributeQueryClient samlAttrQueryClient;
	private final SAMLMetadataManager samlMetadaMan;
	private final UnityApiClient unityClient;
	private final OfflineVerificator offlineVerificator;
	private final GroovyHookExecutor groovyHook;

	@Value("${unity.identifier.relatedProfile}")
	private String relatedTranslationProfile;
	private TimesConfiguration timesConfig;

	@Autowired
	MainUserVerificator(AttributeQueryClient samlAttrQueryClient, SAMLMetadataManager samlMetadaMan,
			UnityApiClient unityClient, TimesConfiguration timesConfig,
			OfflineVerificator offlineVerificator, GroovyHookExecutor groovyHook)
	{
		this.samlAttrQueryClient = samlAttrQueryClient;
		this.samlMetadaMan = samlMetadaMan;
		this.unityClient = unityClient;
		this.timesConfig = timesConfig;
		this.offlineVerificator = offlineVerificator;
		this.groovyHook = groovyHook;
	}

	public void verifyUsers(Set<UnityUser> extractUnityUsers)
	{
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

			log.info("Online verify user " + user.entityId);
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
				// gotoOfflineVerification(user);
				continue;
			}

			Optional<List<ParsedAttribute>> attributes;
			try
			{
				attributes = samlAttrQueryClient.query(samlIdpInfo.attributeQueryServiceUrl,
						identity.get().getValue());

			} catch (SAMLException e)
			{
				SAMLErrorResponseException cause = (SAMLErrorResponseException) e.getCause();
				if (SubStatus.STATUS2_UNKNOWN_PRINCIPIAL.equals(cause.getSamlSubErrorId()))
				{
					changeUserStatusIfNeeded(user, EntityState.toRemove, samlIdpInfo);
				} else
				{
					gotoOfflineVerification(user, samlIdpInfo);

				}
				continue;
			} catch (Exception e)
			{
				gotoOfflineVerification(user, samlIdpInfo);
				continue;
			}

			changeUserStatusIfNeeded(user, getStatusFromAttributesOrFallbackToUserStatus(user, attributes),
					samlIdpInfo);
			updateLastHomeIdpVerificationTime(user);

		}

	}

	private EntityState getStatusFromAttributesOrFallbackToUserStatus(UnityUser user,
			Optional<List<ParsedAttribute>> attributes)
	{
		if (attributes.isEmpty())
		{
			return user.entityState;
		}

		Optional<ParsedAttribute> statusAttr = attributes.get().stream()
				.filter(a -> a.getName().equals(SAML_STATUS_ATTRIBUTE_NAME)
						|| a.getName().startsWith(SAML_STATUS_ATTRIBUTE_SCHAC_PREFIX))
				.findAny();

		return mapToUnityStatusOrFallbackToUserStatus(user, statusAttr);
	}

	private void updateLastHomeIdpVerificationTime(UnityUser user)
	{
		log.debug("Update last idp verification for user " + user.entityId);
		Attribute attr = new Attribute();
		attr.setGroupPath("/");
		attr.setName(Constans.LAST_HOME_IDP_VERIFICATION_ATTRIBUTE);
		attr.setValues(Arrays
				.asList(LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

		unityClient.updateAttribute(String.valueOf(user.entityId), attr);

	}

	private EntityState mapToUnityStatusOrFallbackToUserStatus(UnityUser user, Optional<ParsedAttribute> status)
	{
		if (status.isEmpty() || status.get().getStringValues().isEmpty())
		{
			log.debug("No status attributes for user " + user.entityId);
			return user.entityState;
		}

		String statusL = status.get().getStringValues().get(0).toLowerCase();

		if ("active".equals(statusL))
		{
			return EntityState.valid;
		} else if ("locked".equals(statusL))
		{
			return EntityState.authenticationDisabled;
		} else if ("disabled".equals(statusL))
		{
			return EntityState.disabled;
		} else if ("deleted".equals(statusL))
		{
			return EntityState.onlyLoginPermitted;
		}

		log.debug("Can not interpret new status of user " + user.entityId + " status=" + statusL);
		return user.entityState;

	}

	private void changeUserStatusIfNeeded(UnityUser user, EntityState newStatus, SAMLIdpInfo idpInfo)
	{
		if (user.entityState.equals(newStatus))
		{
			return;
		}

		log.info("Change user status of " + user.entityId + " to " + newStatus.toString());

		Instant time = null;
		if (newStatus.equals(EntityState.onlyLoginPermitted))
		{
			time = getRemoveTime();
			unityClient.scheduleRemoveUserWithLoginPermit(String.valueOf(user.entityId),
					time.toEpochMilli());
		}
		if (newStatus.equals(EntityState.toRemove))
		{
			time = getRemoveTime();
			unityClient.setUserStatus(String.valueOf(user.entityId), EntityState.disabled);
			unityClient.scheduleRemoveUser(String.valueOf(user.entityId), time.toEpochMilli());

		} else
		{
			unityClient.setUserStatus(String.valueOf(user.entityId), newStatus);
		}

		groovyHook.run(user, newStatus, idpInfo, time);
	}

	private Instant getRemoveTime()
	{
		return Instant.now().plus(timesConfig.removeUserCompletlyPeriod(), ChronoUnit.DAYS);
	}

	private void gotoOfflineVerification(UnityUser user, SAMLIdpInfo idpInfo)
	{

		LocalDateTime now = LocalDateTime.now();
		if (user.lastHomeIdPVerification == null
				|| user.lastHomeIdPVerification.plusDays(timesConfig.gracePeriod1()).isAfter(now)
				|| user.entityState.equals(EntityState.disabled))
		{
			return;
		}

		if (user.lastHomeIdPVerification.plusDays(timesConfig.gracePeriod1()).isBefore(now)
				&& user.lastHomeIdPVerification.plusDays(timesConfig.gracePeriod2()).isAfter(now))
		{
			offlineVerificator.verify(user, idpInfo.technicalAdminEmail);
		}


		changeUserStatusIfNeeded(user, EntityState.onlyLoginPermitted, idpInfo);
	}

}
