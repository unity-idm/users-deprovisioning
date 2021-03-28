/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.hook.GroovyHookExecutor;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
class UserStatusUpdater
{
	private static final Logger log = LogManager.getLogger(UserStatusUpdater.class);
	private final GroovyHookExecutor groovyHook;
	private final UnityApiClient unityClient;
	private final DeprovisioningConfiguration config;

	@Autowired
	UserStatusUpdater(GroovyHookExecutor groovyHook, UnityApiClient unityClient, DeprovisioningConfiguration config)
	{
		this.groovyHook = groovyHook;
		this.unityClient = unityClient;
		this.config = config;
	}

	void changeUserStatusIfNeeded(UnityUser user, Identity identity, EntityState newStatus, SAMLIdpInfo idpInfo)
	{
		if (user.entityState.equals(newStatus))
		{
			log.debug("User status of {} {} remains unchanged ({})", user, identity, user.entityState);
			return;
		}

		log.info("Change user status of {} {} to {}", user, identity, newStatus.toString());

		Instant scheduledRemovalTime = null;
		if (newStatus.equals(EntityState.onlyLoginPermitted))
		{
			scheduledRemovalTime = getRemoveTime();
			unityClient.scheduleRemoveUserWithLoginPermit(user.entityId,
					scheduledRemovalTime.toEpochMilli());
		} else if (newStatus.equals(EntityState.toRemove))
		{
			scheduledRemovalTime = getRemoveTime();
			unityClient.setUserStatus(user.entityId, EntityState.disabled);
			unityClient.scheduleRemoveUser(user.entityId, scheduledRemovalTime.toEpochMilli());

		} else
		{
			unityClient.setUserStatus(user.entityId, newStatus);
		}

		groovyHook.runHook(user, newStatus, idpInfo, scheduledRemovalTime);
	}

	private Instant getRemoveTime()
	{
		return Instant.now().plus(config.removeUserCompletlyPeriod);
	}

}
