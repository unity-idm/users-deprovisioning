/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
public class UnityUserExtractor {
	private static final Logger log = LogManager.getLogger(UnityUserExtractor.class);

	private final UnityApiClient client;

	private final DeprovisioningConfiguration config;

	@Autowired
	public UnityUserExtractor(UnityApiClient client, DeprovisioningConfiguration config)

	{
		this.client = client;
		this.config = config;
	}

	public Set<UnityUser> extractUnityUsers() {
		log.info("Starting extraction of users from unity");
		log.info("Excluded groups: {}", config.excludedGroups);
		log.info("Excluded users: {}", config.excludedUsers);
		log.info("Input profiles for online processing: {}", config.inputProfilesForOnlineProcessing);
		log.info("Input profiles for offline processing only: {}", config.inputProfilesForOfflineProcessingOnly);

		
		LocalDateTime now = LocalDateTime.now();

		return client.getUsers(config.unityRootGroup).stream()
				.filter(u -> u.groups.contains(config.unityRootGroup))
				.filter(notInExludedGroups())
				.filter(notInExludedUsers())
				.filter(hasIdentitiesWithInputProfiles())
				.filter(hasValidAuthenticationTime(now))
				.filter(hasValidLastSuccessHomeIdPVerificationTime(now))
				.collect(Collectors.toSet());
	}
	
	private Predicate<UnityUser> hasIdentitiesWithInputProfiles()
	{
		return u -> u.identities.stream().filter(i -> i.getTypeId().equals(Constans.IDENTIFIER_IDENTITY)
				&& (config.inputProfilesForOnlineProcessing.contains(i.getTranslationProfile())
						|| config.inputProfilesForOfflineProcessingOnly
								.contains(i.getTranslationProfile())))
				.count() > 0;
	}

	private Predicate<UnityUser> hasValidAuthenticationTime(LocalDateTime now)
	{
		return u -> u.lastAuthenticationTime == null
				|| u.lastAuthenticationTime.plus(config.validAccountPeriod).isBefore(now);
	}

	private Predicate<UnityUser> hasValidLastSuccessHomeIdPVerificationTime(LocalDateTime now)
	{
		return u -> u.lastSuccessHomeIdPVerification == null
				|| u.lastSuccessHomeIdPVerification.plus(config.validAccountPeriod).isBefore(now);
	}

	private Predicate<UnityUser> notInExludedGroups()
	{
		return u -> !u.groups.stream().anyMatch(config.excludedGroups::contains);
	}

	private Predicate<UnityUser> notInExludedUsers()
	{
		return u -> !u.identities.stream().map(i -> i.getTypeId() + "::" + i.getValue())
				.anyMatch(config.excludedUsers::contains);
	}

}
