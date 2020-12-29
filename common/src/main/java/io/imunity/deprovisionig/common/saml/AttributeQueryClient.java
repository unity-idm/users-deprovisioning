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

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.security.wsutil.samlclient.SAMLAttributeQueryClient;
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
	private final NameID localIssuer;

	@Autowired
	public AttributeQueryClient(SAMLCredentialConfiguration credentials, Environment env,
			@Value("${saml.requester.requesterEntityId}") String requesterEntityId)
	{
		clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(credentials.getCredential());
		clientCfg.setValidator(credentials.getValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(false);

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

		SAMLAttributeQueryClient attrClient;
		try
		{
			attrClient = new SAMLAttributeQueryClient(attributeQueryServiceUrl, clientCfg);
		} catch (MalformedURLException e)
		{
			throw new InternalException("Invalid attribute service url " + attributeQueryServiceUrl, e);
		}

		log.debug("Query for attributes for user " + userIdentity + " from " + attributeQueryServiceUrl);
		AttributeAssertionParser attrQueryParser;
		try
		{
			attrQueryParser = attrClient.getAssertion(
					new NameID(userIdentity, SAMLConstants.NFORMAT_PERSISTENT), localIssuer);
		} catch (SAMLValidationException e)
		{
			throw new SAMLException("Invalid saml attribute query response from " + attributeQueryServiceUrl
					+ " for user  " + userIdentity + " failed", e);
		} catch (Exception e)
		{
			throw new InternalException("Attribute query to " + attributeQueryServiceUrl + " for user "
					+ userIdentity + " failed", e);
		}

		return attrQueryParser;
	}
}
