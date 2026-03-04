/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig;

import java.util.Properties;

import org.apache.hc.client5.http.classic.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpClientProperties;
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

	public HttpClient getClient(String url)
	{
		return getClient(url, new HttpClientProperties(new Properties()));
	}
	
	
	public HttpClient getClient(String url, HttpClientProperties properties)
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		if (url.startsWith("https:"))
		{
			clientCfg.setValidator(truststore.getValidator());
			clientCfg.setSslEnabled(true);
		} else
		{
			clientCfg.setSslEnabled(false);
		}

		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpClientProperties(properties);
		return HttpUtils.createClient(url, clientCfg);
	}
}
