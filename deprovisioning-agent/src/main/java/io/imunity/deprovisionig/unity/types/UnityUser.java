/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity.types;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.imunity.deprovisionig.Constans;

public class UnityUser
{
	public final Long entityId;
	public final EntityState entityState;
	public final Set<String> groups;
	public final List<Identity> identities;
	public final String displayedName;
	public final LocalDateTime lastAuthenticationTime;
	public final LocalDateTime firstHomeIdpVerificationFailure;
	public final LocalDateTime lastSuccessHomeIdPVerification;
	public final LocalDateTime firstOfflineVerificationAttempt;
	public final LocalDateTime lastOfflineVerificationAttempt;
	
	public UnityUser(Long entityId, String displayedName, EntityState entityState, List<Identity> identities,
			Set<String> groups, LocalDateTime lastAuthenticationTime,
			LocalDateTime firstHomeIdpVerificationFailure, LocalDateTime lastSuccessHomeIdPVerification,
			LocalDateTime firstOfflineVerificationAttempt, LocalDateTime lastOfflineVerificationAttempt)
	{
		this.entityId = entityId;
		this.displayedName = displayedName;
		this.entityState = entityState;
		this.identities = identities;
		this.groups = groups;
		this.lastAuthenticationTime = lastAuthenticationTime;
		this.lastSuccessHomeIdPVerification = lastSuccessHomeIdPVerification;
		this.lastOfflineVerificationAttempt = lastOfflineVerificationAttempt;
		this.firstHomeIdpVerificationFailure = firstHomeIdpVerificationFailure;
		this.firstOfflineVerificationAttempt = firstOfflineVerificationAttempt;
	}

	public Optional<Identity> getIdentifierIdentityByProfile(String profile)
	{
		return identities.stream().filter(i -> i.getTypeId().equals(Constans.IDENTIFIER_IDENTITY)
				&& profile.equals(i.getTranslationProfile())).findAny();
	}

	public LocalDateTime getLastSuccessfullOnlineVerificationTime()
	{
		LocalDateTime maxOld = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
		LocalDateTime notEmptylastAuth = lastAuthenticationTime == null ? maxOld : lastAuthenticationTime;
		LocalDateTime notEmptylastHomeIdpVerification = lastSuccessHomeIdPVerification == null ? maxOld
				: lastSuccessHomeIdPVerification;
		return notEmptylastAuth.isAfter(notEmptylastHomeIdpVerification) ? notEmptylastAuth
				: notEmptylastHomeIdpVerification;
	}
	
	public String toFullString()
	{
		return "User " + entityId + " status: " + entityState + " groups:" + groups + " identities:"
				+ identities.stream().map(id -> id.getTypeId() + ":" + id.getValue())
						.collect(Collectors.toList())
				+ " lastAuthenticationTime:" + lastAuthenticationTime + " lastHomeIdPVerification:"
				+ lastSuccessHomeIdPVerification + " lastOfflineVerification:"
				+ lastOfflineVerificationAttempt;
	}
	
	@Override
	public String toString()
	{
		return "[" + entityId + "] " + (displayedName != null ? "[" + displayedName + "]" : "");	
	}
	
	public String toLogString()
	{
		return "User [" + entityId + "] " + (displayedName != null ? "[" + displayedName + "]" : "")
				+ " status: " + entityState + " identities:"
				+ identities.stream().map(id -> id.getTypeId() + ":" + id.getValue())
						.collect(Collectors.toList());
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof UnityUser))
			return false;
		UnityUser castOther = (UnityUser) other;
		return Objects.equals(entityId, castOther.entityId)
				&& Objects.equals(displayedName, castOther.displayedName)
				&& Objects.equals(entityState, castOther.entityState)
				&& Objects.equals(identities, castOther.identities)
				&& Objects.equals(groups, castOther.groups)
				&& Objects.equals(lastAuthenticationTime, castOther.lastAuthenticationTime)
				&& Objects.equals(lastSuccessHomeIdPVerification,
						castOther.lastSuccessHomeIdPVerification)
				&& Objects.equals(firstOfflineVerificationAttempt,
						castOther.firstOfflineVerificationAttempt)
				&& Objects.equals(lastOfflineVerificationAttempt,
						castOther.lastOfflineVerificationAttempt);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityId, displayedName, entityState, identities, groups, lastAuthenticationTime,
				lastSuccessHomeIdPVerification, firstOfflineVerificationAttempt,
				lastOfflineVerificationAttempt);
	}
}
