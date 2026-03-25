/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

record OnlineIdentityVerificationStatus(
		Status status,
		IdentityStatus identityStatus)
{
	enum Status
	{
		success, failure
	}

	enum IdentityStatus
	{
		toRemove, unknown, active, locked, disabled, deleted
	}
}