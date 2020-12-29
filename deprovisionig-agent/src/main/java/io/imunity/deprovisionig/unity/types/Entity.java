/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity.types;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Entity
{
	private EntityInformation entityInformation;
	private List<Identity> identities;

	@JsonCreator
	public Entity()
	{

	}

	public EntityInformation getEntityInformation()
	{
		return entityInformation;
	}

	public void setEntityInformation(EntityInformation entityInformation)
	{
		this.entityInformation = entityInformation;
	}

	public List<Identity> getIdentities()
	{
		return identities;
	}

	public void setIdentities(List<Identity> identities)
	{
		this.identities = identities;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof Entity))
			return false;
		Entity castOther = (Entity) other;
		return Objects.equals(entityInformation, castOther.entityInformation)
				&& Objects.equals(identities, castOther.identities);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityInformation, identities);
	}
}
