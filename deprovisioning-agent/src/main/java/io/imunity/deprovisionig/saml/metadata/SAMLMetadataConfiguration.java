/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml.metadata;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SAMLMetadataConfiguration
{
	public final Duration metadataValidityTime;
	public final String metadataSource;

	@Autowired
	public SAMLMetadataConfiguration(@Value("${saml.metadataValidityTime:1D}") Duration metadataValidityTime,
			@Value("${saml.metadataSource:}") String metadataSource)
	{
		this.metadataSource = metadataSource;
		this.metadataValidityTime = metadataValidityTime;
	}
}
