/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common.saml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.security.canl.CredentialProperties;
import io.imunity.deprovisionig.common.PropertiesHelper;
import io.imunity.deprovisionig.common.Truststore;
import io.imunity.deprovisionig.common.exception.CredentialConfigurationException;

@Component
public class SAMLCredentialConfiguration
{
	private final X509Credential credential;
	private final Truststore truststore;

	@Autowired
	public SAMLCredentialConfiguration(Truststore truststore, Environment env)
			throws CredentialConfigurationException
	{
		this.truststore = truststore;

		try
		{
			credential = new CredentialProperties(PropertiesHelper.getAllProperties(env),
					"saml.requester.credential.").getCredential();
		} catch (Exception e)
		{
			throw new CredentialConfigurationException("Can not load credentials", e);
		}
	}

	public X509Credential getCredential()
	{
		return credential;
	}

	public X509CertChainValidatorExt getValidator()
	{
		return truststore.getValidator();
	}
}
