/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.common.saml.AttributeQueryClient;
import io.imunity.deprovisionig.hook.GroovyHookExecutor;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.saml.metadata.SAMLMetadataManager;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.UnityUser;

@ExtendWith(MockitoExtension.class)
public class VerificatorTest
{
	private UserVerificator verificator;
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
	@Mock
	private OnlineVerificator onlineVerificator;
	@Mock
	private UserStatusUpdater userStatusUpdater;

	private SAMLIdpInfo fullSamlIdpInfo;
	private SAMLIdpInfo samlIdpInfoWithotAttrQuery;

	@BeforeEach
	public void init()
	{
		DeprovisioningConfiguration config = new DeprovisioningConfiguration(Duration.ofDays(2),
				Duration.ofDays(3), Duration.ofDays(2), Duration.ofDays(2), Duration.ofDays(2), "test",
				"", new String[0], new String[0], "test", "", "", "", "");

		verificator = new UserVerificator(samlMetadaMan, client, offlineVerificator, config, userStatusUpdater,
				onlineVerificator);
		fullSamlIdpInfo = new SAMLIdpInfo("http://test.pl", Optional.of("http://test.pl/attr"), "test@test.pl");
		samlIdpInfoWithotAttrQuery = new SAMLIdpInfo("http://test.pl", Optional.empty(), "test@test.pl");
	}

	@Test
	public void shouldGotoOnlineVerification() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, "u1", EntityState.disabled,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(1));
		when(samlMetadaMan.getAttributeQueryAddressesAsMap())
				.thenReturn(Map.of("http://test.pl", fullSamlIdpInfo));
		when(onlineVerificator.verify(eq(u1), eq(u1.identities.get(0)), eq(fullSamlIdpInfo))).thenReturn(true);

		verificator.verifyUsers(Set.of(u1));
		verify(offlineVerificator, never()).verify(eq(u1), eq(u1.identities.get(0)), eq("test@test.pl"));
	}

	@Test
	public void shouldSkipDeprovisioningWhenInOnlinePeriod() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, "u1", EntityState.disabled,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(8), null, null);
		when(samlMetadaMan.getAttributeQueryAddressesAsMap())
				.thenReturn(Map.of("http://test.pl", fullSamlIdpInfo));

		verificator.verifyUsers(Set.of(u1));
		verify(userStatusUpdater, never()).changeUserStatusIfNeeded(eq(u1), eq(u1.identities.get(0)),
				eq(EntityState.onlyLoginPermitted), eq(fullSamlIdpInfo));
	}

	@Test
	public void shouldDisableUserAfterOfflinePeriod() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, "u1", EntityState.valid,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(1));
		UnityUser u2 = new UnityUser(2L, "u2", EntityState.valid,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x2", "test",
						"http://test.pl")),
				Set.of("/"), LocalDateTime.now().minusDays(2), null, LocalDateTime.now().minusDays(2),
				null, null);
		when(samlMetadaMan.getAttributeQueryAddressesAsMap())
				.thenReturn(Map.of("http://test.pl", fullSamlIdpInfo));

		verificator.verifyUsers(Set.of(u1, u2));

		verify(userStatusUpdater).changeUserStatusIfNeeded(eq(u1), eq(u1.identities.get(0)),
				eq(EntityState.onlyLoginPermitted), eq(fullSamlIdpInfo));

	}

	@Test
	public void shouldGoToOfflineVerificationAfterIneffectiveOnlineVerification() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, "u1",  EntityState.disabled,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(1));

		when(samlMetadaMan.getAttributeQueryAddressesAsMap())
				.thenReturn(Map.of("http://test.pl", fullSamlIdpInfo));

		when(onlineVerificator.verify(eq(u1), eq(u1.identities.get(0)), eq(fullSamlIdpInfo))).thenReturn(false);

		verificator.verifyUsers(Set.of(u1));

		verify(offlineVerificator).verify(eq(u1), eq(u1.identities.get(0)), eq("test@test.pl"));
	}

	@Test
	public void shouldSkipOnlineVerificationWhenAttrQueryServiceIsEmpty() throws Exception
	{
		UnityUser u1 = new UnityUser(1L, "u1", EntityState.disabled,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(8),
				LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(1));
		when(samlMetadaMan.getAttributeQueryAddressesAsMap())
				.thenReturn(Map.of("http://test.pl", samlIdpInfoWithotAttrQuery));

		verificator.verifyUsers(Set.of(u1));

		verify(onlineVerificator, never()).verify(eq(u1), eq(u1.identities.get(0)), eq(fullSamlIdpInfo));
		verify(offlineVerificator).verify(eq(u1), eq(u1.identities.get(0)), eq("test@test.pl"));
	}

}
