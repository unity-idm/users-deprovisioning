/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.common.exception.SAMLException;
import io.imunity.deprovisionig.common.saml.AttributeQueryClient;
import io.imunity.deprovisionig.hook.GroovyHookExecutor;
import io.imunity.deprovisionig.saml.StatusAttributeExtractor;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.Attribute;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.UnityUser;

@ExtendWith(MockitoExtension.class)
public class OnlineVerificatorTest
{
	private OnlineVerificator verificator;

	@Mock
	private AttributeQueryClient samlAttrQueryClient;
	@Mock
	private UnityApiClient client;
	@Mock
	private GroovyHookExecutor groovyHook;

	@BeforeEach
	public void init()
	{
		DeprovisioningConfiguration config = new DeprovisioningConfiguration(Duration.ofDays(2),
				Duration.ofDays(3), Duration.ofDays(2), Duration.ofDays(2), Duration.ofDays(2), "test",
				"", new String[0], new String[0], "test", "", "", "", "");

		UserStatusUpdater userStatusUpdater = new UserStatusUpdater(groovyHook, client, config);
		verificator = new OnlineVerificator(samlAttrQueryClient, userStatusUpdater, client);
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

		when(samlAttrQueryClient.getAttributes(eq("http://test.pl/attr"), any())).thenReturn(Optional.of(
				Arrays.asList(new ParsedAttribute(StatusAttributeExtractor.SAML_STATUS_ATTRIBUTE_NAME,
						"", Arrays.asList("active")))));

		verificator.verify(u1, u1.identities.get(0),
				new SAMLIdpInfo("http://test.pl", "http://test.pl/attr", "test@test.pl"));

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
	public void shouldDisableUserWhenUnknownPrincipal() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, EntityState.valid,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(1));

		when(samlAttrQueryClient.getAttributes(eq("http://test.pl/attr"), any())).thenThrow(
				new SAMLException("Error", new SAMLErrorResponseException(Status.STATUS_RESPONDER,
						SubStatus.STATUS2_UNKNOWN_PRINCIPAL, new Exception())));

		verificator.verify(u1, u1.identities.get(0),
				new SAMLIdpInfo("http://test.pl", "http://test.pl/attr", "test@test.pl"));

		verify(client).setUserStatus(eq(1L), eq(EntityState.disabled));
		verify(client).scheduleRemoveUser(eq(1L), anyLong());
		verify(groovyHook).runHook(eq(u1), eq(EntityState.toRemove), any(), any());

	}
}
