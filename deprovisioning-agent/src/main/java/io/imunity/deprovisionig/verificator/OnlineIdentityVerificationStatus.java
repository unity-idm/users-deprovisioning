/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

record OnlineIdentityVerificationStatus(
		OnlineResponseStatus status,
		IdentityStatus identityStatus)
{
	enum OnlineResponseStatus
	{
		complete, not_complete
	}

	enum IdentityStatus
	{
		toRemove, unknown, active, locked, disabled, deleted
	}
}