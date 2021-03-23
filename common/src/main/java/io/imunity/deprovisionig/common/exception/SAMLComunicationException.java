/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common.exception;

public class SAMLComunicationException extends RuntimeException
{
	public SAMLComunicationException(String msg)
	{
		super(msg);
	}

	public SAMLComunicationException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
