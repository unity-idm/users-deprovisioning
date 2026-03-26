/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.util.List;

import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.IdentityStatus;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.OnlineResponseStatus;

class SuccessfulIdentityVerificationResults
{
	private final List<OnlineIdentityVerificationStatus> results;

	SuccessfulIdentityVerificationResults(List<OnlineIdentityVerificationStatus> results)
	{
		if (results == null || results.isEmpty())
		{
			throw new IllegalArgumentException("Results cannot be null or empty");
		}

		boolean allSuccessful = results.stream()
				.allMatch(r -> r.status()
						.equals(OnlineResponseStatus.complete));

		if (!allSuccessful)
		{
			throw new IllegalArgumentException("All identity verification results must be successful");
		}

		this.results = List.copyOf(results);
	}

	EntityState toEntityState(EntityState fallbackState)
	{
		if (hasStatus(IdentityStatus.active))
		{
			return EntityState.valid;
		} else if (hasStatus(IdentityStatus.locked))
		{
			return EntityState.authenticationDisabled;
		} else if (hasStatus(IdentityStatus.deleted))
		{
			return EntityState.onlyLoginPermitted;
		} else if (hasStatus(IdentityStatus.disabled))
		{
			return EntityState.disabled;
		} else if (hasStatus(IdentityStatus.toRemove))
		{
			return EntityState.toRemove;
		}

		return fallbackState;
	}

	private boolean hasStatus(IdentityStatus status)
	{
		return results.stream()
				.anyMatch(r -> r.identityStatus()
						.equals(status));
	}
}
