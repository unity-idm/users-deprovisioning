/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.security.canl.CredentialProperties;
import io.imunity.deprovisionig.common.saml.SAMLCredentialConfiguration;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

@ExtendWith(MockitoExtension.class)
public class SimpleResponseDecryptorTest
{
	@Mock
	private SAMLCredentialConfiguration credentialConfig;

	@Test
	public void shouldDecryptResponseDocument() throws Exception
	{
		Properties p = new Properties();
		p.load(new FileInputStream("src/test/resources/application.properties"));
		X509Credential credential = new CredentialProperties(p, "saml.requester.credential.").getCredential();
		when(credentialConfig.getCredential()).thenReturn(credential);

		ResponseDocument resp = ResponseDocument.Factory
				.parse(new File("src/test/resources/encryptedByUnity.xml"));

		SimpleResponseDecryptor decryptor = new SimpleResponseDecryptor(credentialConfig);

		Optional<ResponseDocument> decrypt = decryptor.decrypt(resp);

		if (decrypt.isEmpty())
			fail("Incorrectly decrypted attribute query response document");
		assertThat(decrypt.get().getResponse().getAssertionArray(0).getAttributeStatementArray(0)
				.getAttributeArray(0).getFriendlyName(), is("eduPersonAssurance"));

	}
}
