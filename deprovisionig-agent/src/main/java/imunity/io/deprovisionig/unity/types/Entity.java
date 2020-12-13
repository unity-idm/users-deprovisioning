/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package imunity.io.deprovisionig.unity.types;

import java.util.List;

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
}
