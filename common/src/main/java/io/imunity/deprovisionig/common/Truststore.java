/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.unicore.security.canl.LoggingStoreUpdateListener;
import eu.unicore.security.canl.TruststoreProperties;
import io.imunity.deprovisionig.common.exception.CredentialConfigurationException;

@Component
public class Truststore
{
	private X509CertChainValidatorExt validator;

	@Autowired
	public Truststore(Environment env) throws CredentialConfigurationException
	{
		try
		{
			validator = new TruststoreProperties(PropertiesHelper.getAllProperties(env),
					Collections.singleton(new LoggingStoreUpdateListener())).getValidator();
		} catch (Exception e)
		{
			throw new CredentialConfigurationException("Can not load truststore", e);
		}

	}

	public X509CertChainValidatorExt getValidator()
	{
		return validator;
	}
}
