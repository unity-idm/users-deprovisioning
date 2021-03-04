/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.saml;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.SAMLUtils;
import eu.unicore.samly2.assertion.AssertionParser;
import io.imunity.deprovisionig.common.saml.SAMLCredentialConfiguration;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.AssertionType;
import xmlbeans.org.oasis.saml2.assertion.EncryptedAssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.EncryptedElementType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;


@Component
public class SimpleResponseDecryptor
{
	private final SAMLCredentialConfiguration credential;
	
	@Autowired		
	public SimpleResponseDecryptor(SAMLCredentialConfiguration credential)
	{
		this.credential = credential;
	}
	
	Optional<ResponseDocument> decrypt(ResponseDocument toDecrypt) throws Exception
	{  
		ResponseDocument resp = ResponseDocument.Factory.newInstance();
		resp.set(toDecrypt.copy());
		
		EncryptedAssertionDocument[] encryptedAssertions = SAMLUtils.getEncryptedAssertions(toDecrypt.getResponse());
		if (encryptedAssertions.length == 0)
		{
			return Optional.empty();
		}
		
		resp.getResponse().setEncryptedAssertionArray(new EncryptedElementType[0]);
		
		for (EncryptedAssertionDocument enAsDoc : encryptedAssertions)
		{
			AssertionParser parser = new AssertionParser(enAsDoc, credential.getCredential().getKey());
			AssertionDocument encryptedAsDoc = parser.getXMLBeanDoc();
			AssertionType assertion = encryptedAsDoc.getAssertion();
			AssertionType addNewAssertion = resp.getResponse().addNewAssertion();
			addNewAssertion.set(assertion.copy());
		}
	
		return Optional.of(resp);		
		
	}
}
