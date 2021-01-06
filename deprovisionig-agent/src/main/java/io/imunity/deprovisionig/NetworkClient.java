/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import io.imunity.deprovisionig.common.Truststore;

@Component
public class NetworkClient
{
	private final Truststore truststore;

	@Autowired
	public NetworkClient(Truststore truststore)
	{
		this.truststore = truststore;
	}
	
	public HttpClient getClient(String url) throws Exception
	{
		return url.startsWith("https:") ? getSSLClient(url) : HttpClientBuilder.create().build();
	}

	private HttpClient getSSLClient(String url) throws Exception
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setValidator(truststore.getValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		return HttpUtils.createClient(url, clientCfg);
	}
}
