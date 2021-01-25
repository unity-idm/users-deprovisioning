/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common.saml;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.impl.FormatMode;
import eu.emi.security.authn.x509.impl.X509Formatter;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;
import io.imunity.deprovisionig.common.PropertiesHelper;
import io.imunity.deprovisionig.common.exception.InternalException;
import io.imunity.deprovisionig.common.exception.SAMLException;

@Component
public class AttributeQueryClient
{
	private static final Logger log = LogManager.getLogger(AttributeQueryClient.class);

	private static final int DEFAULT_SO_TIMEOUT = 120000;
	private static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
	private static final String HTTP_CONFIGURATION_PREFIX = "saml.requester.http.";

	private final DefaultClientConfiguration clientCfg;
	private final SAMLCredentialConfiguration credential;
	private final NameID localIssuer;
	private final boolean signRequest;

	@Autowired
	public AttributeQueryClient(SAMLCredentialConfiguration credential, Environment env,
			@Value("${saml.requester.requesterEntityId}") String requesterEntityId,
			@Value("${saml.requester.signRequest:true}") Boolean signRequest,
			@Value("${saml.requester.sslAuthn:false}") Boolean sslAuthn,
			@Value("${saml.requester.messageLogging:false}") Boolean messageLogging)
	{
		this.signRequest = signRequest;
		this.credential = credential;
		
		clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(credential.getCredential());
		clientCfg.setValidator(credential.getValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(sslAuthn);
		clientCfg.setHttpAuthn(false);
		clientCfg.setMessageLogging(messageLogging);		
		
		log.debug("SSL Authn: " + sslAuthn);
		if (sslAuthn)
		{
			log.debug("Certificate uses for authn: " + new X509Formatter(FormatMode.FULL)
					.format(credential.getCredential().getCertificate()));
		}
		log.debug("Sign request: " + signRequest);

		HttpClientProperties httpClientProperties = new HttpClientProperties(HTTP_CONFIGURATION_PREFIX,
				PropertiesHelper.getAllProperties(env));
		if (!httpClientProperties.isSet(HttpClientProperties.SO_TIMEOUT))
		{
			httpClientProperties.setSocketTimeout(DEFAULT_SO_TIMEOUT);
		}

		if (!httpClientProperties.isSet(HttpClientProperties.CONNECT_TIMEOUT))
		{
			httpClientProperties.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
		}

		clientCfg.setHttpClientProperties(httpClientProperties);
		localIssuer = new NameID(requesterEntityId, SAMLConstants.NFORMAT_ENTITY);
	}

	public Optional<List<ParsedAttribute>> getAttributes(String attributeQueryServiceUrl, String userIdentity)
			throws InternalException
	{
		log.debug("Get attributes for user " + userIdentity + " from " + attributeQueryServiceUrl);
		try
		{
			return Optional.of(query(attributeQueryServiceUrl, userIdentity).getAttributes());
		} catch (SAMLValidationException e)
		{
			throw new SAMLException("Can not extract attributes for saml response", e);
		}
	}

	public AttributeAssertionParser query(String attributeQueryServiceUrl, String userIdentity)
			throws InternalException
	{
		SAMLAttributeQueryClientWithLog attrClient;
		try
		{
			attrClient = new SAMLAttributeQueryClientWithLog(attributeQueryServiceUrl, clientCfg);
		} catch (MalformedURLException e)
		{
			throw new InternalException("Invalid attribute service url " + attributeQueryServiceUrl, e);
		}
	

		log.debug("Query for attributes for user " + userIdentity + " from " + attributeQueryServiceUrl);
		AttributeAssertionParser attrQueryParser;
		try
		{
			attrQueryParser = attrClient.getAssertion(
					new NameID(userIdentity, SAMLConstants.NFORMAT_PERSISTENT), localIssuer,
					signRequest, credential.getCredential());
		} catch (SAMLValidationException e)
		{
			throw new SAMLException("Invalid saml attribute query response from " + attributeQueryServiceUrl
					+ " for user  " + userIdentity, e);
		} catch (Exception e)
		{
			throw new InternalException("Attribute query to " + attributeQueryServiceUrl + " for user "
					+ userIdentity + " failed", e);
		}

		return attrQueryParser;
	}
}
