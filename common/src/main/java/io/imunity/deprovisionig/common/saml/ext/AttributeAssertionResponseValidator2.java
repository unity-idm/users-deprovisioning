/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common.saml.ext;

import java.util.ArrayList;
import java.util.List;

import eu.unicore.samly2.SAMLUtils;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.AssertionValidator;
import eu.unicore.samly2.validators.AttributeAssertionResponseValidator;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.AssertionType;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseType;

public class AttributeAssertionResponseValidator2 extends AttributeAssertionResponseValidator
{

	public AttributeAssertionResponseValidator2(String consumerSamlName, String consumerEndpointUri,
			String requestId, long samlValidityGraceTime, SamlTrustChecker trustChecker,
			NameIDType requestedSubject)
	{
		super(consumerSamlName, consumerEndpointUri, requestId, samlValidityGraceTime, trustChecker,
				requestedSubject);

	}

	@Override
	public void validate(ResponseDocument attributeResponseDoc) throws SAMLValidationException
	{
		attributeAssertions = new ArrayList<AssertionDocument>();

		ResponseType response = attributeResponseDoc.getResponse();
		//ResponseTrustCheckResult responseTrust = 
		super.validate(attributeResponseDoc, response);

//		NameIDType issuer = response.getIssuer();
//		if (issuer == null || issuer.isNil() || issuer.getStringValue() == null)
//			throw new SAMLRequesterException("Issuer must be present");
//		if (issuer.getFormat() != null && !issuer.getFormat().equals(SAMLConstants.NFORMAT_ENTITY))
//			throw new SAMLValidationException(
//					"Issuer of SAML response must be of Entity type in SSO AuthN. It is: "
//							+ issuer.getFormat());

		List<AssertionDocument> assertions;
		try
		{
			assertions = SAMLUtils.extractAllAssertions(response, decryptionKey);
		} catch (Exception e)
		{
			throw new SAMLValidationException(
					"XML handling problem during retrieval of response assertions", e);
		}
		if (assertions == null)
			throw new SAMLValidationException("SAML response doesn't contain any assertion");
//		AssertionValidator asValidator = new AssertionValidator(consumerSamlName, consumerEndpointUri,
//				requestId, samlValidityGraceTime, trustChecker, responseTrust);

		for (AssertionDocument assertionDoc : assertions)
		{
			AssertionType assertion = assertionDoc.getAssertion();
			if (assertion.sizeOfAttributeStatementArray() > 0)
				validateAssertion(assertionDoc, null); // asValidator);
			else
				throw new SAMLValidationException(
						"In response to attribute query got response with assertion without attribute statements");
		}
	}

	@Override
	protected void validateAssertion(AssertionDocument assertionDoc, AssertionValidator asValidator)
			throws SAMLValidationException
	{

//		asValidator.validate(assertionDoc);
//		AssertionType assertion = assertionDoc.getAssertion();
//		NameIDType receivedSubject = assertion.getSubject().getNameID();
//
//		if (!SAMLUtils.compareNameIDs(receivedSubject, requestedSubject))
//			throw new SAMLValidationException("Received assertion for  subject which was not requested: "
//					+ receivedSubject.xmlText() + "(requested was " + receivedSubject.xmlText()
//					+ ")");
		attributeAssertions.add(assertionDoc);
	}

}
