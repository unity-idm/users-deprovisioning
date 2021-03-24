/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity.types;

public enum EntityState
{
	valid,
	authenticationDisabled,
	disabled,
	onlyLoginPermitted,
	toRemove
}
