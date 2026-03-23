/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml.metadata;

import java.util.Objects;
import java.util.Optional;

import io.imunity.deprovisionig.unity.types.I18nString;

public class SAMLIdpInfo
{
	public final String id;
	public final Optional<String> attributeQueryServiceUrl;
	public final String technicalAdminEmail;
	public final I18nString name;

	public SAMLIdpInfo(String id, Optional<String> attributeQueryServiceUrl, String technicalAdminEmail, I18nString name)
	{
		this.id = id;
		this.attributeQueryServiceUrl = attributeQueryServiceUrl;
		this.technicalAdminEmail = technicalAdminEmail;
		this.name = name;
	}

	public String toString()
	{
		return "SAMLIdpInfo [id=" + id + ", attributeQueryServiceUrl=" + attributeQueryServiceUrl
				+ ", technicalAdminEmail=" + technicalAdminEmail + ", name=" + name + "]";
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributeQueryServiceUrl, id, name, technicalAdminEmail);
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
		SAMLIdpInfo other = (SAMLIdpInfo) obj;
		return Objects.equals(attributeQueryServiceUrl, other.attributeQueryServiceUrl) && Objects.equals(id, other.id)
				&& Objects.equals(name, other.name) && Objects.equals(technicalAdminEmail, other.technicalAdminEmail);
	}
}
