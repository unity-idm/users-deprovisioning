/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MultiGroupMembers
{
	public final Collection<Entity> entities;
	public final Map<String, List<EntityGroupAttributes>> members;

	@JsonCreator
	public MultiGroupMembers(@JsonProperty("entities") Collection<Entity> entities,
			@JsonProperty("members") Map<String, List<EntityGroupAttributes>> members)
	{
		this.entities = Collections.unmodifiableCollection(entities);
		this.members = Collections.unmodifiableMap(members);
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof MultiGroupMembers))
			return false;
		MultiGroupMembers castOther = (MultiGroupMembers) other;
		return Objects.equals(entities, castOther.entities) && Objects.equals(members, castOther.members);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entities, members);
	}

	public static class EntityGroupAttributes
	{
		public final long entityId;
		public final Collection<Attribute> attributes;

		@JsonCreator
		public EntityGroupAttributes(@JsonProperty("entityId") long entityId,
				@JsonProperty("attributes") Collection<Attribute> attribtues)
		{
			this.entityId = entityId;
			this.attributes = Collections.unmodifiableCollection(attribtues);
		}

		@Override
		public String toString()
		{
			return "EntityGroupAttributes [entityId=" + entityId + ", attribtues=" + attributes + "]";
		}

		@Override
		public boolean equals(final Object other)
		{
			if (!(other instanceof EntityGroupAttributes))
				return false;
			EntityGroupAttributes castOther = (EntityGroupAttributes) other;
			return Objects.equals(entityId, castOther.attributes)
					&& Objects.equals(entityId, castOther.attributes);

		}

		@Override
		public int hashCode()
		{
			return Objects.hash(entityId, attributes);
		}

	}
}
