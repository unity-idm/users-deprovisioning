/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package imunity.io.deprovisionig.extractor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import imunity.io.deprovisionig.Constans;
import imunity.io.deprovisionig.TimesConfiguration;
import imunity.io.deprovisionig.unity.UnityApiClient;
import imunity.io.deprovisionig.unity.types.UnityUser;

@Component
public class UnityUserExtractor
{
	private static final Logger log = LogManager.getLogger(UnityUserExtractor.class);

	private final UnityApiClient client;
	private final TimesConfiguration timesConfig;

	@Value("${unity.root.group}")
	private String unityRootGroup;

	@Value("${unity.exclude.groups:}")
	private String[] excludedGroups;

	@Value("${unity.exclude.users:}")
	private String[] excludedUsers;

	@Value("${unity.identifier.relatedProfile}")
	private String relatedTranslationProfile;

	@Autowired
	public UnityUserExtractor(UnityApiClient client, TimesConfiguration timesConfig)
	{
		super();
		this.client = client;
		this.timesConfig = timesConfig;
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
				&& (u.lastAuthenticationTime == null
						|| u.lastAuthenticationTime.plusDays(timesConfig.ttl()).isBefore(now))
				&& (u.lastHomeIdPVerification == null
						|| u.lastHomeIdPVerification.plusDays(timesConfig.ttl()).isBefore(now)))
				.collect(Collectors.toSet());
		return users;
	}

}
