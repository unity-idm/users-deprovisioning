/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package imunity.io.deprovisionig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class TimesConfiguration
{
	@Value("${time.timetolive:14}")
	private int timetolive;
	
	@Value("${time.gracePeriod1:14}")
	private int gracePeriod1;
	
	@Value("${time.gracePeriod2:14}")
	private int gracePeriod2;
	
	@Value("${time.emailResendPeriod:10}")
	private int emailResendPeriod;
	
	@Value("${time.removeUserCompletlyPeriod:10}")
	private int removeUserCompletlyPeriod;
	
	public int ttl()
	{
		return timetolive;
	}
	
	public int gracePeriod1()
	{
		return timetolive + gracePeriod1;
	}
	
	public int gracePeriod2()
	{
		return timetolive + gracePeriod1 + gracePeriod2;
	}
	
	public int getEmailResendPeriod()
	{
		return emailResendPeriod;
	}
	
	public int removeUserCompletlyPeriod()
	{
		return removeUserCompletlyPeriod;
	}

	
}
