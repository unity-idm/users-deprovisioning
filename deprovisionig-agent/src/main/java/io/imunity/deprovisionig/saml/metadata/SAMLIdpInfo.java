/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml.metadata;

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
		return "SAML IDP " + id + " attributeQueryService:" + attributeQueryServiceUrl + " technicalAdminEmail:"
				+ technicalAdminEmail;
	}

}
