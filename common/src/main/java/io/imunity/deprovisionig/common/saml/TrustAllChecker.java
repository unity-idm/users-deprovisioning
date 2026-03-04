/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.common.saml;

import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.messages.SAMLVerifiableElement;
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
	public CheckingMode getCheckingMode()
	{
		return CheckingMode.REQUIRE_SIGNED_ASSERTION;
	}

	@Override
	public ResponseTrustCheckResult checkTrust(SAMLVerifiableElement responseMessage, StatusResponseType response)
			throws SAMLValidationException
	{
		return new ResponseTrustCheckResult(true);
	}

	@Override
	public void checkTrust(SAMLVerifiableElement requestMessage, RequestAbstractType request)
			throws SAMLValidationException
	{
		
	}
	

}
