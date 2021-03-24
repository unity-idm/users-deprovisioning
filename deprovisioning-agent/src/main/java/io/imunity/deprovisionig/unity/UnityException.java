/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity;


public class UnityException extends Exception
{
	public UnityException(String msg)
	{
		super(msg);
	}

	public UnityException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public UnityException(Throwable cause)
	{
		super("", cause);
	}
}
