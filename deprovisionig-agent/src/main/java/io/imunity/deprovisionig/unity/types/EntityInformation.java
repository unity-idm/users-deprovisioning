/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.unity.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityInformation
{
	private Long entityId;
	private EntityState state;

	@JsonCreator
	public EntityInformation()
	{
	}

	public Long getEntityId()
	{
		return entityId;
	}

	public void setEntityId(Long entityId)
	{
		this.entityId = entityId;
	}

	public EntityState getState()
	{
		return state;
	}

	public void setState(EntityState entityState)
	{
		this.state = entityState;
	}

	
}