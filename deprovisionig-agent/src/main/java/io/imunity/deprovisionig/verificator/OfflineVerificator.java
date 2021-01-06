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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.TimesConfiguration;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.LocalDateTimeAttribute;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
public class OfflineVerificator
{
	private static final Logger log = LogManager.getLogger(OfflineVerificator.class);

	private final String emailTemplate;
	private final TimesConfiguration timesConfig;
	private final UnityApiClient unityClient;

	@Autowired
	public OfflineVerificator(UnityApiClient unityClient, TimesConfiguration timesConfig,
			@Value("${unity.email.template:userDeprovisioning}") String emailTemplate)
	{
		this.unityClient = unityClient;
		this.timesConfig = timesConfig;
		this.emailTemplate = emailTemplate;
	}

	public boolean verify(UnityUser user, String technicalAdminEmail)
	{
		log.debug("Goto offline verification with user " + user.entityId);
		LocalDateTime now = LocalDateTime.now();

		LocalDateTime firstOfflineVerificationAttempt = now;
		if (user.firstOfflineVerificationAttempt == null
				|| user.firstOfflineVerificationAttempt.isBefore(user.lastAuthenticationTime))
		{
			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.FIRST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE, now));
		} else
		{
			firstOfflineVerificationAttempt = user.firstOfflineVerificationAttempt;
		}

		if (!now.isEqual(firstOfflineVerificationAttempt) && (now.isBefore(firstOfflineVerificationAttempt)
				|| now.isAfter(firstOfflineVerificationAttempt
						.plus(timesConfig.offlineVerificationPeriod))))
		{
			log.debug("Skip offline verification for user " + user.entityId + "(offlineVerificationPeriod has passed)");
			return false;
		}

		if (user.lastOfflineVerificationAttempt == null || user.lastOfflineVerificationAttempt
				.plus(timesConfig.emailResendPeriod).isBefore(now))
		{

			LocalDateTime deprovisioningDate = firstOfflineVerificationAttempt
					.plus(timesConfig.offlineVerificationPeriod);
			long daysLeft = Duration.between(now, deprovisioningDate).toDays();

			Map<String, String> params = new HashMap<>();
			if (technicalAdminEmail != null)
			{
				params.put("email", technicalAdminEmail);
			}
			params.put("daysLeft", String.valueOf(daysLeft));
			params.put("deprovisionigDate",
					deprovisioningDate.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

			unityClient.sendEmail(user.entityId, emailTemplate, params);
			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.LAST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE, now));
		}else
		{
			log.debug("Skip send email to user " + user.entityId + " (emailResendPeriod)");
		}

		log.debug("Offline verification of " + user.entityId + " complete");
		return true;
	}

}
