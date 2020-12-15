/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.verificator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.TimesConfiguration;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.Attribute;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
public class OfflineVerificator
{
	private static final Logger log = LogManager.getLogger(OfflineVerificator.class);
	
	@Value("${unity.email.template:userDeprovisioning}")
	private String emailTemplate;
	
	
	private final TimesConfiguration timesConfig;
	private final UnityApiClient unityClient;
	
	public OfflineVerificator(UnityApiClient unityClient, TimesConfiguration timesConfig)
	{
		this.unityClient = unityClient;
		this.timesConfig = timesConfig;
	}
	
	public void verify(UnityUser user, String technicalAdminEmail)
	{
		log.info("Goto offline verification with user " + user.entityId);	
		LocalDateTime now = LocalDateTime.now();
		
		if (user.lastOfflineVerification == null || user.lastOfflineVerification.plusDays(timesConfig.getEmailResendPeriod()).isBefore(now))
		{
			//TODO PARAMS
			//the days left, 
			//the date of deprovision

			Map<String, String> params = new HashMap<>();
			if (technicalAdminEmail != null)
			{
				params.put("email", technicalAdminEmail);
			}
			
			unityClient.sendEmail(String.valueOf(user.entityId), emailTemplate, params);
			
			Attribute attr = new Attribute();
			attr.setGroupPath("/");
			attr.setName(Constans.LAST_OFFLINE_VERIFICATION_ATTRIBUTE);
			attr.setValues(Arrays
					.asList(now.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
			unityClient.updateAttribute(String.valueOf(user.entityId), attr);		
		}
	}

}
