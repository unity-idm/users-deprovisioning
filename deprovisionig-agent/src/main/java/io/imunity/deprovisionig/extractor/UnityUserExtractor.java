/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.extractor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.TimesConfiguration;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
public class UnityUserExtractor
{
	private static final Logger log = LogManager.getLogger(UnityUserExtractor.class);

	private final UnityApiClient client;
	private final TimesConfiguration timesConfig;

	private final String unityRootGroup;
	private final String[] excludedGroups;
	private final String[] excludedUsers;
	private final String relatedTranslationProfile;

	@Autowired
	public UnityUserExtractor(UnityApiClient client, TimesConfiguration timesConfig,
			@Value("${unity.root.group:/}") String unityRootGroup,
			@Value("${unity.exclude.groups:}") String[] excludedGroups,
			@Value("${unity.exclude.users:}") String[] excludedUsers,
			@Value("${unity.identifier.relatedProfile:}") String relatedTranslationProfile)

	{
		this.client = client;
		this.timesConfig = timesConfig;
		this.excludedGroups = excludedGroups;
		this.unityRootGroup = unityRootGroup;
		this.excludedUsers = excludedUsers;
		this.relatedTranslationProfile = relatedTranslationProfile;
	}

	public Set<UnityUser> extractUnityUsers()
	{
		log.info("Starting extract users from unity");
		log.info("Excluded groups: " + Arrays.toString(excludedGroups));
		log.info("Excluded users: " + Arrays.toString(excludedUsers));
		log.info("Related users profile: " + relatedTranslationProfile);

		LocalDateTime now = LocalDateTime.now();

		Set<UnityUser> users = client.getUsers(unityRootGroup).stream().filter(u -> u.groups
				.contains(unityRootGroup)
				&& !u.groups.stream().anyMatch(Set.of(excludedGroups)::contains)
				&& !u.identities.stream().map(i -> i.getTypeId() + "::" + i.getValue())
						.anyMatch(Set.of(excludedUsers)::contains)
				&& u.identities.stream().filter(i -> i.getTypeId().equals(Constans.IDENTIFIER_IDENTITY)
						&& relatedTranslationProfile.equals(i.getTranslationProfile()))
						.count() > 0
				&& (u.lastAuthenticationTime == null || u.lastAuthenticationTime
						.plus(timesConfig.validAccountPeriod).isBefore(now))
				&& (u.lastSuccessHomeIdPVerification == null || u.lastSuccessHomeIdPVerification
						.plus(timesConfig.validAccountPeriod).isBefore(now)))
				.collect(Collectors.toSet());
		return users;
	}

}
