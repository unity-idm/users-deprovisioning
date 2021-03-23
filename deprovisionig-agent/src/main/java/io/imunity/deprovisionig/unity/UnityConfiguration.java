/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.unity;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UnityConfiguration
{
	
	public final String unityRootGroup;
	public final Set<String> excludedGroups;
	public final Set<String> excludedUsers;
	public final String relatedTranslationProfile;
	public final String restUri;
	public final String restUsername;
	public final String restPassword;
	public final String emailTemplate;
	
	
	public UnityConfiguration(@Value("${unity.root.group:/}") String unityRootGroup,
			@Value("${unity.exclude.groups:}") String[] excludedGroups,
			@Value("${unity.exclude.users:}") String[] excludedUsers,
			@Value("${unity.identifier.relatedProfile:}") String relatedTranslationProfile,
			@Value("${unity.rest.uri}") String restUri,
			@Value("${unity.rest.client.username}") String restUsername,
			@Value("${unity.rest.client.password}") String restPassword,
			@Value("${unity.email.template:userDeprovisioning}") String emailTemplate)
	{
		this.unityRootGroup = unityRootGroup;
		this.excludedGroups = Set.of(excludedGroups);
		this.excludedUsers  = Set.of(excludedUsers);
		this.relatedTranslationProfile = relatedTranslationProfile;
		this.restUri = restUri;
		this.restUsername = restUsername;
		this.restPassword = restPassword;
		this.emailTemplate = emailTemplate;
	}
}
