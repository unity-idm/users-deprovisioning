/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.unity.UnityApiClient;
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

	private final DeprovisioningConfiguration config;
	private final UnityApiClient unityClient;

	@Autowired
	OfflineVerificator(UnityApiClient unityClient, DeprovisioningConfiguration config)
	{
		this.unityClient = unityClient;
		this.config = config;

	}

	boolean verify(UnityUser user, Identity identity, String technicalAdminEmail)
	{
		log.info("Attempting offline verification of user {} {}", user, identity);
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
					user, identity);
			return false;
		}

		if (user.lastOfflineVerificationAttempt == null
				|| user.lastOfflineVerificationAttempt.plus(config.emailResendPeriod).isBefore(now))
		{

			LocalDateTime deprovisioningDate = firstOfflineVerificationAttempt
					.plus(config.offlineVerificationPeriod);
			long daysLeft = Duration.between(now, deprovisioningDate).toDays();

			Map<String, String> params = new HashMap<>();
			if (technicalAdminEmail != null)
			{
				params.put(EMAIL_MESSAGE_TEMPLATE_PARAM, technicalAdminEmail);
			}
			params.put(DAYSLEFT_MESSAGE_TEMPLATE_PARAM, String.valueOf(daysLeft));
			params.put(DEPROVISIONING_DATE_MESSAGE_TEMPLATE_PARAM,
					deprovisioningDate.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

			log.info("Sending offline verification email to user {} {}", user, identity);

			unityClient.sendEmail(user.entityId, config.emailTemplate, params);
			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.LAST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE, now));
		} else
		{
			log.info("Skip send email to user {} {} (emailResendPeriod)", user, identity);
		}

		log.debug("Offline verification of {} {} complete", user, identity);
		return true;
	}

}
