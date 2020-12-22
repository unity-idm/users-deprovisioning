/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.deprovisionig.extractor.UnityUserExtractor;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.UnityUser;

@ExtendWith(MockitoExtension.class)
public class UnityUserExtractorTest
{
	@Mock
	private UnityApiClient client;

	private UnityUserExtractor extractor;
	private TimesConfiguration timesConfig;

	@BeforeEach
	public void init()
	{
		timesConfig = new TimesConfiguration(Duration.ofDays(2), Duration.ofDays(2), Duration.ofDays(2),
				Duration.ofDays(2), Duration.ofDays(2));
	}

	@Test
	public void shouldSkipByAuthenticationDate()
	{
		extractor = new UnityUserExtractor(client, timesConfig, "/A", new String[0], new String[0], "test");

		when(client.getUsers(eq("/A"))).thenReturn(Set.of(
				getUser(1, EntityState.valid, new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test"),
						Set.of("/", "/A"), LocalDateTime.now().minusDays(3),
						LocalDateTime.now().minusDays(3)),
				getUser(2, EntityState.valid, new Identity(Constans.IDENTIFIER_IDENTITY, "x2", "test"),
						Set.of("/", "/A"), LocalDateTime.now().minusDays(1),
						LocalDateTime.now().minusDays(3))));

		Set<UnityUser> extractUnityUsers = extractor.extractUnityUsers();
		assertThat(extractUnityUsers.size(), is(1));
		UnityUser user = extractUnityUsers.iterator().next();
		assertThat(user.entityId, is(1L));
	}

	@Test
	public void shouldSkipByLastSuccessHomeIdPVerificationDate()
	{
		extractor = new UnityUserExtractor(client, timesConfig, "/A", new String[] { "/B" }, new String[0],
				"test");

		when(client.getUsers(eq("/A"))).thenReturn(Set.of(
				getUser(1, EntityState.valid, new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test"),
						Set.of("/", "/A"), LocalDateTime.now().minusDays(3),
						LocalDateTime.now().minusDays(3)),
				getUser(2, EntityState.valid, new Identity(Constans.IDENTIFIER_IDENTITY, "x2", "test"),
						Set.of("/", "/A"), LocalDateTime.now().minusDays(3),
						LocalDateTime.now().minusDays(1))));

		Set<UnityUser> extractUnityUsers = extractor.extractUnityUsers();
		assertThat(extractUnityUsers.size(), is(1));
		UnityUser user = extractUnityUsers.iterator().next();
		assertThat(user.entityId, is(1L));

	}

	@Test
	public void shouldSkipByExcludedGroups()
	{

		extractor = new UnityUserExtractor(client, timesConfig, "/A", new String[] { "/B" }, new String[0],
				"test");

		when(client.getUsers(eq("/A"))).thenReturn(Set.of(
				getUser(1, EntityState.valid, new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test"),
						Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(3),
						LocalDateTime.now().minusDays(3)),
				getUser(2, EntityState.valid, new Identity(Constans.IDENTIFIER_IDENTITY, "x2", "test"),
						Set.of("/", "/A"), LocalDateTime.now().minusDays(3),
						LocalDateTime.now().minusDays(3))));

		Set<UnityUser> extractUnityUsers = extractor.extractUnityUsers();
		assertThat(extractUnityUsers.size(), is(1));

		UnityUser user = extractUnityUsers.iterator().next();
		assertThat(user.entityId, is(2L));
		assertThat(user.groups, is(Set.of("/", "/A")));

	}

	@Test
	public void shouldSkipByExcludedIdentity()
	{

		extractor = new UnityUserExtractor(client, timesConfig, "/A", new String[0],
				new String[] { "email::test@test.pl" }, "test");

		when(client.getUsers(eq("/A"))).thenReturn(Set.of(
				getUser(1, EntityState.valid, new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test"),
						Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(3),
						LocalDateTime.now().minusDays(3)),
				getUser(2, EntityState.valid,
						Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x2", "test"),
								new Identity("email", "test@test.pl", "test")),
						Set.of("/", "/A"), LocalDateTime.now().minusDays(3),
						LocalDateTime.now().minusDays(3))));

		Set<UnityUser> extractUnityUsers = extractor.extractUnityUsers();
		assertThat(extractUnityUsers.size(), is(1));

		UnityUser user = extractUnityUsers.iterator().next();
		assertThat(user.identities.size(), is(1));
		assertThat(user.entityId, is(1L));
	}

	@Test
	public void shouldGetOnlyFromRootGroup()
	{

		extractor = new UnityUserExtractor(client, timesConfig, "/B", new String[0], new String[0], "test");

		when(client.getUsers(eq("/B"))).thenReturn(Set.of(
				getUser(1, EntityState.valid, new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test"),
						Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(3),
						LocalDateTime.now().minusDays(3)),
				getUser(2, EntityState.valid, new Identity(Constans.IDENTIFIER_IDENTITY, "x2", "test"),
						Set.of("/", "/A"), LocalDateTime.now().minusDays(3),
						LocalDateTime.now().minusDays(3))));

		Set<UnityUser> extractUnityUsers = extractor.extractUnityUsers();
		assertThat(extractUnityUsers.size(), is(1));
		UnityUser user = extractUnityUsers.iterator().next();
		assertThat(user.entityId, is(1L));
		assertThat(user.groups, is(Set.of("/", "/A", "/B")));

	}

	UnityUser getUser(long entityId, EntityState status, Identity id, Set<String> groups,
			LocalDateTime lastAuthenticationTime, LocalDateTime lastSuccessHomeIdPVerification)
	{

		return getUser(entityId, status, Arrays.asList(id), groups, lastAuthenticationTime,
				lastSuccessHomeIdPVerification);

	}

	UnityUser getUser(long entityId, EntityState status, List<Identity> ids, Set<String> groups,
			LocalDateTime lastAuthenticationTime, LocalDateTime lastSuccessHomeIdPVerification)
	{

		return new UnityUser(entityId, status, ids, groups, lastAuthenticationTime, (LocalDateTime) null,
				lastSuccessHomeIdPVerification, (LocalDateTime) null, (LocalDateTime) null);

	}

}
