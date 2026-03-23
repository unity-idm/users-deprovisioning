/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.deprovisionig.NetworkClient;
import io.imunity.deprovisionig.common.WorkdirFileManager;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.saml.metadata.SAMLMetadataConfiguration;
import io.imunity.deprovisionig.saml.metadata.SAMLMetadataManager;

@ExtendWith(MockitoExtension.class)
public class SAMLMetadataManagerTest
{
	@Test
	public void shouldLoadIdpsWithNames() throws Exception
	{
		// given
		Path path = Paths.get("src/test/resources/metadata.switchaai.xml")
				.toAbsolutePath();
		SAMLMetadataConfiguration config = new SAMLMetadataConfiguration(Duration.ofHours(1), path.toUri()
				.toString());

		SAMLMetadataManager manager = new SAMLMetadataManager(Mockito.mock(NetworkClient.class),
				Mockito.mock(WorkdirFileManager.class), config);

		// when
		Map<String, SAMLIdpInfo> result = manager.getIDPsAsMap();

		// then
		assertNotNull(result);
		assertEquals("University of Fribourg", result.get("https://aai.unifr.ch/idp/shibboleth").name.getValue("en"));
		assertEquals("Universität Freiburg", result.get("https://aai.unifr.ch/idp/shibboleth").name.getValue("de"));
		assertEquals("University of Fribourg",
				result.get("https://aai.unifr.ch/idp/shibboleth").name.getDefaultValue());

	}

	@Test
	public void shouldLoadNameFromOrganizationWhenIDPDescriptorIsEmpty() throws Exception
	{
		// given
		Path path = Paths.get("src/test/resources/metadata.switchaai.xml")
				.toAbsolutePath();
		SAMLMetadataConfiguration config = new SAMLMetadataConfiguration(Duration.ofHours(1), path.toUri()
				.toString());

		SAMLMetadataManager manager = new SAMLMetadataManager(Mockito.mock(NetworkClient.class),
				Mockito.mock(WorkdirFileManager.class), config);

		// when
		Map<String, SAMLIdpInfo> result = manager.getIDPsAsMap();

		// then
		assertNotNull(result);
		assertEquals("HTW Chur FROM ORGANIZATION",
				result.get("https://aai-login.fh-htwchur.ch/idp/shibboleth").name.getValue("en"));

	}
}
