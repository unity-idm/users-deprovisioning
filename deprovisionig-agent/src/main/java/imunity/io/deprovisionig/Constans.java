/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package imunity.io.deprovisionig;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface Constans
{
	public static final ObjectMapper MAPPER = new ObjectMapper();
	
	public static final String LAST_AUTHENTICATION_ATTRIBUTE = "sys:LastAuthentication";
	public static final String LAST_HOME_IDP_VERIFICATION_ATTRIBUTE  = "deprovisioning:LastHomeIdPVerification";
	public static final String LAST_OFFLINE_VERIFICATION_ATTRIBUTE  = "deprovisioning:LastOfflineVerification";
	
	public static final String IDENTIFIER_IDENTITY  = "identifier";
	
}
