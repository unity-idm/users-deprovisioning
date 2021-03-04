/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLUtils;
import eu.unicore.samly2.assertion.AssertionParser;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.dsig.DSigException;
import eu.unicore.security.dsig.DigSignatureUtil;
import eu.unicore.security.enc.EncryptionUtil;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.EncryptedElementType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

public class Encryptor
{

	@Test
	public void decrypt() throws Exception
	{
		Properties p = new Properties();
		p.load(new FileInputStream("src/test/resources/application.properties"));
		X509Credential credential = new CredentialProperties(p, "saml.requester.credential.").getCredential();

		ResponseDocument resp = ResponseDocument.Factory
				.parse(new File("src/test/resources/responses/encryptedByUnity.xml"));
		AssertionParser parser = new AssertionParser(SAMLUtils.getEncryptedAssertions(resp.getResponse())[0],
				credential.getKey());

		System.out.println(DigSignatureUtil.dumpDOMToString(parser.getAsDOM()));

	}

	@Test
	public void encryptAssertion() throws DSigException, Exception
	{
		Properties p = new Properties();
		p.load(new FileInputStream("src/test/resources/application.properties"));
		X509Credential credential = new CredentialProperties(p, "saml.requester.credential.").getCredential();

		ResponseDocument resp = ResponseDocument.Factory
				.parse(new File("src/test/resources/responses/encrypted.xml"));
		EncryptionUtil encUtil = new EncryptionUtil();

		Document encrypted = encUtil.encrypt(new AssertionParser(AssertionDocument.Factory
				.parse(new File("src/test/resources/responses/withSchacUserStatus.xml"))).getAsDOM(),
				credential.getCertificate(), 256);
		EncryptedElementType addNewEncryptedAssertion = EncryptedElementType.Factory.parse(encrypted);
		resp.getResponse().setEncryptedAssertionArray(0, addNewEncryptedAssertion);

		System.out.println(DigSignatureUtil.dumpDOMToString(SAMLUtils.getDOM(resp)));

	}
}
