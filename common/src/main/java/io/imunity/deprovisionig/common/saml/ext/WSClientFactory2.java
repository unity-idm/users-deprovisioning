/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common.saml.ext;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.ProxyServerType;

import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.util.httpclient.HttpClientProperties;
import eu.unicore.util.httpclient.IClientConfiguration;

public class WSClientFactory2 extends WSClientFactory
{

	public WSClientFactory2(IClientConfiguration securityCfg)
	{
		super(securityCfg);

	}

	public void setupHTTPParams(HTTPConduit http)
	{

		// HTTP auth
		if (securityProperties.doHttpAuthn())
		{
			AuthorizationPolicy httpAuth = new AuthorizationPolicy();
			httpAuth.setUserName(securityProperties.getHttpUser());
			httpAuth.setPassword(securityProperties.getHttpPassword());
			http.setAuthorization(httpAuth);
		}

		// TLS
		TLSClientParameters params = new TLSClientParameters();
		params.setSSLSocketFactory(new MySSLSocketFactory2(securityProperties));
		params.setDisableCNCheck(true);
		http.setTlsClientParameters(params);

		// timeouts
		http.getClient().setConnectionTimeout(settings.getIntValue(HttpClientProperties.CONNECT_TIMEOUT));
		http.getClient().setReceiveTimeout(settings.getIntValue(HttpClientProperties.SO_TIMEOUT));

		// TODO gzip? CXF has a GZIP Feature
		// boolean gzipEnabled =
		// Boolean.parseBoolean(properties.getProperty(GZIP_ENABLE,
		// "true"));

		if (settings.getBooleanValue(HttpClientProperties.CONNECTION_CLOSE))
		{
			http.getClient().setConnection(ConnectionType.CLOSE);
		}

		boolean allowChunking = settings.getBooleanValue(HttpClientProperties.ALLOW_CHUNKING);
		http.getClient().setAllowChunking(allowChunking);

		// http proxy
		String uri = http.getAddress();
		configureHttpProxy(http, uri);

	}
	
	private void configureHttpProxy(HTTPConduit http, String uri){
		if (isNonProxyHost(uri)) 
			return;

		// Setup the proxy settings
		String proxyHost = settings.getValue(HttpClientProperties.HTTP_PROXY_HOST);
		if (proxyHost == null)
		{
			proxyHost = System.getProperty("http."+HttpClientProperties.HTTP_PROXY_HOST);
		}

		if (proxyHost != null && proxyHost.trim().length()>0)
		{ 
			String portS = settings.getValue(HttpClientProperties.HTTP_PROXY_PORT);
			if (portS == null)
			{
				portS = System.getProperty("http."+HttpClientProperties.HTTP_PROXY_PORT);
			}
			int port = 80;
			if (portS != null)
				port = Integer.parseInt(portS);

			http.getClient().setProxyServer(proxyHost);
			http.getClient().setProxyServerPort(port);
			
			String proxyType=settings.getValue(HttpClientProperties.HTTP_PROXY_TYPE);
			http.getClient().setProxyServerType(ProxyServerType.fromValue(proxyType));
			
			String user=settings.getValue(HttpClientProperties.HTTP_PROXY_USER);
			if(user!=null){
				ProxyAuthorizationPolicy ap=new ProxyAuthorizationPolicy();
				ap.setUserName(user);
				String password=settings.getValue(HttpClientProperties.HTTP_PROXY_PASS);
				if(password!=null){
					ap.setPassword(password);
				}
				http.setProxyAuthorization(ap);
			}
		}

	}
	
	private boolean isNonProxyHost(String uri){
		String nonProxyHosts=settings.getValue(HttpClientProperties.HTTP_NON_PROXY_HOSTS);
		if(nonProxyHosts==null)return false;
		try{
			URI u=new URI(uri);
			String host=u.getHost();
			String[] npHosts=nonProxyHosts.split(" ");
			for(String npHost: npHosts){
				if(host.contains(npHost))return true;
			}
		}catch(URISyntaxException e){
			logger.error("Can't resolve URI from "+uri, e);
		}	

		return false;
	}


}
