/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.IdentityStatus;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.Status;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentitiesStatusToEntityStatusMapperTest
{
	@Test
	void shouldReturnValidWhenAnyIdentityIsActive()
	{
		List<OnlineIdentityVerificationStatus> results = List.of(status(IdentityStatus.locked),
				status(IdentityStatus.active));

		EntityState result = IdentitiesStatusToEntityStatusMapper.mapSuccessIdentityStatusToEntityState(results,
				EntityState.disabled);

		assertEquals(EntityState.valid, result);
	}

	@Test
	void shouldReturnAuthenticationDisabledWhenLockedAndNoActive()
	{
		List<OnlineIdentityVerificationStatus> results = List.of(status(IdentityStatus.locked),
				status(IdentityStatus.disabled));

		EntityState result = IdentitiesStatusToEntityStatusMapper.mapSuccessIdentityStatusToEntityState(results,
				EntityState.valid);

		assertEquals(EntityState.authenticationDisabled, result);
	}

	@Test
	void shouldReturnOnlyLoginPermittedWhenDeletedAndNoHigherPriority()
	{
		List<OnlineIdentityVerificationStatus> results = List.of(status(IdentityStatus.deleted));

		EntityState result = IdentitiesStatusToEntityStatusMapper.mapSuccessIdentityStatusToEntityState(results,
				EntityState.valid);

		assertEquals(EntityState.onlyLoginPermitted, result);
	}

	@Test
	void shouldReturnDisabledWhenDisabledAndNoHigherPriority()
	{
		List<OnlineIdentityVerificationStatus> results = List.of(status(IdentityStatus.disabled));

		EntityState result = IdentitiesStatusToEntityStatusMapper.mapSuccessIdentityStatusToEntityState(results,
				EntityState.valid);

		assertEquals(EntityState.disabled, result);
	}

	@Test
	void shouldReturnToRemoveWhenToRemoveAndNoHigherPriority()
	{
		List<OnlineIdentityVerificationStatus> results = List.of(status(IdentityStatus.toRemove));

		EntityState result = IdentitiesStatusToEntityStatusMapper.mapSuccessIdentityStatusToEntityState(results,
				EntityState.valid);

		assertEquals(EntityState.toRemove, result);
	}

	@Test
	void shouldReturnFallbackWhenNoStatusesMatch()
	{
		List<OnlineIdentityVerificationStatus> results = List.of();

		EntityState result = IdentitiesStatusToEntityStatusMapper.mapSuccessIdentityStatusToEntityState(results,
				EntityState.disabled);

		assertEquals(EntityState.disabled, result);
	}

	@Test
	void shouldRespectPriorityOrder()
	{
		List<OnlineIdentityVerificationStatus> results = List.of(status(IdentityStatus.toRemove),
				status(IdentityStatus.disabled), status(IdentityStatus.active));

		EntityState result = IdentitiesStatusToEntityStatusMapper.mapSuccessIdentityStatusToEntityState(results,
				EntityState.authenticationDisabled);

		assertEquals(EntityState.valid, result);
	}

	private OnlineIdentityVerificationStatus status(IdentityStatus identityStatus)
	{
		return new OnlineIdentityVerificationStatus(Status.success, identityStatus);
	}
}