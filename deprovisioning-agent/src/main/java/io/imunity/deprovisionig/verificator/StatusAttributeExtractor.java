/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.attrprofile.ParsedAttribute;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.UnityUser;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.IdentityStatus;

class StatusAttributeExtractor
{
	private static final Logger log = LogManager.getLogger(StatusAttributeExtractor.class);

	public static final String SAML_STATUS_ATTRIBUTE_NAME = "urn:oid:1.3.6.1.4.1.25178.1.2.19";
	public static final String SAML_STATUS_ATTRIBUTE_SCHAC_PREFIX = "urn:schac:userStatus:";

	static IdentityStatus getStatusFromAttributesOrFallbackToUnknown(UnityUser user, Identity identity,
			Optional<List<ParsedAttribute>> attributes)
	{
		if (attributes.isEmpty())
		{
			log.debug("No status attributes in saml response for identity {} (of user {})", identity, user);
			return IdentityStatus.unknown;
		}

		Optional<ParsedAttribute> statusAttr = attributes.get().stream()
				.filter(a -> a.getName().equals(SAML_STATUS_ATTRIBUTE_NAME)
						|| a.getName().startsWith(SAML_STATUS_ATTRIBUTE_SCHAC_PREFIX))
				.findAny();

		return mapToUnityStatusOrFallbackToUnknown(user, identity, statusAttr);
	}

	private static IdentityStatus mapToUnityStatusOrFallbackToUnknown(UnityUser user, Identity identity,
			Optional<ParsedAttribute> status)
	{
		if (!status.isPresent())
		{
			log.debug("No status attributes in saml response  for identity {} (of user {})", identity, user);
			return IdentityStatus.active;
		}

		if (status.get().getStringValues().isEmpty())
		{
			log.debug("Empty status attribute values in saml response  for identity {} (of user {})", identity, user);
			return IdentityStatus.unknown;
		}

		String statusL = Stream.of(status.get().getStringValues().get(0).toLowerCase().split(":"))
				.reduce((first, last) -> last).get();

		if ("active".equals(statusL))
		{
			return IdentityStatus.active;
		} else if ("locked".equals(statusL))
		{
			return IdentityStatus.locked;
		} else if ("disabled".equals(statusL))
		{
			return IdentityStatus.disabled;
		} else if ("deleted".equals(statusL))
		{
			return IdentityStatus.deleted;
		}

		log.warn("Can not interpret new status for identity {} (of user {}), status {}", identity, user, statusL);
		return IdentityStatus.unknown;
	}
}
