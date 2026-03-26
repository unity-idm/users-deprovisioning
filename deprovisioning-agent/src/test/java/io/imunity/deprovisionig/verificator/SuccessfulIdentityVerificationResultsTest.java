/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.IdentityStatus;
import io.imunity.deprovisionig.verificator.OnlineIdentityVerificationStatus.OnlineResponseStatus;

class SuccessfulIdentityVerificationResultsTest
{
	@Test
	void constructorShouldThrowWhenNullList()
	{
		assertThrows(IllegalArgumentException.class, () -> new SuccessfulIdentityVerificationResults(null));
	}

	@Test
	void constructorShouldThrowWhenEmptyList()
	{
		assertThrows(IllegalArgumentException.class, () -> new SuccessfulIdentityVerificationResults(List.of()));
	}

	@Test
	void constructorShouldThrowWhenAnyResultNotComplete()
	{
		OnlineIdentityVerificationStatus incomplete = status(OnlineResponseStatus.not_complete, IdentityStatus.active);
		List<OnlineIdentityVerificationStatus> list = List
				.of(status(OnlineResponseStatus.complete, IdentityStatus.active), incomplete);

		assertThrows(IllegalArgumentException.class, () -> new SuccessfulIdentityVerificationResults(list));
	}

	@Test
	void constructorShouldSucceedWhenAllComplete()
	{
		List<OnlineIdentityVerificationStatus> list = List.of(
				status(OnlineResponseStatus.complete, IdentityStatus.active),
				status(OnlineResponseStatus.complete, IdentityStatus.locked));

		SuccessfulIdentityVerificationResults results = new SuccessfulIdentityVerificationResults(list);

		assertNotNull(results);
	}

	@Test
	void toEntityStateShouldReturnValidForActive()
	{
		SuccessfulIdentityVerificationResults results = new SuccessfulIdentityVerificationResults(
				List.of(status(OnlineResponseStatus.complete, IdentityStatus.active)));

		assertEquals(EntityState.valid, results.toEntityState(EntityState.disabled));
	}

	@Test
	void toEntityStateShouldReturnAuthenticationDisabledForLocked()
	{
		SuccessfulIdentityVerificationResults results = new SuccessfulIdentityVerificationResults(
				List.of(status(OnlineResponseStatus.complete, IdentityStatus.locked)));

		assertEquals(EntityState.authenticationDisabled, results.toEntityState(EntityState.disabled));
	}

	@Test
	void toEntityStateShouldReturnOnlyLoginPermittedForDeleted()
	{
		SuccessfulIdentityVerificationResults results = new SuccessfulIdentityVerificationResults(
				List.of(status(OnlineResponseStatus.complete, IdentityStatus.deleted)));

		assertEquals(EntityState.onlyLoginPermitted, results.toEntityState(EntityState.disabled));
	}

	@Test
	void toEntityStateShouldReturnDisabledForDisabled()
	{
		SuccessfulIdentityVerificationResults results = new SuccessfulIdentityVerificationResults(
				List.of(status(OnlineResponseStatus.complete, IdentityStatus.disabled)));

		assertEquals(EntityState.disabled, results.toEntityState(EntityState.valid));
	}

	@Test
	void toEntityStateShouldReturnToRemoveForToRemove()
	{
		SuccessfulIdentityVerificationResults results = new SuccessfulIdentityVerificationResults(
				List.of(status(OnlineResponseStatus.complete, IdentityStatus.toRemove)));

		assertEquals(EntityState.toRemove, results.toEntityState(EntityState.valid));
	}

	@Test
	void toEntityStateShouldReturnFallbackWhenNoMatchingStatus()
	{
		SuccessfulIdentityVerificationResults results = new SuccessfulIdentityVerificationResults(
				List.of(status(OnlineResponseStatus.complete, IdentityStatus.unknown)));

		assertEquals(EntityState.disabled, results.toEntityState(EntityState.disabled));
		assertEquals(EntityState.valid, results.toEntityState(EntityState.valid));
	}

	private OnlineIdentityVerificationStatus status(OnlineResponseStatus responseStatus, IdentityStatus identityStatus)
	{
		return new OnlineIdentityVerificationStatus(responseStatus, identityStatus);
	}
}