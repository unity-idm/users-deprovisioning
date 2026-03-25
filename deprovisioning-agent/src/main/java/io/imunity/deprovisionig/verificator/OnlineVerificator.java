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
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.unity.types.Identity;
import io.imunity.deprovisionig.unity.types.LocalDateTimeAttribute;
import io.imunity.deprovisionig.unity.types.UnityUser;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.IdentityStatus;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.Status;

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
	
	
	OnlineVerificationStatus verify(UnityUser user, List<Identity> identities, SAMLIdpInfo samlIdpInfo)
	{
		List<OnlineIdentityVerificationStatus> results = identities.stream()
				.map(i -> verifySingleIdentity(user, i, samlIdpInfo))
				.collect(Collectors.toList());
		if (!results.stream().anyMatch(r -> r.status().equals(Status.success)))
		{
			return OnlineVerificationStatus.failure;
		}
		
		userStatusUpdater.changeUserStatusIfNeeded(user, identities,
				IdentitiesStatusToEntityStatusMapper.mapSuccessIdentityStatusToEntityState(results, user.entityState),
				samlIdpInfo);
		updateLastSuccessVerificationTime(user);
		
		return OnlineVerificationStatus.success;
	}
	
	private OnlineIdentityVerificationStatus verifySingleIdentity(UnityUser user, Identity identity, SAMLIdpInfo samlIdpInfo)
	{
		log.info("Online verification attempt of user {} {}", user, identity);
		Optional<List<ParsedAttribute>> attributes;
		try
		{
			attributes = samlAttrQueryClient.getAttributes(samlIdpInfo.attributeQueryServiceUrl.get(),
					new NameID(identity.getValue(), SAMLConstants.NFORMAT_PERSISTENT));

		} catch (SAMLException e)
		{
			if (!(e.getCause() instanceof SAMLErrorResponseException))
			{
				return fail(user, identity, e);
			}

			SAMLErrorResponseException cause = (SAMLErrorResponseException) e.getCause();
			if (SubStatus.STATUS2_UNKNOWN_PRINCIPAL.equals(cause.getSamlSubErrorId()))
			{
				return success(user, identity, IdentityStatus.toRemove, samlIdpInfo);
			} else
			{
				return fail(user, identity, e);

			}
		} catch (Exception e)
		{
			return fail(user, identity, e);
		}
		log.debug("Collected attributes for user {} {}: {}", user, identity,
				attributes.isPresent()
						? attributes.get().stream()
								.map(a -> a.getName() + "=" + a.getStringValues())
								.collect(Collectors.toList())
						: "empty");

		return success(user, identity, StatusAttributeExtractor
				.getStatusFromAttributesOrFallbackToUnknown(user, identity, attributes), samlIdpInfo);
	}

	private OnlineIdentityVerificationStatus fail(UnityUser user, Identity identity, Exception e)
	{
		log.info("Online verification failed for user {} {} ", user, identity);
		if (e != null)
		{
			log.debug("Online verification error:", e);
		}
		return new OnlineIdentityVerificationStatus(Status.failure, null);
	}
	
	private OnlineIdentityVerificationStatus success(UnityUser user, Identity identity, IdentityStatus state, SAMLIdpInfo samlIdpInfo)
	{
		log.info("Online verification successfull for user {}, identity {}, state {} ", user, identity, state);
		return new OnlineIdentityVerificationStatus(Status.success, state);
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
