/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.saml;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.attrprofile.ParsedAttribute;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.UnityUser;
import io.imunity.deprovisionig.verificator.MainUserVerificator;

public class StatusAttributeExtractor
{
	private static final Logger log = LogManager.getLogger(MainUserVerificator.class);
	
	public static final String SAML_STATUS_ATTRIBUTE_NAME = "urn:oid:1.3.6.1.4.1.25178.1.2.19";
	public static final String SAML_STATUS_ATTRIBUTE_SCHAC_PREFIX = "urn:schac:userStatus:";
	
	public static EntityState getStatusFromAttributesOrFallbackToUserStatus(UnityUser user,
			Optional<List<ParsedAttribute>> attributes)
	{
		if (attributes.isEmpty())
		{
			return user.entityState;
		}

		Optional<ParsedAttribute> statusAttr = attributes.get().stream()
				.filter(a -> a.getName().equals(SAML_STATUS_ATTRIBUTE_NAME)
						|| a.getName().startsWith(SAML_STATUS_ATTRIBUTE_SCHAC_PREFIX))
				.findAny();

		return mapToUnityStatusOrFallbackToUserStatus(user, statusAttr);
	}

	private static EntityState mapToUnityStatusOrFallbackToUserStatus(UnityUser user, Optional<ParsedAttribute> status)
	{
		if (status.isEmpty() || status.get().getStringValues().isEmpty())
		{
			log.debug("No status attributes in saml response  for user " + user.entityId);
			return user.entityState;
		}

		String statusL = Stream.of(status.get().getStringValues().get(0).toLowerCase().split(":")).reduce((first,last)->last).get();

		if ("active".equals(statusL))
		{
			return EntityState.valid;
		} else if ("locked".equals(statusL))
		{
			return EntityState.authenticationDisabled;
		} else if ("disabled".equals(statusL))
		{
			return EntityState.disabled;
		} else if ("deleted".equals(statusL))
		{
			return EntityState.onlyLoginPermitted;
		}

		log.debug("Can not interpret new status of user " + user.entityId + " status=" + statusL);
		return user.entityState;
	}
}
