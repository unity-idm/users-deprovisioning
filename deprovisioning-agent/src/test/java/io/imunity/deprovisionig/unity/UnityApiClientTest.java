/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.groovy.util.Maps;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.unity.types.Attribute;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.MultiGroupMembers;

@ExtendWith(MockitoExtension.class)
public class UnityApiClientTest
{
	@Mock
	private UnityRestClient restClient;

	private UnityApiClient apiClient;

	@BeforeEach
	public void init()
	{
		apiClient = new UnityApiClient(restClient);
	}

	@Test
	public void shouldCallStatusChange() throws UnityException
	{

		apiClient.setUserStatus(1, EntityState.valid);
		verify(restClient).put(eq("/entity/1/status/" + EntityState.valid.toString()), eq(Optional.empty()));
	}

	@Test
	public void shouldCallScheduleRemoveUser() throws UnityException
	{

		apiClient.scheduleRemoveUser(1, 2);
		verify(restClient).put(eq("/entity/1/admin-schedule"),
				eq(Optional.of(Map.of("operation", "REMOVE", "when", String.valueOf(2)))));
	}

	@Test
	public void shouldCallScheduleRemoveUserWithLoginPermit() throws UnityException
	{

		apiClient.scheduleRemoveUserWithLoginPermit(1, 2);
		verify(restClient).put(eq("/entity/1/removal-schedule"),
				eq(Optional.of(Map.of("when", String.valueOf(2)))));
	}

	@Test
	public void shouldCallUpdateAttribute() throws UnityException, JsonProcessingException
	{

		Attribute attr = new Attribute();
		attr.setName("name");
		attr.setValues(Arrays.asList("v1"));

		apiClient.updateAttribute(1, attr);

		verify(restClient).put(eq("/entity/1/attribute"), eq(ContentType.APPLICATION_JSON),
				eq(Optional.of(Constans.MAPPER.writeValueAsString(attr))));
	}

	@Test
	public void shouldCallNotificationTrigger() throws UnityException, JsonProcessingException
	{

		apiClient.sendEmail(1, "x", Maps.of("days", "1"));
		verify(restClient).post(eq("/userNotification-trigger/entity/1/template/x"),
				eq(Maps.of("custom.days", "1")));
	}

	@Test
	public void shouldCallGroupMembersMulti() throws UnityException, JsonProcessingException
	{
		when(restClient.post(eq("/group-members-multi/%2F"), eq(ContentType.APPLICATION_JSON),
				eq(Optional.of("[]"))))
						.thenReturn(Constans.MAPPER.writeValueAsString(new MultiGroupMembers(
								Collections.emptyList(), Collections.emptyMap())));
		apiClient.getUsers("/A");
		verify(restClient).post(eq("/group-members-multi/%2F"), eq(ContentType.APPLICATION_JSON),
				eq(Optional.of("[]")));
	}
}
