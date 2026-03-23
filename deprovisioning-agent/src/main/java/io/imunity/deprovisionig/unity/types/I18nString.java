/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.deprovisionig.unity.types;

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class I18nString
{
	private final Map<String, String> values;
	private String defaultValue;

	public I18nString()
	{
		this.values = new HashMap<>();
	}

	public I18nString(Map<Locale, String> values)
	{
		this();
		addAllMapValues(values);
	}

	public I18nString(String defaultValue)
	{
		this();
		this.defaultValue = defaultValue;
	}

	public I18nString(String locale, String value)
	{
		this();
		if (locale == null)
			this.defaultValue = value;
		else
			this.values.put(locale, value);
	}

	public String getValue(String locale, String defaultLocale)
	{
		return (locale != null && values.containsKey(locale)) ? values.get(locale)
				: (defaultLocale != null && values.containsKey(defaultLocale)) ? values.get(defaultLocale)
						: defaultValue;
	}

	public String getValue(String locale)
	{
		return (locale != null && values.containsKey(locale)) ? values.get(locale) : defaultValue;
	}

	public String getValueRaw(String locale)
	{
		return values.get(locale);
	}

	public void addValue(String locale, String value)
	{
		if (locale != null)
			values.put(locale, value);
		else
			setDefaultValue(value);
	}

	public void addAllValues(Map<String, String> values)
	{
		this.values.putAll(values);
	}

	public void addAllMapValues(Map<Locale, String> values)
	{
		this.values.putAll(values.entrySet()
				.stream()
				.filter(entry -> !entry.getValue()
						.isBlank())
				.collect(toMap(entry -> entry.getKey()
						.toString(), Entry::getValue)));
	}

	public Map<String, String> getMap()
	{
		return new HashMap<>(values);
	}

	public Map<Locale, String> getLocalizedMap()
	{
		return values.entrySet()
				.stream()
				.collect(toMap(key -> Locale.forLanguageTag(key.getKey()), Entry::getValue));
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	public boolean isEmpty()
	{
		return (defaultValue == null || defaultValue.isEmpty()) && values.isEmpty();
	}

	public boolean hasNonDefaultValue()
	{
		return !values.isEmpty();
	}

	@Override
	public String toString()
	{
		return "I18nString [values=" + values + ", defaultValue=" + defaultValue + "]";
	}

	@Override
	public I18nString clone()
	{
		I18nString ret = new I18nString(defaultValue);
		ret.addAllValues(values);
		return ret;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}

	public void replace(String oldV, String newV)
	{
		if (defaultValue != null)
			defaultValue = defaultValue.replace(oldV, newV);
		for (Entry<String, String> v : values.entrySet())
			values.put(v.getKey(), v.getValue()
					.replace(oldV, newV));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		I18nString other = (I18nString) obj;
		if (defaultValue == null)
		{
			if (other.defaultValue != null)
				return false;
		} else if (!defaultValue.equals(other.defaultValue))
			return false;
		if (values == null)
		{
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}
}
