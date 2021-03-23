/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common.exception;

public class CredentialConfigurationException extends ConfigurationException
{	
	public CredentialConfigurationException(String msg)
	{
		super(msg);
	}
	
	public CredentialConfigurationException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
