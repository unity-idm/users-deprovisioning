/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.common.saml.ext;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.ws.soap.SOAPFaultException;

import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.elements.SAMLAttribute;
import eu.unicore.samly2.elements.Subject;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.proto.AttributeQuery;
import eu.unicore.samly2.trust.PKISamlTrustChecker;
import eu.unicore.samly2.validators.AssertionValidator;
import eu.unicore.samly2.validators.AttributeAssertionResponseValidator;
import eu.unicore.samly2.webservice.SAMLQueryInterface;
import eu.unicore.util.httpclient.IClientConfiguration;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

public class SAMLAttributeQeuryClient2 extends AbstractSAMLClient2
{
private SAMLQueryInterface queryProxy;

	
	public SAMLAttributeQeuryClient2(String address, IClientConfiguration clientConfiguration) 
		throws MalformedURLException
	{
		super(address, clientConfiguration, new PKISamlTrustChecker(clientConfiguration.getValidator(), true));
		queryProxy = factory.createPlainWSProxy(SAMLQueryInterface.class, address);
	}
	
	public AttributeAssertionParser getAssertion(NameID whose, NameID requesterSamlName) throws SAMLValidationException
	{
		return getAssertionGeneric(whose, requesterSamlName, null);
	}

	public AttributeAssertionParser getAssertion(NameID whose, NameID requesterSamlName, SAMLAttribute attribute) 
			throws SAMLValidationException
	{
		return getAssertionGeneric(whose, requesterSamlName, Collections.singleton(attribute));
	}

	public AttributeAssertionParser getAssertion(NameID whose, NameID requesterSamlName, Set<SAMLAttribute> attributes) 
			throws SAMLValidationException
	{
		return getAssertionGeneric(whose, requesterSamlName, attributes);
	}

	
	/*-********************************************************
	 * INTERNAL methods 
	 *-********************************************************/

	/**
	 * Gets an assertion using high level API arguments.
	 * @param whose
	 * @param requesterSamlName
	 * @param attributes
	 * @throws SAMLValidationException
	 */
	protected AttributeAssertionParser getAssertionGeneric(NameID whose, NameID requesterSamlName, 
			Set<SAMLAttribute> attributes) throws SAMLValidationException
	{
		AttributeQuery attrQuery = createQuery(whose, requesterSamlName);
		if (attributes != null && attributes.size() > 0)
			attrQuery.setAttributes(attributes.toArray(new SAMLAttribute[attributes.size()]));
		return performSAMLQuery(attrQuery);
	}
	
	
	/**
	 * Performs a SAML query using a provided AttributeQUery argument. 
	 * Response is parsed and validated.
	 *  
	 * @param attrQuery what to query for
	 * @return parsed and verified assertion
	 * @throws SAMLValidationException
	 */
	protected AttributeAssertionParser performSAMLQuery(AttributeQuery attrQuery)
			throws SAMLValidationException
	{
		ResponseDocument xmlRespDoc;

		try
		{
			xmlRespDoc = queryProxy.attributeQuery(attrQuery.getXMLBeanDoc());
		} catch (SOAPFaultException e)
		{
			throw new SAMLResponderException("SAML service invocation failed: " + e.getMessage(), e);
		}
		
		AttributeAssertionResponseValidator validator = new AttributeAssertionResponseValidator(
				null, 
				null, 
				null, //no we don't want to check 'inResponseTo'
				AssertionValidator.DEFAULT_VALIDITY_GRACE_PERIOD, 
				trustChecker, 
				attrQuery.getXMLBean().getSubject().getNameID());
		validator.validate(xmlRespDoc);
		
		List<AssertionDocument> assertions = validator.getAttributeAssertions();
		
		if (assertions.size() == 0)
			return null;
		if (assertions.size() > 1)
			throw new SAMLValidationException(
				"More than one assertion was returned. It is OK," +
					"however this implementation supports only " +
					"responses with a single assertion.");
		AssertionDocument assertion = assertions.get(0);

		return new AttributeAssertionParser(assertion);
	}

	protected AttributeQuery createQuery(NameID whose, NameID requesterSamlName) throws SAMLValidationException
	{
		NameIDType subjectN = whose.getXBean();
		Subject subject = new Subject(subjectN.getStringValue(), subjectN.getFormat());
		if (requesterSamlName == null)
			requesterSamlName = getLocalIssuer();
		if (requesterSamlName == null)
			throw new SAMLValidationException("No SAML issuer was given and it is not " +
					"possible to generate one as local credential is missing.");
		return new AttributeQuery(requesterSamlName.getXBean(), subject.getXBean());
	}
}
