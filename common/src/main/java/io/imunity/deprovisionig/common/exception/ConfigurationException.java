/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.common.exception;

public class ConfigurationException extends RuntimeException
{
	public ConfigurationException(String msg)
	{
		super(msg);
	}
	
	public ConfigurationException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
