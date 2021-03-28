/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.SAMLConstants.SubStatus;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLErrorResponseException;
import io.imunity.deprovisionig.Constans;
import io.imunity.deprovisionig.common.exception.SAMLException;
import io.imunity.deprovisionig.common.saml.AttributeQueryClient;
import io.imunity.deprovisionig.saml.StatusAttributeExtractor;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.LocalDateTimeAttribute;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
class OnlineVerificator
{
	private static final Logger log = LogManager.getLogger(OnlineVerificator.class);

	private final AttributeQueryClient samlAttrQueryClient;
	private final UserStatusUpdater userStatusUpdater;
	private final UnityApiClient unityClient;

	@Autowired
	OnlineVerificator(AttributeQueryClient samlAttrQueryClient, UserStatusUpdater userStatusUpdater,
			UnityApiClient unityClient)
	{
		this.samlAttrQueryClient = samlAttrQueryClient;
		this.userStatusUpdater = userStatusUpdater;
		this.unityClient = unityClient;
	}

	boolean verify(UnityUser user, Identity identity, SAMLIdpInfo samlIdpInfo)
	{
		log.debug("Online verification attempt of user " + identity);
		Optional<List<ParsedAttribute>> attributes;
		try
		{
			attributes = samlAttrQueryClient.getAttributes(samlIdpInfo.attributeQueryServiceUrl,
					new NameID(identity.getValue(), SAMLConstants.NFORMAT_PERSISTENT));

		} catch (SAMLException e)
		{
			log.debug("Can not get user attributes from attribute query service", e);
			if (!(e.getCause() instanceof SAMLErrorResponseException))
			{
				return false;
			}

			SAMLErrorResponseException cause = (SAMLErrorResponseException) e.getCause();
			if (SubStatus.STATUS2_UNKNOWN_PRINCIPAL.equals(cause.getSamlSubErrorId()))
			{
				return success(user, identity, EntityState.toRemove, samlIdpInfo);
			} else
			{
				return false;

			}
		} catch (Exception e)
		{
			log.debug("Online verification failure", e);
			return false;
		}
		log.debug("Collected attributes for user " + identity + ": "
				+ (attributes.isPresent()
						? attributes.get().stream()
								.map(a -> a.getName() + "=" + a.getStringValues())
								.collect(Collectors.toList())
						: "empty"));

		return success(user, identity, StatusAttributeExtractor
				.getStatusFromAttributesOrFallbackToUserStatus(user, attributes), samlIdpInfo);
	}

	private boolean success(UnityUser user, Identity identity, EntityState state, SAMLIdpInfo samlIdpInfo)
	{
		userStatusUpdater.changeUserStatusIfNeeded(user, identity, state, samlIdpInfo);
		updateLastSuccessVerificationTime(user);
		log.debug("Online verification successfull for user " + user.entityId);
		return true;
	}

	private void updateLastSuccessVerificationTime(UnityUser user)
	{
		unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
				.of(Constans.LAST_SUCCESS_HOME_IDP_VERIFICATION_ATTRIBUTE, LocalDateTime.now()));

		if (user.firstHomeIdpVerificationFailure != null)
		{
			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.FIRST_HOME_IDP_VERIFICATION_FAILURE_ATTRIBUTE, null));
		}

		if (user.firstOfflineVerificationAttempt != null)
		{
			unityClient.updateAttribute(user.entityId, LocalDateTimeAttribute
					.of(Constans.FIRST_OFFLINE_VERIFICATION_ATTEMPT_ATTRIBUTE, null));
		}
	}
}
