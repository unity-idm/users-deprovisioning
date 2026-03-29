/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.hook.GroovyHookExecutor;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.UnityUser;

@ExtendWith(MockitoExtension.class)
class UserStatusUpdaterTest
{
	@Mock
	private GroovyHookExecutor groovyHook;

	@Mock
	private UnityApiClient unityClient;

	private DeprovisioningConfiguration config;

	private UserStatusUpdater updater;

	@BeforeEach
	void setUp()
	{
		config = new DeprovisioningConfiguration(Duration.ofDays(2), Duration.ofDays(3), Duration.ofDays(2),
				Duration.ofDays(2), Duration.ofDays(2), "test", "", new String[0], new String[0],
				Set.of("onlineProfile"), Set.of("offlineOnly"), "", "", "", "test", "fallbackAdmin@demo.com", 20, 6, 0,
				20000, new HashMap<>());

		updater = new UserStatusUpdater(groovyHook, unityClient, config);
	}

	@Test
	void testStatusUnchanged()
	{
		UnityUser user = createUser(1L, EntityState.valid);
		IdentitiesFromSingleIdp identities = createIdentities();

		updater.changeUserStatusIfNeeded(user, identities, EntityState.valid);

		verify(unityClient, never()).clearScheduledOperation(anyLong());
		verify(unityClient, never()).setUserStatus(anyLong(), any());
		verify(groovyHook, never()).runHook(any(), any(), any(), any());
	}

	@Test
	void testChangeStatusToOnlyLoginPermitted()
	{
		UnityUser user = createUser(1L, EntityState.valid);
		IdentitiesFromSingleIdp identities = createIdentities();

		updater.changeUserStatusIfNeeded(user, identities, EntityState.onlyLoginPermitted);

		verify(unityClient, times(1)).clearScheduledOperation(1L);
		verify(unityClient, times(1)).scheduleRemoveUserWithLoginPermit(eq(1L), anyLong());
		verify(groovyHook, times(1)).runHook(eq(user), eq(EntityState.onlyLoginPermitted), eq(identities.idpInfo),
				any());
	}

	@Test
	void testChangeStatusToRemove()
	{
		UnityUser user = createUser(2L, EntityState.valid);
		IdentitiesFromSingleIdp identities = createIdentities();

		updater.changeUserStatusIfNeeded(user, identities, EntityState.toRemove);

		verify(unityClient, times(1)).clearScheduledOperation(2L);
		verify(unityClient, times(1)).setUserStatus(2L, EntityState.disabled);
		verify(unityClient, times(1)).scheduleRemoveUser(eq(2L), anyLong());
		verify(groovyHook, times(1)).runHook(eq(user), eq(EntityState.toRemove), eq(identities.idpInfo), any());
	}

	@Test
	void testChangeStatusToOtherStates()
	{
		UnityUser user = createUser(3L, EntityState.valid);
		IdentitiesFromSingleIdp identities = createIdentities();

		updater.changeUserStatusIfNeeded(user, identities, EntityState.disabled);

		verify(unityClient, times(1)).clearScheduledOperation(3L);
		verify(unityClient, times(1)).setUserStatus(3L, EntityState.disabled);
		verify(unityClient, never()).scheduleRemoveUser(anyLong(), anyLong());
		verify(unityClient, never()).scheduleRemoveUserWithLoginPermit(anyLong(), anyLong());
		verify(groovyHook, times(1)).runHook(eq(user), eq(EntityState.disabled), eq(identities.idpInfo), any());
	}

	private UnityUser createUser(long entityId, EntityState state)
	{
		return new UnityUser(entityId, null, state, null, null, null, null, null, null, null);
	}

	private IdentitiesFromSingleIdp createIdentities()
	{
		return new IdentitiesFromSingleIdp(
				List.of(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test", "http://test.pl")), Map.of());
	}
}
