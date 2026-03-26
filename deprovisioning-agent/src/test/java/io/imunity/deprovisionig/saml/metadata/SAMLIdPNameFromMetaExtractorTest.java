/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml.metadata;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.imunity.deprovisionig.unity.types.I18nString;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.IDPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.OrganizationType;
import xmlbeans.org.oasis.saml2.metadata.LocalizedNameType;

class SAMLIdPNameFromMetaExtractorTest
{

	@Test
	void shouldReturnDefaultNameWhenNoNamesPresent()
	{
		EntityDescriptorType entity = mock(EntityDescriptorType.class);
		IDPSSODescriptorType idp = mock(IDPSSODescriptorType.class);

		when(entity.getIDPSSODescriptorList()).thenReturn(Collections.singletonList(idp));
		when(idp.getProtocolSupportEnumeration())
				.thenReturn(Collections.singletonList("urn:oasis:names:tc:SAML:2.0:protocol"));

		I18nString result = SAMLIdPNameFromMetaExtractor.getLocalizedNamesAsI18nString("entity1", entity);

		assertEquals("Unnamed Identity Provider", result.getDefaultValue());
	}

	@Test
	void shouldUseOrganizationNameAsDefault()
	{
		EntityDescriptorType entity = mock(EntityDescriptorType.class);
		IDPSSODescriptorType idp = mock(IDPSSODescriptorType.class);
		OrganizationType org = mock(OrganizationType.class);
		LocalizedNameType name = mock(LocalizedNameType.class);

		when(entity.getIDPSSODescriptorList()).thenReturn(Collections.singletonList(idp));
		when(idp.getProtocolSupportEnumeration())
				.thenReturn(Collections.singletonList("urn:oasis:names:tc:SAML:2.0:protocol"));

		when(entity.getOrganization()).thenReturn(org);
		when(org.getOrganizationNameArray()).thenReturn(new LocalizedNameType[]
		{ name });
		when(org.getOrganizationDisplayNameArray()).thenReturn(null);

		when(name.getLang()).thenReturn("en");
		when(name.getStringValue()).thenReturn("Test IdP");

		I18nString result = SAMLIdPNameFromMetaExtractor.getLocalizedNamesAsI18nString("entity1", entity);

		assertEquals("Test IdP", result.getDefaultValue());
		assertEquals("Test IdP", result.getMap()
				.get("en"));
	}

	@Test
	void shouldPreferExplicitDefaultLanguage()
	{
		EntityDescriptorType entity = mock(EntityDescriptorType.class);
		IDPSSODescriptorType idp = mock(IDPSSODescriptorType.class);
		OrganizationType org = mock(OrganizationType.class);

		LocalizedNameType pl = mock(LocalizedNameType.class);
		LocalizedNameType en = mock(LocalizedNameType.class);

		when(entity.getIDPSSODescriptorList()).thenReturn(Collections.singletonList(idp));
		when(idp.getProtocolSupportEnumeration())
				.thenReturn(Collections.singletonList("urn:oasis:names:tc:SAML:2.0:protocol"));

		when(entity.getOrganization()).thenReturn(org);
		when(org.getOrganizationNameArray()).thenReturn(new LocalizedNameType[]
		{ pl, en });
		when(org.getOrganizationDisplayNameArray()).thenReturn(null);

		when(pl.getLang()).thenReturn("pl");
		when(pl.getStringValue()).thenReturn("Polska nazwa");

		when(en.getLang()).thenReturn("en");
		when(en.getStringValue()).thenReturn("English name");

		I18nString result = SAMLIdPNameFromMetaExtractor.getLocalizedNamesAsI18nString("entity1", entity);

		assertEquals("English name", result.getDefaultValue());
	}
}