/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.stereotype.Component;

import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.NetworkClient;
import io.imunity.deprovisionig.common.exception.ConfigurationException;

@Component
class UnityRestClient
{
	private final HttpHost host;
	private final String restPath;
	private final HttpClientContext context;
	private final HttpClient client;

	UnityRestClient(NetworkClient networkClient, DeprovisioningConfiguration unityConfig)
	{
		URI uri;
		try
		{
			uri = new URI(unityConfig.restUri);
		} catch (URISyntaxException e)
		{
			throw new ConfigurationException("Invalid rest uri");
		}
		host = HttpHost.create(uri);
		restPath = uri.getPath();
		context = getClientContext(host, unityConfig.restUsername, unityConfig.restPassword);
		client = networkClient.getClient(unityConfig.restUri, unityConfig.getHttpClientProperties());
	}

	String post(String path, ContentType contentType, Optional<String> content) throws UnityException

	{
		HttpPost postReq;
		try
		{
			URIBuilder builder = new URIBuilder(restPath + path);
			postReq = new HttpPost(builder.build());
		} catch (URISyntaxException e)
		{
			throw new UnityException("Invalid unity url", e);
		}

		if (contentType != null)
		{
			postReq.setHeader("Content-type", contentType.toString());
		}
		if (content.isPresent())
		{
			postReq.setEntity(new StringEntity(content.get()));
		}

		
		try (CloseableHttpResponse response = execute(postReq))
		{
			return EntityUtils.toString(response.getEntity());
		} catch (ParseException | IOException e)
		{
			throw new UnityException("Can not parse unity response", e);
		}
	}

	void post(String path, Map<String, String> params) throws UnityException

	{
		URIBuilder builder;
		try
		{
			builder = new URIBuilder(restPath + path);
		} catch (URISyntaxException e)
		{
			throw new UnityException("Invalid unity url", e);
		}
		params.forEach((k, v) -> builder.addParameter(k, v));
		HttpPost postReq;
		try
		{
			postReq = new HttpPost(builder.build());
		} catch (URISyntaxException e)
		{
			throw new UnityException("Invalid unity url params", e);
		}

		executeAndAssertStatusIsOk(postReq);
	}

	void put(String path, Optional<Map<String, String>> params) throws UnityException

	{
		URIBuilder builder;
		try
		{
			builder = new URIBuilder(restPath + path);
		} catch (URISyntaxException e)
		{
			throw new UnityException("Invalid unity url", e);
		}
		if (params.isPresent())
		{
			params.get().forEach((k, v) -> builder.addParameter(k, v));
		}
		HttpPut putReq;
		try
		{
			putReq = new HttpPut(builder.build());
		} catch (URISyntaxException e)
		{
			throw new UnityException("Invalid unity url params", e);
		}

		executeAndAssertStatusIsOk(putReq);
	}
	
	void delete(String path) throws UnityException
	{
		HttpDelete delReq;
		try
		{
			delReq = new HttpDelete(new URIBuilder(restPath + path).build());
		} catch (URISyntaxException e)
		{
			throw new UnityException("Invalid unity url", e);
		}

		executeAndAssertStatusIsOk(delReq);
	}

	void put(String path, ContentType contentType, Optional<String> content) throws UnityException
	{
		HttpPut putReq;
		try
		{
			putReq = new HttpPut(new URIBuilder(restPath + path).build());
		} catch (URISyntaxException e)
		{
			throw new UnityException("Invalid unity url", e);
		}

		if (contentType != null)
		{
			putReq.setHeader("Content-type", contentType.toString());
		}
		if (content.isPresent())
		{
			putReq.setEntity(new StringEntity(content.get()));
		}
		
		executeAndAssertStatusIsOk(putReq);
	}

	
	private void executeAndAssertStatusIsOk(ClassicHttpRequest request) throws UnityException
	{
		try (CloseableHttpResponse response = execute(request))
		{
			assertResponseStatusIsOk(response);
		} catch (IOException e)
		{
			throw new UnityException("Can not connect to unity", e);
		}
	}
	
	private void assertResponseStatusIsOk(HttpResponse response) throws UnityException
	{
		if (!(HttpStatus.SC_OK == response.getCode()
				|| HttpStatus.SC_NO_CONTENT == response.getCode()))
		{
			throw new UnityException(response.getCode() + " " + response.getReasonPhrase());
		}
	}

	private CloseableHttpResponse execute(ClassicHttpRequest request) throws UnityException
	{
		try
		{
			return (CloseableHttpResponse) client.executeOpen(host, request, context);
		} catch (IOException e)
		{
			throw new UnityException("Can not connect to unity", e);
		}
	}

	private HttpClientContext getClientContext(HttpHost host, String user, String pass)
	{
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(host),
				new UsernamePasswordCredentials(user, pass.toCharArray()));

		BasicScheme basicAuth = new BasicScheme();
	    basicAuth.initPreemptive(new UsernamePasswordCredentials(user, pass.toCharArray()));

	    AuthCache authCache = new BasicAuthCache();
	    authCache.put(host, basicAuth);
		HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);
		return context;
	}
}
