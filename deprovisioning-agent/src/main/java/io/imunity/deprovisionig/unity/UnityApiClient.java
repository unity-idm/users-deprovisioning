/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.unity.types.Attribute;
import io.imunity.deprovisionig.unity.types.Entity;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.MultiGroupMembers;
import io.imunity.deprovisionig.unity.types.UnityUser;
import io.imunity.deprovisionig.unity.types.MultiGroupMembers.EntityGroupAttributes;

@Component
public class UnityApiClient
{
	public static final String CUSTOM_EMAIL_VAR_PREFIX = "custom.";
	private static final Logger log = LogManager.getLogger(UnityApiClient.class);

	private final UnityRestClient client;

	@Autowired
	public UnityApiClient(UnityRestClient client)
	{

		this.client = client;
	}

	public void setUserStatus(long entityId, EntityState state)
	{
		try
		{
			client.put("/v1/entity/" + entityId + "/status/" + state.toString(), Optional.empty());
			log.info("Set status of user {} to {}", entityId, state);
		} catch (UnityException e)
		{
			log.error("Can not set status " + state.toString() + " of user" + entityId, e);
		}
	}

	public void scheduleRemoveUser(long entityId, long when)
	{
		try
		{
			client.put("/v1/entity/" + entityId + "/admin-schedule",
					Optional.of(Map.of("when", String.valueOf(when), "operation", "REMOVE")));
			log.info("Schedule remove of user (login permit) {} to {}", entityId,
					Instant.ofEpochMilli(when));
		} catch (UnityException e)
		{
			log.error("Can not shedule remove user", e);
		}
	}

	public void scheduleRemoveUserWithLoginPermit(long entityId, long when)
	{
		try
		{
			client.put("/v1/entity/" + entityId + "/removal-schedule",
					Optional.of(Map.of("when", String.valueOf(when))));
			log.info("Schedule remove of user (with login permit) to {} in linked Unity", entityId,
					Instant.ofEpochMilli(when));
		} catch (UnityException e)
		{
			log.error("Can not shedule remove user with login permit option", e);
		}
	}

	public void updateAttribute(long entityId, Attribute attribute)
	{
		try
		{
			client.put("/v1/entity/" + entityId + "/attribute", ContentType.APPLICATION_JSON,
					Optional.of(Constans.MAPPER.writeValueAsString(attribute)));
			log.info("Update attribute {} of user {},set value to {} in linked Unity", attribute.getName(),
					entityId, attribute.getValues());
		} catch (UnityException | JsonProcessingException e)
		{
			log.error("Can not update attribute " + attribute.getName() + " for user " + entityId, e);
		}

	}

	public void sendEmail(long entityId, String template, Map<String, String> params)
	{
		try
		{
			client.post("/v1/userNotification-trigger/entity/" + entityId + "/template/" + template,
					params.entrySet().stream()
							.collect(Collectors.toMap(
									e -> CUSTOM_EMAIL_VAR_PREFIX + e.getKey(),
									Map.Entry::getValue)));
			log.info("Send email to the user {} with params {}", entityId, params);
		} catch (UnityException e)
		{
			log.error("Can not send email via unity", e);
		}
	}

	public Set<UnityUser> getUsers(String group)
	{
		log.info("Getting users from group " + group);
		MultiGroupMembers groupMemebers = null;
		try
		{
			groupMemebers = Constans.MAPPER.readValue(client.post("/v1/group-members-multi/%2F",
					ContentType.APPLICATION_JSON, Optional.of("[]")), MultiGroupMembers.class);
		} catch (UnityException | JsonProcessingException e)
		{
			log.error("Can not get users from unity", e);
		}

		return mapToUnityUsers(groupMemebers, group);
	}

	private Set<UnityUser> mapToUnityUsers(MultiGroupMembers groupMemebers, String group)
	{
		Set<UnityUser> users = new HashSet<>();
		if (groupMemebers == null || groupMemebers.entities.isEmpty()
				|| groupMemebers.members.get(group) == null)
		{
			return users;
		}

		Set<Long> targetEntities = groupMemebers.members.get(group).stream().map(e -> e.entityId)
				.collect(Collectors.toSet());

		for (Long entity : targetEntities)
		{
			Optional<EntityGroupAttributes> entityRootAttrs = groupMemebers.members.get("/").stream()
					.filter(ea -> ea.entityId == entity).findAny();

			Entity entityFull = groupMemebers.entities.stream()
					.filter(e -> e.getEntityInformation().getEntityId() == entity).findAny().get();

			users.add(new UnityUser(entity,
					extractStringAttribute(Constans.DISPLAY_NAME_ATTRIBUTE, entityRootAttrs),
					entityFull.getEntityInformation().getState(), entityFull.getIdentities(),
					groupMemebers.members.entrySet().stream().filter(e -> e.getValue().stream()
							.filter(ea -> ea.entityId == entity).findAny().isPresent())
							.map(e -> e.getKey()).collect(Collectors.toSet()),
					extractDataTimeAttribute(Constans.LAST_AUTHENTICATION_ATTRIBUTE,
							entityRootAttrs),
					extractDataTimeAttribute(Constans.FIRST_HOME_IDP_VERIFICATION_FAILURE_ATTRIBUTE,
							entityRootAttrs),
					extractDataTimeAttribute(Constans.LAST_SUCCESS_HOME_IDP_VERIFICATION_ATTRIBUTE,
							entityRootAttrs),
					extractDataTimeAttribute(Constans.FIRST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE,
							entityRootAttrs),
					extractDataTimeAttribute(Constans.LAST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE,
							entityRootAttrs)));
		}

		return users;
	}

	private String extractStringAttribute(String attrName, Optional<EntityGroupAttributes> entityGroupAttributes)
	{
		if (entityGroupAttributes.isEmpty())
			return null;

		Optional<Attribute> attr = entityGroupAttributes.get().attributes.stream()
				.filter(a -> a.getName().equals(attrName)).findAny();
		if (attr.isPresent() && attr.get().getValues() != null && attr.get().getValues().size() > 0)
			return attr.get().getValues().get(0);

		return null;
	}

	private LocalDateTime extractDataTimeAttribute(String attrName,
			Optional<EntityGroupAttributes> entityGroupAttributes)
	{
		if (entityGroupAttributes.isEmpty())
			return null;

		Optional<Attribute> attr = entityGroupAttributes.get().attributes.stream()
				.filter(a -> a.getName().equals(attrName)).findAny();
		if (attr.isPresent() && attr.get().getValues() != null && attr.get().getValues().size() > 0)
			return LocalDateTime.parse(attr.get().getValues().get(0),
					DateTimeFormatter.ISO_LOCAL_DATE_TIME);

		return null;
	}
}
