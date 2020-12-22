/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface Constans
{
	public static final ObjectMapper MAPPER = new ObjectMapper();

	public static final String LAST_AUTHENTICATION_ATTRIBUTE = "sys:LastAuthentication";
	
	public static final String LAST_SUCCESS_HOME_IDP_VERIFICATION_ATTRIBUTE = "deprovisioning:lastSuccessHomeIdPVerificationTS";
	public static final String FIRST_HOME_IDP_VERIFICATION_FAILURE_ATTRIBUTE = "deprovisioning:firstHomeIdpVerificationFailureTS";
	
	public static final String LAST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE = "deprovisioning:lastOfflineVerificationAttemptTS";
	public static final String FIRST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE = "deprovisioning:firstOfflineVerificationAttemptTS";

	public static final String IDENTIFIER_IDENTITY = "identifier";

}
