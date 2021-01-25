/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common.saml.ext;

import java.net.MalformedURLException;

import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.security.wsutil.samlclient.AbstractSAMLClient;
import eu.unicore.util.httpclient.IClientConfiguration;

public class AbstractSAMLClient2 extends AbstractSAMLClient
{

	protected AbstractSAMLClient2(String address, IClientConfiguration secCfg, NameID issuer,
			SamlTrustChecker trustChecker) throws MalformedURLException
	{
		super(address, secCfg, issuer, trustChecker);
		factory = new WSClientFactory2(secCfg);

	}

	protected AbstractSAMLClient2(String address, IClientConfiguration secProv, SamlTrustChecker trustChecker)
			throws MalformedURLException
	{
		this(address, secProv, (NameID) null, trustChecker);
	}
}
