/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.deprovisionig.common.saml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SAMLRequesterConfiguration
{	
	public final String requesterEntityId;
	public final boolean signRequest;
	public final boolean sslAuthn;
	public final boolean messageLogging;
	
	public SAMLRequesterConfiguration(@Value("${saml.requester.requesterEntityId}") String requesterEntityId,
			@Value("${saml.requester.signRequest:true}") Boolean signRequest,
			@Value("${saml.requester.sslAuthn:false}") Boolean sslAuthn,
			@Value("${saml.requester.messageLogging:false}") Boolean messageLogging)
	{
		this.signRequest = signRequest;
		this.requesterEntityId = requesterEntityId;
		this.sslAuthn = sslAuthn;
		this.messageLogging = messageLogging;
	}
}
