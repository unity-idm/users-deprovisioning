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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.FormatMode;
import eu.emi.security.authn.x509.impl.X509Formatter;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.security.wsutil.samlclient.SAMLAttributeQueryClient;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;
import io.imunity.deprovisionig.common.PropertiesHelper;
import io.imunity.deprovisionig.common.exception.SAMLComunicationException;
import io.imunity.deprovisionig.common.exception.SAMLException;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

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
	private final SAMLRequesterConfiguration requesterConfig;

	@Autowired
	public AttributeQueryClient(SAMLCredentialConfiguration credential, Environment env,
			SAMLRequesterConfiguration requesterConfig)
	{
		this.requesterConfig = requesterConfig;
		this.credential = credential;

		clientCfg = getClientConfiguration(env);
		localIssuer = new NameID(requesterConfig.requesterEntityId, SAMLConstants.NFORMAT_ENTITY);
	}

	private DefaultClientConfiguration getClientConfiguration(Environment env)
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(credential.getCredential());
		clientCfg.setValidator(credential.getValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(requesterConfig.sslAuthn);
		clientCfg.setHttpAuthn(false);
		clientCfg.setMessageLogging(requesterConfig.messageLogging);

		log.debug("SSL Authn: " + requesterConfig.sslAuthn);
		if (requesterConfig.sslAuthn)
		{
			log.debug("Certificate uses for authn: " + new X509Formatter(FormatMode.FULL)
					.format(credential.getCredential().getCertificate()));
		}
		log.debug("Sign request: " + requesterConfig.signRequest);
		clientCfg.setHttpClientProperties(getHttpClientProperties(env));
		return clientCfg;
	}

	private HttpClientProperties getHttpClientProperties(Environment env)
	{
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
		return httpClientProperties;
	}

	public Optional<List<ParsedAttribute>> getAttributes(String attributeQueryServiceUrl, NameID userIdentity)
			throws SAMLException
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

	public AttributeAssertionParser query(String attributeQueryServiceUrl, NameID userIdentity) throws SAMLException
	{
		SAMLAttributeQueryClient attrClient = prepareQueryClient(attributeQueryServiceUrl);
		log.debug("Query for attributes for user " + userIdentity + " from " + attributeQueryServiceUrl);
		AttributeAssertionParser attrQueryParser;
		try
		{
			Optional<X509Credential> signAndDecryptCredential = Optional.of(credential.getCredential());
			attrQueryParser = attrClient.getAssertion(
					userIdentity, localIssuer,
					requesterConfig.signRequest ? signAndDecryptCredential : Optional.empty(),
					signAndDecryptCredential);
		} catch (SAMLValidationException e)
		{
			throw new SAMLException("Invalid saml attribute query response from " + attributeQueryServiceUrl
					+ " for user  " + userIdentity, e);
		} catch (Exception e)
		{
			throw new SAMLComunicationException("Attribute query to " + attributeQueryServiceUrl + " for user "
					+ userIdentity + " failed", e);
		}

		return attrQueryParser;
	}

	public ResponseDocument queryForRawAssertion(String attributeQueryServiceUrl, String userIdentity)
			throws SAMLException
	{
		SAMLAttributeQueryClient attrClient = prepareQueryClient(attributeQueryServiceUrl);
		log.debug("Query for raw attributes document for user " + userIdentity + " from "
				+ attributeQueryServiceUrl);
		ResponseDocument respDoc;
		try
		{

			respDoc = attrClient.getRawAssertion(new NameID(userIdentity, SAMLConstants.NFORMAT_PERSISTENT),
					localIssuer, null,
					requesterConfig.signRequest ? Optional.of(credential.getCredential()) : Optional.empty());
		} catch (SAMLValidationException e)
		{
			throw new SAMLException("Invalid saml attribute query response from " + attributeQueryServiceUrl
					+ " for user  " + userIdentity, e);
		} catch (Exception e)
		{
			throw new SAMLComunicationException("Attribute query to " + attributeQueryServiceUrl + " for user "
					+ userIdentity + " failed", e);
		}

		return respDoc;
	}

	private SAMLAttributeQueryClient prepareQueryClient(String attributeQueryServiceUrl)
	{
		try
		{
			return new SAMLAttributeQueryClient(attributeQueryServiceUrl, clientCfg, new TrustAllChecker());
		} catch (MalformedURLException e)
		{
			throw new SAMLComunicationException("Invalid attribute service url " + attributeQueryServiceUrl, e);
		}
	}
}
