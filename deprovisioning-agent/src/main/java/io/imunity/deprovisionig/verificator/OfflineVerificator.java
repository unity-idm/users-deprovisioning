/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.I18nString;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.LocalDateTimeAttribute;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
class OfflineVerificator
{
	private static final Logger log = LogManager.getLogger(OfflineVerificator.class);

	private static final String EMAIL_MESSAGE_TEMPLATE_PARAM = "email";
	private static final String DAYSLEFT_MESSAGE_TEMPLATE_PARAM = "daysLeft";
	private static final String DEPROVISIONING_DATE_MESSAGE_TEMPLATE_PARAM = "deprovisioningDate";
	private static final String IDP_NAME = "idpName";

	private final DeprovisioningConfiguration config;
	private final UnityApiClient unityClient;

	@Autowired
	OfflineVerificator(UnityApiClient unityClient, DeprovisioningConfiguration config)
	{
		this.unityClient = unityClient;
		this.config = config;

	}

	boolean verify(UnityUser user, IdentitiesFromSingleIdp identitiesSingleIdp)
	{		
		log.info("Attempting offline verification of user {} (with identities {})", user, identitiesSingleIdp.identities);
		LocalDateTime now = LocalDateTime.now();

		LocalDateTime firstOfflineVerificationAttempt = now;
		if (user.firstOfflineVerificationAttempt == null
				|| user.firstOfflineVerificationAttempt.isBefore(user.getLastAuthenticationTimeFallbackToDefault()))
		{
			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.FIRST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE, now));
		} else
		{
			firstOfflineVerificationAttempt = user.firstOfflineVerificationAttempt;
		}

		if (!now.isEqual(firstOfflineVerificationAttempt) && (now.isBefore(firstOfflineVerificationAttempt)
				|| now.isAfter(firstOfflineVerificationAttempt.plus(config.offlineVerificationPeriod))))
		{
			log.info("Skip offline verification for user {} {} (offlineVerificationPeriod has passed)",
					user, identitiesSingleIdp.identities);
			return false;
		}

		if (user.lastOfflineVerificationAttempt == null
				|| user.lastOfflineVerificationAttempt.plus(config.emailResendPeriod).isBefore(now))
		{

			LocalDateTime deprovisioningDate = firstOfflineVerificationAttempt
					.plus(config.offlineVerificationPeriod);
			long daysLeft = Duration.between(now, deprovisioningDate).toDays();

			String technicalAdminEmail = getTechnicalAdminEmailFallbackToDefault(identitiesSingleIdp.idpInfo);
			
			Map<String, String> params = new HashMap<>();
			if (technicalAdminEmail != null)
			{
				params.put(EMAIL_MESSAGE_TEMPLATE_PARAM, technicalAdminEmail);
			}
			params.put(DAYSLEFT_MESSAGE_TEMPLATE_PARAM, String.valueOf(daysLeft));
			params.put(DEPROVISIONING_DATE_MESSAGE_TEMPLATE_PARAM,
					deprovisioningDate.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			params.put(IDP_NAME, getIdpNameForEmailMessage(identitiesSingleIdp.identities,
					Optional.ofNullable(identitiesSingleIdp.idpInfo)
					.map(i -> i.name)
					.orElse(null)));
			
			log.info("Sending offline verification email to user {} (with identities {})", user, identitiesSingleIdp.identities);

			unityClient.sendEmail(user.entityId, config.emailTemplate, params);
			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.LAST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE, now));
		} else
		{
			log.info("Skip send email to user {} (with identities {}) (emailResendPeriod)", user, identitiesSingleIdp.identities);
		}

		log.debug("Offline verification of user {} (with identities {}) complete", user, identitiesSingleIdp.identities);
		return true;
	}

	String getTechnicalAdminEmailFallbackToDefault(SAMLIdpInfo samlIdpInfo)
	{
		return samlIdpInfo == null || samlIdpInfo.technicalAdminEmail == null || samlIdpInfo.technicalAdminEmail.isEmpty()
				? config.fallbackOfflineVerificationAdminEmail
				: samlIdpInfo.technicalAdminEmail;
	}

	
	private String getIdpNameForEmailMessage(List<Identity> ids, I18nString idpName)
	{
		if (idpName != null)
		{
			if (idpName.getValue("en") != null)
			{
				return idpName.getValue("en");
			} else if (idpName.getValue("de") != null)
			{
				return idpName.getValue("de");
			}

			if (idpName.getLocalizedMap() != null && !idpName.getLocalizedMap()
					.isEmpty())
			{
				return idpName.getLocalizedMap()
						.values()
						.stream()
						.findFirst()
						.get();
			}
		}

		String remoteIdp = ids.iterator().next().getRemoteIdp();
		if (config.idpNames != null && remoteIdp != null && config.idpNames.containsKey(remoteIdp))
		{
			return config.idpNames.get(remoteIdp);
		}

		log.debug("No name found for IdP {}, using id as fallback", remoteIdp);

		return ids.iterator().next().getRemoteIdp();
	}
}
