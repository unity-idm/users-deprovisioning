/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
public class UnityUserExtractor
{
	private static final Logger log = LogManager.getLogger(UnityUserExtractor.class);

	private final UnityApiClient client;

	private final DeprovisioningConfiguration config;

	@Autowired
	public UnityUserExtractor(UnityApiClient client, DeprovisioningConfiguration config)

	{
		this.client = client;
		this.config = config;
	}

	public Set<UnityUser> extractUnityUsers()
	{
		log.info("Starting extraction of users from unity");
		log.info("Excluded groups: " + config.excludedGroups);
		log.info("Excluded users: " + config.excludedUsers);
		log.info("Related users profile: " + config.relatedTranslationProfile);

		LocalDateTime now = LocalDateTime.now();

		return client.getUsers(config.unityRootGroup).stream()
				.filter(u -> u.groups.contains(config.unityRootGroup))
				.filter(u -> !u.groups.stream().anyMatch(config.excludedGroups::contains))

				.filter(u -> !u.identities.stream().map(i -> i.getTypeId() + "::" + i.getValue())
						.anyMatch(config.excludedUsers::contains))
				.filter(u -> u.identities.stream()
						.filter(i -> i.getTypeId().equals(Constans.IDENTIFIER_IDENTITY)
								&& config.relatedTranslationProfile
										.equals(i.getTranslationProfile()))
						.count() > 0)
				.filter(u -> u.lastAuthenticationTime == null || u.lastAuthenticationTime
						.plus(config.validAccountPeriod).isBefore(now))
				.filter(u -> u.lastSuccessHomeIdPVerification == null
						|| u.lastSuccessHomeIdPVerification.plus(config.validAccountPeriod)
								.isBefore(now))
				.collect(Collectors.toSet());
	}
}
