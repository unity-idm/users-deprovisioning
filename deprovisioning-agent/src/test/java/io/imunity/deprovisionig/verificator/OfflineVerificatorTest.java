/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.Attribute;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.UnityUser;

@ExtendWith(MockitoExtension.class)
public class OfflineVerificatorTest
{
	private OfflineVerificator verificator;

	@Mock
	private UnityApiClient client;

	@BeforeEach
	public void init()
	{

		DeprovisioningConfiguration config = new DeprovisioningConfiguration(Duration.ofDays(2),
				Duration.ofDays(3), Duration.ofDays(10), Duration.ofDays(4), Duration.ofDays(2), "test",
				"", new String[0], new String[0], "test", "", "", "", "test");

		verificator = new OfflineVerificator(client, config);
	}

	@Test
	public void shouldSkipWhenAfterOfflinePeriod()
	{

		UnityUser u1 = new UnityUser(1L, "u1", EntityState.disabled,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(11),
				LocalDateTime.now().minusDays(11), LocalDateTime.now().minusDays(11),
				LocalDateTime.now().minusDays(11), LocalDateTime.now().minusDays(4));

		verificator.verify(u1, u1.identities.get(0), "test@test.pl");

		verify(client, never()).sendEmail(eq(1L), eq("test"), any());

	}

	@Test
	public void shouldSendEmailAndUpdateAttribute()
	{

		UnityUser u1 = new UnityUser(1L, "u1", EntityState.disabled,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(11),
				LocalDateTime.now().minusDays(11), LocalDateTime.now().minusDays(11),
				LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(4));

		verificator.verify(u1, u1.identities.get(0), "test@test.pl");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> emailArgs = ArgumentCaptor.forClass(Map.class);

		verify(client).sendEmail(eq(1L), eq("test"), emailArgs.capture());
		assertThat(emailArgs.getValue().get("email"), is("test@test.pl"));
		assertThat(emailArgs.getValue().get("daysLeft"), is("4"));

		ArgumentCaptor<Attribute> argument = ArgumentCaptor.forClass(Attribute.class);
		verify(client).updateAttribute(eq(1L), argument.capture());
		assertThat(argument.getValue().getName(), is(Constans.LAST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE));

	}

	@Test
	public void shouldSkipSendEmailDueToEmailResendPeriod()
	{

		UnityUser u1 = new UnityUser(1L, "u1", EntityState.disabled,
				Arrays.asList(new Identity(Constans.IDENTIFIER_IDENTITY, "x1", "test",
						"http://test.pl")),
				Set.of("/", "/A", "/B"), LocalDateTime.now().minusDays(11),
				LocalDateTime.now().minusDays(11), LocalDateTime.now().minusDays(11),
				LocalDateTime.now().minusDays(9), LocalDateTime.now().minusDays(3));

		verificator.verify(u1, u1.identities.get(0), "test@test.pl");

		verify(client, never()).sendEmail(eq(1L), eq("test"), any());
	}
}
