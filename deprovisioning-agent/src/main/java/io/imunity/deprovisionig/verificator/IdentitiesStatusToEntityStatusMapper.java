/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.util.List;

import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.IdentityStatus;

class IdentitiesStatusToEntityStatusMapper
{
	static EntityState mapSuccessIdentityStatusToEntityState(List<OnlineIdentityVerificationStatus> results,
			EntityState fallbackState)
	{
		if (results.stream()
				.anyMatch(r -> r.identityStatus().equals(IdentityStatus.active)))
		{
			return EntityState.valid;
		} else if (results.stream()
				.anyMatch(r -> r.identityStatus().equals(IdentityStatus.locked)))
		{
			return EntityState.authenticationDisabled;
		} else if (results.stream()
				.anyMatch(r -> r.identityStatus().equals(IdentityStatus.deleted)))
		{
			return EntityState.onlyLoginPermitted;
		} else if (results.stream()
				.anyMatch(r -> r.identityStatus().equals(IdentityStatus.disabled)))
		{
			return EntityState.disabled;
		} else if (results.stream()
				.anyMatch(r -> r.identityStatus().equals(IdentityStatus.toRemove)))
		{
			return EntityState.toRemove;
		} else
		{
			return fallbackState;
		}
	}
}
