/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TimesConfiguration
{
	public final Duration validAccountPeriod;
	public final Duration onlineOnlyVerificationPeriod;
	public final Duration offlineVerificationPeriod;
	public final Duration emailResendPeriod;
	public final Duration removeUserCompletlyPeriod;

	public TimesConfiguration(@Value("${time.validAccountPeriod:14}") Duration validAccountPeriod,
			@Value("${time.onlineOnlyVerificationPeriod:14}") Duration onlineOnlyVerificationPeriod,
			@Value("${time.offlineVerificationPeriod:14}") Duration offlineVerificationPeriod,
			@Value("${time.emailResendPeriod:10}") Duration emailResendPeriod,
			@Value("${time.removeUserCompletlyPeriod:10}") Duration removeUserCompletlyPeriod)

	{
		this.validAccountPeriod = validAccountPeriod;
		this.onlineOnlyVerificationPeriod = onlineOnlyVerificationPeriod;
		this.offlineVerificationPeriod = offlineVerificationPeriod;
		this.emailResendPeriod = emailResendPeriod;
		this.removeUserCompletlyPeriod = removeUserCompletlyPeriod;
	}
}
