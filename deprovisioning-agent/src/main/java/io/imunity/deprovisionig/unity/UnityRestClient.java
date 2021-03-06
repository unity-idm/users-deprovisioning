/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.unity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
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
		host = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
		restPath = uri.getPath();
		context = getClientContext(host, unityConfig.restUsername, unityConfig.restPassword);
		client = networkClient.getClient(unityConfig.restUri);
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
			try
			{
				postReq.setEntity(new StringEntity(content.get()));
			} catch (UnsupportedEncodingException e)
			{
				throw new UnityException("Can not set post request content", e);
			}
		}

		HttpResponse response = execute(postReq);
		try
		{
			return EntityUtils.toString(response.getEntity());
		} catch (ParseException | IOException e)
		{
			throw new UnityException("Can not parse unity response", e);
		}
	}

	HttpResponse post(String path, Map<String, String> params) throws UnityException

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

		HttpResponse response = execute(postReq);
		assertResponseStatusIsOk(response);
		return response;
	}

	HttpResponse put(String path, Optional<Map<String, String>> params) throws UnityException

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

		HttpResponse response = execute(putReq);
		assertResponseStatusIsOk(response);
		return response;
	}

	HttpResponse put(String path, ContentType contentType, Optional<String> content) throws UnityException
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
			try
			{
				putReq.setEntity(new StringEntity(content.get()));
			} catch (UnsupportedEncodingException e)
			{
				throw new UnityException("Can not set post request content", e);
			}
		}

		HttpResponse response = execute(putReq);
		assertResponseStatusIsOk(response);
		return response;
	}

	private void assertResponseStatusIsOk(HttpResponse response) throws UnityException
	{
		if (!(HttpStatus.SC_OK == response.getStatusLine().getStatusCode()
				|| HttpStatus.SC_NO_CONTENT == response.getStatusLine().getStatusCode()))
		{
			throw new UnityException(response.getStatusLine().toString());
		}
	}

	private HttpResponse execute(HttpRequest request) throws UnityException
	{
		try
		{
			return client.execute(host, request, context);
		} catch (IOException e)
		{
			throw new UnityException("Can not connect to unity", e);
		}
	}

	private HttpClientContext getClientContext(HttpHost host, String user, String pass)
	{
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(host.getHostName(), host.getPort()),
				new UsernamePasswordCredentials(user, pass));

		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);
		HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);
		return context;
	}
}
