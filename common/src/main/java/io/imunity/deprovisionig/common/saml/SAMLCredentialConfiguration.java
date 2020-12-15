/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common.saml;

import java.io.IOException;
import java.security.KeyStoreException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import io.imunity.deprovisionig.common.CredentialConfiguration;
import io.imunity.deprovisionig.common.exception.CredentialConfigurationException;

@Component
public class SAMLCredentialConfiguration
{
	@Value("${saml.requester.credential.path}")
	private String credentialPath;
	@Value("${saml.requester.credential.password}")
	private String credentialPassword;
	@Value("${saml.requester.credential.format}")
	private String credentialFormat;
	@Value("${saml.requester.credential.keyAlias}")
	private String credentialKeyAlias;

	private CredentialConfiguration main;
	
	@Autowired
	public SAMLCredentialConfiguration(CredentialConfiguration main)
	{
		this.main = main;
	}
	
	
	public KeystoreCredential getCredential() throws CredentialConfigurationException
	{
		try
		{
			return new KeystoreCredential(credentialPath, credentialPassword.toCharArray(),
					credentialPassword.toCharArray(), credentialKeyAlias, credentialFormat);
		} catch (KeyStoreException | IOException e)
		{
			throw new CredentialConfigurationException("Can not load keystore", e);
		}
	}

	public KeystoreCertChainValidator getValidator() throws CredentialConfigurationException
	{
		return main.getValidator();
	}
}
