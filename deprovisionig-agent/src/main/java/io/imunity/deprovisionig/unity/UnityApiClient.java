/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity;

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
import io.imunity.deprovisionig.exception.UnityException;
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

	@Autowired
	private UnityRestClient client;

	public void setUserStatus(String identity, EntityState state)
	{
		try
		{
			client.put("/entity/" + identity + "/state/" + state.toString(), Optional.empty());
		} catch (UnityException e)
		{
			log.error("Can not set status " + state.toString() + " of user" + identity, e);
		}
	}

	public void scheduleRemoveUser(String identity, long when)
	{
		try
		{
			client.put("/entity/" + identity + "/removal-schedule",
					Optional.of(Map.of("when", String.valueOf(when))));
		} catch (UnityException e)
		{
			log.error("Can not shedule remove user with login permit option", e);
		}
	}

	public void scheduleRemoveUserWithLoginPermit(String identity, long when)
	{
		try
		{
			client.put("/entity/" + identity + "/admin-schedule",
					Optional.of(Map.of("when", String.valueOf(when), "operation", "REMOVE")));
		} catch (UnityException e)
		{
			log.error("Can not shedule remove user", e);
		}
	}

	public void updateAttribute(String identity, Attribute attribute)
	{
		try
		{
			client.put("/entity/" + identity + "/attribute", ContentType.APPLICATION_JSON,
					Optional.of(Constans.MAPPER.writeValueAsString(attribute)));
		} catch (UnityException | JsonProcessingException e)
		{
			log.error("Can not update attribute", e);
		}

	}

	public void sendEmail(String identity, String template, Map<String, String> params)
	{

		try
		{
			client.post("/userNotification-trigger/entity/" + identity + "/template/" + template,
					params.entrySet().stream()
							.collect(Collectors.toMap(
									e -> CUSTOM_EMAIL_VAR_PREFIX + e.getKey(),
									Map.Entry::getValue)));
		} catch (UnityException e)
		{
			log.error("Can not send email via unity", e);
		}

	}

	public Set<UnityUser> getUsers(String group)
	{
		MultiGroupMembers groupMemebers = null;
		try
		{
			groupMemebers = Constans.MAPPER.readValue(client.post("/group-members-multi/%2F",
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
		if (groupMemebers == null)
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

			users.add(new UnityUser(entity, entityFull.getEntityInformation().getState(),
					entityFull.getIdentities(),
					groupMemebers.members.entrySet().stream().filter(e -> e.getValue().stream()
							.filter(ea -> ea.entityId == entity).findAny().isPresent())
							.map(e -> e.getKey()).collect(Collectors.toSet()),
					extractDataTimeAttribute(Constans.LAST_AUTHENTICATION_ATTRIBUTE,
							entityRootAttrs),
					extractDataTimeAttribute(Constans.LAST_HOME_IDP_VERIFICATION_ATTRIBUTE,
							entityRootAttrs),
					extractDataTimeAttribute(Constans.LAST_OFFLINE_VERIFICATION_ATTRIBUTE,
							entityRootAttrs)));
		}

		return users;
	}

	private LocalDateTime extractDataTimeAttribute(String attrName,
			Optional<EntityGroupAttributes> entityGroupAttributes)
	{

		if (entityGroupAttributes.isEmpty())
			return null;

		Optional<Attribute> lastAuthAttr = entityGroupAttributes.get().attributes.stream()
				.filter(a -> a.getName().equals(attrName)).findAny();
		if (lastAuthAttr.isPresent())
			return LocalDateTime.parse(lastAuthAttr.get().getValues().get(0),
					DateTimeFormatter.ISO_LOCAL_DATE_TIME);

		return null;
	}
}
