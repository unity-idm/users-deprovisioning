/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml.metadata;

import java.util.Objects;

public class SAMLIdpInfo
{
	public final String id;
	public final String attributeQueryServiceUrl;
	public final String technicalAdminEmail;

	public SAMLIdpInfo(String id, String attributeQueryServiceUrl, String technicalAdminEmail)
	{
		this.id = id;
		this.attributeQueryServiceUrl = attributeQueryServiceUrl;
		this.technicalAdminEmail = technicalAdminEmail;
	}

	@Override
	public String toString()
	{
		return "SAML IDP " + id + " [attributeQueryService:" + attributeQueryServiceUrl
				+ ", technicalAdminEmail:" + technicalAdminEmail + "]";
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof SAMLIdpInfo))
			return false;
		SAMLIdpInfo castOther = (SAMLIdpInfo) other;
		return Objects.equals(id, castOther.id)
				&& Objects.equals(attributeQueryServiceUrl, castOther.attributeQueryServiceUrl)
				&& Objects.equals(technicalAdminEmail, castOther.technicalAdminEmail);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, attributeQueryServiceUrl, technicalAdminEmail);
	}

}
