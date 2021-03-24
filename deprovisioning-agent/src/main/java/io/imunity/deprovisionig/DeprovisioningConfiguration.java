/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig;

import java.time.Duration;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeprovisioningConfiguration
{
	// TIMES
	public final Duration validAccountPeriod;
	public final Duration onlineOnlyVerificationPeriod;
	public final Duration offlineVerificationPeriod;
	public final Duration emailResendPeriod;
	public final Duration removeUserCompletlyPeriod;

	//UNITY
	public final String unityRootGroup;
	public final Set<String> excludedGroups;
	public final Set<String> excludedUsers;
	public final String relatedTranslationProfile;
	public final String restUri;
	public final String restUsername;
	public final String restPassword;
	public final String emailTemplate;

	//HOOK
	public final String hookScript;

	public DeprovisioningConfiguration(@Value("${time.validAccountPeriod:14}") Duration validAccountPeriod,
			@Value("${time.onlineOnlyVerificationPeriod:14}") Duration onlineOnlyVerificationPeriod,
			@Value("${time.offlineVerificationPeriod:14}") Duration offlineVerificationPeriod,
			@Value("${time.emailResendPeriod:10}") Duration emailResendPeriod,
			@Value("${time.removeUserCompletlyPeriod:10}") Duration removeUserCompletlyPeriod,
			@Value("${hookScript:}") String hookScript,
			@Value("${unity.root.group:/}") String unityRootGroup,
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
		
		this.validAccountPeriod = validAccountPeriod;
		this.onlineOnlyVerificationPeriod = onlineOnlyVerificationPeriod;
		this.offlineVerificationPeriod = offlineVerificationPeriod;
		this.emailResendPeriod = emailResendPeriod;
		this.removeUserCompletlyPeriod = removeUserCompletlyPeriod;
		
		this.hookScript = hookScript;
	}

}
