/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.common;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

public class PropertiesHelper
{
	public static Properties getAllProperties(Environment env)
	{
		Properties props = new Properties();
		MutablePropertySources propSrcs = ((AbstractEnvironment) env).getPropertySources();
		StreamSupport.stream(propSrcs.spliterator(), false)
		        .filter(ps -> ps instanceof EnumerablePropertySource)
		        .map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames())
		        .flatMap(Arrays::<String>stream)
		        .forEach(propName -> props.setProperty(propName, env.getProperty(propName)));
		return props;
	}
}
