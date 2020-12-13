/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package imunity.io.deprovisionig.exception;

public class InternalException extends Exception
{

	public InternalException()
	{
	}

	public InternalException(String msg)
	{
		super(msg);
	}

	public InternalException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public InternalException(Throwable cause)
	{
		super("", cause);
	}

}
