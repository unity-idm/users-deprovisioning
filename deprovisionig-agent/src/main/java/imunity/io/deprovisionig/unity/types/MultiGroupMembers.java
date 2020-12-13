/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package imunity.io.deprovisionig.unity.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
	}
}
