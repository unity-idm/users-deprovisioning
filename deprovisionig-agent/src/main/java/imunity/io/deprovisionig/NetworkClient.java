/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package imunity.io.deprovisionig;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;

@Component
public class NetworkClient
{
	private CredentialConfiguration credentials;

	@Autowired
	public NetworkClient(CredentialConfiguration credentials)
	{
		this.credentials = credentials;
	}
	
	public HttpClient getClient(String url) throws Exception
	{
		return url.startsWith("https:") ? getSSLClient(url) : HttpClientBuilder.create().build();
	}

	private HttpClient getSSLClient(String url) throws Exception
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setValidator(credentials.getValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		return HttpUtils.createClient(url, clientCfg);
	}

}
