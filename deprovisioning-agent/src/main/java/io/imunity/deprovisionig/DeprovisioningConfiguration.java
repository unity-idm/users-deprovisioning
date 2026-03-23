/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.unicore.util.httpclient.HttpClientProperties;

@Component
public class DeprovisioningConfiguration
{
	// TIMES
	public final Duration validAccountPeriod;
	public final Duration onlineOnlyVerificationPeriod;
	public final Duration offlineVerificationPeriod;
	public final Duration emailResendPeriod;
	public final Duration removeUserCompletlyPeriod;

	// UNITY
	public final String unityRootGroup;
	public final Set<String> excludedGroups;
	public final Set<String> excludedUsers;
	public final Set<String> inputProfilesForOnlineProcessing;
	public final Set<String> inputProfilesForOfflineProcessingOnly;
	public final String restUri;
	public final String restUsername;
	public final String restPassword;
	public final String emailTemplate;

	// HTTP
	public final int maxTotalConnections;
	public final int maxPerRouteConnections;
	public final int socketTimeout;
	public final int connectionTimeout;

	// HOOK
	public final String hookScript;

	public final String fallbackOfflineVerificationAdminEmail;
	
	public final Map<String, String> idpNames;

	public DeprovisioningConfiguration(@Value("${time.validAccountPeriod:14}") Duration validAccountPeriod,
			@Value("${time.onlineOnlyVerificationPeriod:14}") Duration onlineOnlyVerificationPeriod,
			@Value("${time.offlineVerificationPeriod:14}") Duration offlineVerificationPeriod,
			@Value("${time.emailResendPeriod:10}") Duration emailResendPeriod,
			@Value("${time.removeUserCompletlyPeriod:10}") Duration removeUserCompletlyPeriod,
			@Value("${hookScript:}") String hookScript,
			@Value("${unity.root.group:/}") String unityRootGroup,
			@Value("${unity.exclude.groups:}") String[] excludedGroups,
			@Value("${unity.exclude.users:}") String[] excludedUsers,
			@Value("${unity.identifier.inputProfilesForOnlineProcessing:}") Set<String> inputProfilesForOnlineProcessing,
			@Value("${unity.identifier.inputProfilesForOfflineProcessingOnly:}") Set<String> inputProfilesForOfflineProcessingOnly,
			@Value("${unity.rest.uri}") String restUri,
			@Value("${unity.rest.client.username}") String restUsername,
			@Value("${unity.rest.client.password}") String restPassword,
			@Value("${unity.email.template:userDeprovisioning}") String emailTemplate,
			@Value("${fallbackOfflineVerificationAdminEmail:}") String fallbackOfflineVerificationAdminEmail,
			@Value("${unity.http.maxTotalConnections:20}") int maxTotalConnections,
			@Value("${unity.http.maxPerRouteConnections:6}") int maxPerRouteConnections,
			@Value("${unity.http.socket.timeout:0}") int socketTimeout,
			@Value("${unity.http.connection.timeout:20000}") int connectionTimeout,
			@Value("#{${unity.idp.names:{}}}") Map<String, String> idpNames)
	
	{
		this.unityRootGroup = unityRootGroup;
		this.excludedGroups = Set.of(excludedGroups);
		this.excludedUsers = Set.of(excludedUsers);
		this.inputProfilesForOnlineProcessing = inputProfilesForOnlineProcessing;
		this.inputProfilesForOfflineProcessingOnly = inputProfilesForOfflineProcessingOnly;
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

		this.fallbackOfflineVerificationAdminEmail = fallbackOfflineVerificationAdminEmail;

		this.maxTotalConnections = maxTotalConnections;
		this.maxPerRouteConnections = maxPerRouteConnections;
		this.socketTimeout = socketTimeout;
		this.connectionTimeout = connectionTimeout;
		
		this.idpNames = idpNames;
	}

	public HttpClientProperties getHttpClientProperties()
	{
		Properties properties = new Properties();
		properties.setProperty(HttpClientProperties.PREFIX + HttpClientProperties.MAX_TOTAL_CONNECTIONS,
				String.valueOf(maxTotalConnections));
		properties.setProperty(HttpClientProperties.PREFIX + HttpClientProperties.MAX_HOST_CONNECTIONS,
				String.valueOf(maxPerRouteConnections));
		properties.setProperty(HttpClientProperties.PREFIX + HttpClientProperties.SO_TIMEOUT,
				String.valueOf(socketTimeout));
		properties.setProperty(HttpClientProperties.PREFIX + HttpClientProperties.CONNECT_TIMEOUT,
				String.valueOf(connectionTimeout));
		HttpClientProperties httpClientProperties = new HttpClientProperties(properties);
		return httpClientProperties;
	}
}
