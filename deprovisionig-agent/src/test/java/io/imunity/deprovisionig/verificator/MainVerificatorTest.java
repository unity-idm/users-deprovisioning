/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.unicore.samly2.SAMLConstants.Status;
import eu.unicore.samly2.SAMLConstants.SubStatus;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.exceptions.SAMLErrorResponseException;
import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.TimesConfiguration;
import io.imunity.deprovisionig.common.exception.InternalException;
import io.imunity.deprovisionig.common.exception.SAMLException;
import io.imunity.deprovisionig.common.saml.AttributeQueryClient;
import io.imunity.deprovisionig.hook.GroovyHookExecutor;
import io.imunity.deprovisionig.saml.StatusAttributeExtractor;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.saml.metadata.SAMLMetadataManager;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.Attribute;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.UnityUser;
import io.imunity.deprovisionig.verificator.MainUserVerificator;
import io.imunity.deprovisionig.verificator.OfflineVerificator;

@ExtendWith(MockitoExtension.class)
public class MainVerificatorTest
{

	private MainUserVerificator verificator;
	@Mock
	private AttributeQueryClient samlAttrQueryClient;
	@Mock
	private SAMLMetadataManager samlMetadaMan;
	@Mock
	private UnityApiClient client;
	@Mock
	private OfflineVerificator offlineVerificator;
	@Mock
	private GroovyHookExecutor groovyHook;

	@BeforeEach
	public void init()
	{
		TimesConfiguration timesConfig = new TimesConfiguration(Duration.ofDays(2), Duration.ofDays(3),
				Duration.ofDays(2), Duration.ofDays(2), Duration.ofDays(2));

		verificator = new MainUserVerificator(samlAttrQueryClient, samlMetadaMan, client, timesConfig,
				offlineVerificator, groovyHook, "test");
	}

	@Test
	public void shouldChangeUserStatusToValidAfterSuccessfullOnlineVerification() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, EntityState.disabled,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(1));
		when(samlMetadaMan.getAttributeQueryAddressesAsMap()).thenReturn(Map.of("http://test.pl",
				new SAMLIdpInfo("http://test.pl", "http://test.pl/attr", "test@test.pl")));
		when(samlAttrQueryClient.getAttributes(eq("http://test.pl/attr"), eq("x1"))).thenReturn(Optional.of(
				Arrays.asList(new ParsedAttribute(StatusAttributeExtractor.SAML_STATUS_ATTRIBUTE_NAME,
						"", Arrays.asList("active")))));

		verificator.verifyUsers(Set.of(u1));

		verify(client).setUserStatus(eq(1L), eq(EntityState.valid));
		ArgumentCaptor<Attribute> argument = ArgumentCaptor.forClass(Attribute.class);
		verify(client, times(3)).updateAttribute(eq(1L), argument.capture());
		assertThat(argument.getAllValues().get(0).getName(),
				is(Constans.LAST_SUCCESS_HOME_IDP_VERIFICATION_ATTRIBUTE));
		assertThat(argument.getAllValues().get(1).getName(),
				is(Constans.FIRST_HOME_IDP_VERIFICATION_FAILURE_ATTRIBUTE));
		assertThat(argument.getAllValues().get(2).getName(),
				is(Constans.FIRST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE));

		verify(groovyHook).runHook(eq(u1), eq(EntityState.valid), any(), any());
	}

	@Test
	public void shouldSkipDeprovisioningWhenInOnlinePeriod() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, EntityState.disabled,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(8), null, null);
		when(samlMetadaMan.getAttributeQueryAddressesAsMap()).thenReturn(Map.of("http://test.pl",
				new SAMLIdpInfo("http://test.pl", "http://test.pl/attr", "test@test.pl")));
		when(samlAttrQueryClient.getAttributes(eq("http://test.pl/attr"), eq("x1")))
				.thenThrow(new InternalException());

		verificator.verifyUsers(Set.of(u1));

		verify(client, never()).scheduleRemoveUserWithLoginPermit(eq(1L), anyLong());
		verify(groovyHook, never()).runHook(eq(u1), eq(EntityState.valid), any(), any());
	}

	@Test
	public void shouldDisableUserAfterOfflinePeriod() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, EntityState.valid,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(1));
		UnityUser u2 = new UnityUser(2L, EntityState.valid,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x2", "test",
						"http://test.pl")),
				Set.of("/"), LocalDateTime.now().minusDays(2), null, LocalDateTime.now().minusDays(2),
				null, null);
		when(samlMetadaMan.getAttributeQueryAddressesAsMap()).thenReturn(Map.of("http://test.pl",
				new SAMLIdpInfo("http://test.pl", "http://test.pl/attr", "test@test.pl")));
		when(samlAttrQueryClient.getAttributes(eq("http://test.pl/attr"), eq("x1")))
				.thenThrow(new InternalException());

		verificator.verifyUsers(Set.of(u1, u2));

		verify(client).scheduleRemoveUserWithLoginPermit(eq(1L), anyLong());
		verify(groovyHook).runHook(eq(u1), eq(EntityState.onlyLoginPermitted), any(), any());
	}

	@Test
	public void shouldDisableUserWhenUnknownPrincipal() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, EntityState.valid,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(1));
		when(samlMetadaMan.getAttributeQueryAddressesAsMap()).thenReturn(Map.of("http://test.pl",
				new SAMLIdpInfo("http://test.pl", "http://test.pl/attr", "test@test.pl")));
		when(samlAttrQueryClient.getAttributes(eq("http://test.pl/attr"), eq("x1"))).thenThrow(
				new SAMLException("Error", new SAMLErrorResponseException(Status.STATUS_RESPONDER,
						SubStatus.STATUS2_UNKNOWN_PRINCIPIAL, new Exception())));

		verificator.verifyUsers(Set.of(u1));

		verify(client).setUserStatus(eq(1L), eq(EntityState.disabled));
		verify(client).scheduleRemoveUser(eq(1L), anyLong());
		verify(groovyHook).runHook(eq(u1), eq(EntityState.toRemove), any(), any());

	}

	@Test
	public void shouldGoToOfflineVerification() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, EntityState.valid,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1));

		when(samlMetadaMan.getAttributeQueryAddressesAsMap()).thenReturn(Map.of("http://test.pl",
				new SAMLIdpInfo("http://test.pl", "http://test.pl/attr", "test@test.pl")));
		when(samlAttrQueryClient.getAttributes(eq("http://test.pl/attr"), eq("x1")))
				.thenThrow(new InternalException());

		verificator.verifyUsers(Set.of(u1));

		verify(offlineVerificator).verify(eq(u1), eq("test@test.pl"));

	}
}
