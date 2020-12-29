/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity.types;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Attribute
{
	private String name;
	private String valueSyntax;
	private String groupPath;
	private List<String> values = Collections.emptyList();
	private String translationProfile;
	private String remoteIdp;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getValueSyntax()
	{
		return valueSyntax;
	}

	public void setValueSyntax(String valueSyntax)
	{
		this.valueSyntax = valueSyntax;
	}

	public String getGroupPath()
	{
		return groupPath;
	}

	public void setGroupPath(String groupPath)
	{
		this.groupPath = groupPath;
	}

	public List<String> getValues()
	{
		return values;
	}

	public void setValues(List<String> values)
	{
		this.values = values;
	}

	public String getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(String translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	public String getRemoteIdp()
	{
		return remoteIdp;
	}

	public void setRemoteIdp(String remoteIdp)
	{
		this.remoteIdp = remoteIdp;
	}

	@JsonCreator
	public Attribute()
	{

	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof Attribute))
			return false;
		Attribute castOther = (Attribute) other;
		return Objects.equals(name, castOther.name) && Objects.equals(valueSyntax, castOther.valueSyntax)
				&& Objects.equals(groupPath, castOther.groupPath)
				&& Objects.equals(values, castOther.values)
				&& Objects.equals(translationProfile, castOther.translationProfile)
				&& Objects.equals(remoteIdp, castOther.remoteIdp);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, valueSyntax, groupPath, values, translationProfile, remoteIdp);
	}

}