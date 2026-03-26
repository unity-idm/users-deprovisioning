/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.verificator;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.unity.types.Identity;

class IdentitiesFromSingleIdp
{
	final List<Identity> identities;
	final SAMLIdpInfo idpInfo;

	IdentitiesFromSingleIdp(List<Identity> onlineVerifiableIdentities, Map<String, SAMLIdpInfo> idpsAsMap)
	{
		if (onlineVerifiableIdentities.isEmpty())
		{
			this.identities = List.of();
			this.idpInfo = null;
			return;
		}

		String firstIdp = onlineVerifiableIdentities.get(0)
				.getRemoteIdp();
		boolean allSameIdp = onlineVerifiableIdentities.stream()
				.allMatch(identity -> firstIdp.equals(identity.getRemoteIdp()));

		if (!allSameIdp)
		{
			throw new IllegalArgumentException("All identities must belong to the same remoteIdp");
		}
		this.identities = List.copyOf(onlineVerifiableIdentities);
		this.idpInfo = idpsAsMap.get(firstIdp);
	}

	String remoteIdp()
	{
		return identities.get(0)
				.getRemoteIdp();
	}

	String translationProfile()
	{
		return identities.get(0)
				.getTranslationProfile();
	}

	boolean isEmpty()
	{
		return identities.isEmpty();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(identities, idpInfo);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdentitiesFromSingleIdp other = (IdentitiesFromSingleIdp) obj;
		return Objects.equals(identities, other.identities) && Objects.equals(idpInfo, other.idpInfo);
	}

}