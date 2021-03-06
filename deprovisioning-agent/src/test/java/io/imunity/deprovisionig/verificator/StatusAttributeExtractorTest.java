/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.apache.xmlbeans.XmlException;
import org.junit.jupiter.api.Test;

import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.UnityUser;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;

public class StatusAttributeExtractorTest
{
	
	@Test
	public void shouldFallbackToActiveWhenEmptyStatusAttr() throws XmlException, SAMLValidationException, IOException
	{
		AssertionDocument doc = AssertionDocument.Factory.parse(new File("src/test/resources/responses/assertionWithoutStatusAttribute.xml"));
		AttributeAssertionParser parser = new AttributeAssertionParser(doc.getAssertion());

		EntityState newStatus = StatusAttributeExtractor.getStatusFromAttributesOrFallbackToUserStatus(
				getUser(EntityState.disabled), Optional.of(parser.getAttributes()));

		assertThat(newStatus, is(EntityState.valid));
	}
	
	@Test
	public void shouldFallbackToUserStatusWhenUnknownStatus() throws XmlException, SAMLValidationException, IOException
	{
		AssertionDocument doc = AssertionDocument.Factory.parse(new File("src/test/resources/responses/assertionWithUnknownStatusAttribute.xml"));
		AttributeAssertionParser parser = new AttributeAssertionParser(doc.getAssertion());

		EntityState newStatus = StatusAttributeExtractor.getStatusFromAttributesOrFallbackToUserStatus(
				getUser(EntityState.disabled), Optional.of(parser.getAttributes()));

		assertThat(newStatus, is(EntityState.disabled));
	}
		
	@Test
	public void shouldExtractStatusAttribute() throws XmlException, SAMLValidationException, IOException
	{
		AssertionDocument doc = AssertionDocument.Factory.parse(new File("src/test/resources/responses/assertionWithStatusAttribute.xml"));
		AttributeAssertionParser parser = new AttributeAssertionParser(doc.getAssertion());

		EntityState newStatus = StatusAttributeExtractor.getStatusFromAttributesOrFallbackToUserStatus(
				getUser(EntityState.disabled), Optional.of(parser.getAttributes()));

		assertThat(newStatus, is(EntityState.valid));
	}

	private UnityUser getUser(EntityState status)
	{
		return new UnityUser(1L, "u1", status,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test")),
				Set.of("/", "/A", "/B"), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
				LocalDateTime.now(), LocalDateTime.now());
	}

}
