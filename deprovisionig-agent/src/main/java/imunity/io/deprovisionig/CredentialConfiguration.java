/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package imunity.io.deprovisionig;

import java.io.IOException;
import java.security.KeyStoreException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import imunity.io.deprovisionig.exception.CredentialConfigurationException;

@Component
public class CredentialConfiguration
{
	@Value("${truststore.path}")
	private String truststorePath;
	@Value("${truststore.password}")
	private String truststorePassword;
	@Value("${truststore.format}")
	private String truststoreFormat;

	public CredentialConfiguration()
	{
		
	}
	
	public KeystoreCertChainValidator getValidator() throws CredentialConfigurationException
	{
		try
		{
			return new KeystoreCertChainValidator(truststorePath, truststorePassword.toCharArray(),
					truststoreFormat, -1);
		} catch (KeyStoreException | IOException e)
		{
			throw new CredentialConfigurationException("Can not load truststore", e);
		}
	}
}
