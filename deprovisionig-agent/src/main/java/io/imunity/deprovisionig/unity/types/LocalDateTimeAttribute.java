/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.unity.types;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class LocalDateTimeAttribute
{
	public static Attribute of(String name, LocalDateTime value)
	{
		Attribute attr = new Attribute();
		attr.setGroupPath("/");
		attr.setName(name);
		if (value != null)
		{
			attr.setValues(Arrays.asList(value.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
		}
		return attr;
	}
}
