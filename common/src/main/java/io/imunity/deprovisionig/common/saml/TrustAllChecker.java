/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.common.saml;

import org.apache.xmlbeans.XmlObject;

import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.trust.CheckingMode;
import eu.unicore.samly2.trust.ResponseTrustCheckResult;
import eu.unicore.samly2.trust.SamlTrustChecker;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.protocol.RequestAbstractType;
import xmlbeans.org.oasis.saml2.protocol.StatusResponseType;

public class TrustAllChecker implements SamlTrustChecker
{

	@Override
	public void checkTrust(AssertionDocument assertionDoc) throws SAMLValidationException
	{
			
	}

	@Override
	public void checkTrust(AssertionDocument assertionDoc, ResponseTrustCheckResult responseCheckResult)
			throws SAMLValidationException
	{
			
	}

	@Override
	public ResponseTrustCheckResult checkTrust(XmlObject responseDoc, StatusResponseType response)
			throws SAMLValidationException
	{
		return new ResponseTrustCheckResult(true);
	}

	@Override
	public void checkTrust(XmlObject requestDoc, RequestAbstractType request) throws SAMLValidationException
	{
		
	}

	@Override
	public CheckingMode getCheckingMode()
	{
		return CheckingMode.REQUIRE_SIGNED_ASSERTION;
	}
	

}
