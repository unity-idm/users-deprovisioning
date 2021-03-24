/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.SAMLConstants.SubStatus;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLErrorResponseException;
import eu.unicore.samly2.webservice.SAMLQueryInterface;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.wsutil.samlclient.SAMLAttributeQueryClient;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;
import io.imunity.deprovisionig.common.saml.TrustAllChecker;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

@ExtendWith(MockitoExtension.class)
public class AtributeClientTest
{
	@Mock
	private SAMLQueryInterface queryInterface;

	@Test
	public void shouldGetAssertionFromEncryptedResponse() throws Exception
	{
		Properties p = new Properties();
		p.load(new FileInputStream("src/test/resources/application.properties"));
		X509Credential credential = new CredentialProperties(p, "saml.requester.credential.").getCredential();
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(credential);
		clientCfg.setHttpClientProperties(new HttpClientProperties(new Properties()));

		SAMLAttributeQueryClient client = new SAMLAttributeQueryClient(
				"https://localhost:2443/soapidp/saml2idp-soap/AssertionQueryService", clientCfg,
				new TrustAllChecker(), queryInterface);

		ResponseDocument resp = ResponseDocument.Factory
				.parse(new File("src/test/resources/responses/encryptedByUnity.xml"));
		when(queryInterface.attributeQuery(any())).thenReturn(resp);

		AttributeAssertionParser assertion = client.getAssertion(
				new NameID("Eq0LJU48CdQOF/+pEi2+j/TFMnk=", SAMLConstants.NFORMAT_PERSISTENT),
				new NameID("x", SAMLConstants.NFORMAT_ENTITY), Optional.empty(),
				Optional.of(credential));

		assertThat(assertion.getAttributes().size(), is(9));

		assertThat(assertion.getAttribute("urn:oid:1.3.6.1.4.1.25178.1.2.19").getStringValues().get(0),
				is("urn:schac:userStatus:de:helmholtz-berlin.de:active"));
	}

	@Test
	public void shouldGetAssertionFromResponse() throws Exception
	{
		Properties p = new Properties();
		p.load(new FileInputStream("src/test/resources/application.properties"));
		X509Credential credential = new CredentialProperties(p, "saml.requester.credential.").getCredential();
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(credential);
		clientCfg.setHttpClientProperties(new HttpClientProperties(new Properties()));

		SAMLAttributeQueryClient client = new SAMLAttributeQueryClient(
				"https://localhost:2443/soapidp/saml2idp-soap/AssertionQueryService", clientCfg,
				new TrustAllChecker(), queryInterface);

		ResponseDocument resp = ResponseDocument.Factory
				.parse(new File("src/test/resources/responses/kitShibDecrypted.xml"));
		when(queryInterface.attributeQuery(any())).thenReturn(resp);

		AttributeAssertionParser assertion = client.getAssertion(
				new NameID("n6ppeYRlKeJRObmZBmgJQ6vVNR4=", SAMLConstants.NFORMAT_PERSISTENT),
				new NameID("x", SAMLConstants.NFORMAT_ENTITY), Optional.empty(),
				Optional.of(credential));

		assertThat(assertion.getAttributes().size(), is(8));

		assertThat(assertion.getAttribute("urn:oid:1.3.6.1.4.1.5923.1.1.1.9").getStringValues().get(0),
				is("affiliate@kit.edu"));
	}

	@Test
	public void shouldThrowExceptionWhenUnknownPrincipal() throws Exception
	{
		Properties p = new Properties();
		p.load(new FileInputStream("src/test/resources/application.properties"));
		X509Credential credential = new CredentialProperties(p, "saml.requester.credential.").getCredential();
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(credential);
		clientCfg.setHttpClientProperties(new HttpClientProperties(new Properties()));

		SAMLAttributeQueryClient client = new SAMLAttributeQueryClient(
				"https://localhost:2443/soapidp/saml2idp-soap/AssertionQueryService", clientCfg,
				new TrustAllChecker(), queryInterface);

		ResponseDocument resp = ResponseDocument.Factory
				.parse(new File("src/test/resources/responses/unknownPrincipal.xml"));
		when(queryInterface.attributeQuery(any())).thenReturn(resp);

		SAMLErrorResponseException exception = assertThrows(SAMLErrorResponseException.class,
				() -> client.getAssertion(new NameID("x", SAMLConstants.NFORMAT_PERSISTENT),
						new NameID("x", SAMLConstants.NFORMAT_ENTITY), Optional.empty(),
						Optional.of(credential)));

		assertThat(SubStatus.STATUS2_UNKNOWN_PRINCIPAL, is(exception.getSamlSubErrorId()));
	}
}
