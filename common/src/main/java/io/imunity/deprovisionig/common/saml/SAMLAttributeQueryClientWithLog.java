/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common.saml;

import java.net.MalformedURLException;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLUtils;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.elements.SAMLAttribute;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.proto.AttributeQuery;
import eu.unicore.security.dsig.DSigException;
import eu.unicore.security.dsig.DigSignatureUtil;
import eu.unicore.util.httpclient.IClientConfiguration;
import io.imunity.deprovisionig.common.exception.InternalException;
import io.imunity.deprovisionig.common.saml.ext.SAMLAttributeQeuryClient2;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

public class SAMLAttributeQueryClientWithLog extends SAMLAttributeQeuryClient2
{
	private static final Logger log = LogManager.getLogger(SAMLAttributeQueryClientWithLog.class);

	public SAMLAttributeQueryClientWithLog(String address, IClientConfiguration clientConfiguration)
			throws MalformedURLException
	{
		super(address, clientConfiguration);

	}

	public AttributeAssertionParser getAssertion(NameID whose, NameID requesterSamlName, boolean sign,
			X509Credential credential) throws SAMLValidationException, InternalException
	{
		return getAssertionGeneric(whose, requesterSamlName, null, sign, credential);
	}

	//For test tool only
	public ResponseDocument getRawAssertion(NameID whose, NameID requesterSamlName, Set<SAMLAttribute> attributes,
			boolean sign, X509Credential credential) throws SAMLValidationException, InternalException
	{
		AttributeQuery attrQuery = createQuery(whose, requesterSamlName);
		if (attributes != null && attributes.size() > 0)
			attrQuery.setAttributes(attributes.toArray(new SAMLAttribute[attributes.size()]));

		if (sign)
		{
			try
			{
				attrQuery.sign(credential.getKey(), credential.getCertificateChain());
			} catch (Exception e)
			{
				throw new InternalException("Can't sign request", e);
			}
		}

		try
		{
			log.debug("Attribute query document: " + DigSignatureUtil
					.dumpDOMToString(SAMLUtils.getDOM(attrQuery.getXMLBeanDoc())));
		} catch (DSigException e)
		{
			log.error("Can not dump query document to log");
		}

		return performRawSAMLQuery(attrQuery);
	}

	private AttributeAssertionParser getAssertionGeneric(NameID whose, NameID requesterSamlName,
			Set<SAMLAttribute> attributes, boolean sign, X509Credential credential)
			throws SAMLValidationException, InternalException
	{
		AttributeQuery attrQuery = createQuery(whose, requesterSamlName);
		if (attributes != null && attributes.size() > 0)
			attrQuery.setAttributes(attributes.toArray(new SAMLAttribute[attributes.size()]));

		if (sign)
		{
			try
			{
				attrQuery.sign(credential.getKey(), credential.getCertificateChain());
			} catch (Exception e)
			{
				throw new InternalException("Can't sign request", e);
			}
		}

		try
		{
			log.debug("Attribute query document: " + DigSignatureUtil
					.dumpDOMToString(SAMLUtils.getDOM(attrQuery.getXMLBeanDoc())));
		} catch (DSigException e)
		{
			log.error("Can not dump query document to log");
		}

		return performSAMLQuery(attrQuery);
	}
}
