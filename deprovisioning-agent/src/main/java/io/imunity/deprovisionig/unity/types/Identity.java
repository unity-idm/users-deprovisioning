/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity.types;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Identity
{
	private String typeId;
	private String value;
	private String remoteIdp;
	private String translationProfile;

	@JsonCreator
	public Identity()
	{

	}

	public Identity(String typeId, String value, String translationProfile)
	{
		this.typeId = typeId;
		this.value = value;
		this.translationProfile = translationProfile;
	}

	public Identity(String typeId, String value, String translationProfile, String remoteIdp)
	{
		this.typeId = typeId;
		this.value = value;
		this.translationProfile = translationProfile;
		this.remoteIdp = remoteIdp;
	}

	public String getRemoteIdp()
	{
		return remoteIdp;
	}

	public void setRemoteIdp(String remoteIdp)
	{
		this.remoteIdp = remoteIdp;
	}

	public String getTypeId()
	{
		return typeId;
	}

	public void setTypeId(String typeId)
	{
		this.typeId = typeId;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(String translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	@Override
	public String toString()
	{
		
		return "[" + typeId + "]" + value;
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof Identity))
			return false;
		Identity castOther = (Identity) other;
		return Objects.equals(typeId, castOther.typeId) && Objects.equals(value, castOther.value)
				&& Objects.equals(remoteIdp, castOther.remoteIdp)
				&& Objects.equals(translationProfile, castOther.translationProfile);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(typeId, value, remoteIdp, translationProfile);
	}
}
