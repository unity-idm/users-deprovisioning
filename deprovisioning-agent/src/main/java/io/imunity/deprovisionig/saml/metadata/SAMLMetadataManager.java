/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml.metadata;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.SAMLConstants;
import io.imunity.deprovisionig.NetworkClient;
import io.imunity.deprovisionig.common.WorkdirFileManager;
import xmlbeans.org.oasis.saml2.metadata.AttributeAuthorityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.ContactType;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;

@Component
public class SAMLMetadataManager
{
	public static final Set<String> SUPPORTED_URL_SCHEMES = new HashSet<>(Arrays.asList("data", "http", "https"));
	private static final Logger log = LogManager.getLogger(SAMLMetadataManager.class);
	private static final int META_CONTENT_SIZE_LIMIT = 10240;
	
	private SAMLMetadataConfiguration config;

	private final NetworkClient networkClient;
	private final WorkdirFileManager fileMan;

	@Autowired
	public SAMLMetadataManager(NetworkClient networkClient, WorkdirFileManager fileMan,
			SAMLMetadataConfiguration config)
	{
		this.networkClient = networkClient;
		this.fileMan = fileMan;
		this.config = config;
	}

	public Map<String, SAMLIdpInfo> getAttributeQueryAddressesAsMap() throws Exception
	{
		Map<String, SAMLIdpInfo> attrQueryAddr = new HashMap<>();
		EntitiesDescriptorDocument metaDoc = getMetadaFromUri(config.metadataSource);
		EntitiesDescriptorType meta = metaDoc.getEntitiesDescriptor();
		searchAttributeQueryAddr(meta, attrQueryAddr);
		return attrQueryAddr;
	}

	private void searchAttributeQueryAddr(EntitiesDescriptorType meta, Map<String, SAMLIdpInfo> result)
	{
		EntitiesDescriptorType[] nested = meta.getEntitiesDescriptorArray();
		if (nested != null)
		{
			for (EntitiesDescriptorType nestedD : nested)
				searchAttributeQueryAddr(nestedD, result);
		}
		EntityDescriptorType[] entities = meta.getEntityDescriptorArray();

		if (entities != null)
		{
			for (EntityDescriptorType entity : entities)
			{
				searchAttributeQueryAddr(entity, result);
			}
		}
	}

	private void searchAttributeQueryAddr(EntityDescriptorType meta, Map<String, SAMLIdpInfo> result)
	{

		AttributeAuthorityDescriptorType[] idpDefs = meta.getAttributeAuthorityDescriptorArray();
		for (AttributeAuthorityDescriptorType idpDef : idpDefs)
		{

			Optional<EndpointType> saml2AttrService = getSaml2AttributeService(idpDef);

			if (saml2AttrService.isEmpty())
			{
				log.info("IDP of entity " + meta.getEntityID() + " doesn't support SAML2 - ignoring");
				continue;
			}

			result.put(meta.getEntityID(), new SAMLIdpInfo(meta.getEntityID(),
					getSOAPEndpointLocation(saml2AttrService.get()), getTechnicalPersonEmail(meta)));

		}
	}

	private String getSOAPEndpointLocation(EndpointType endpointType)
	{
		final String UNITY_OLD_ENDPOINT_SUFFIX = "/saml2idp-soap";
		final String UNITY_MISSING_ENDPOINT_PATH = "/AssertionQueryService";
		String location = endpointType.getLocation();
		return location.endsWith(UNITY_OLD_ENDPOINT_SUFFIX) ? location + UNITY_MISSING_ENDPOINT_PATH : location;
	}
	
	private String getTechnicalPersonEmail(EntityDescriptorType idpDef)
	{
		Optional<ContactType> contactTechnical = Stream.of(idpDef.getContactPersonArray())
				.filter(a -> a.getContactType()
						.equals(xmlbeans.org.oasis.saml2.metadata.ContactTypeType.TECHNICAL))
				.findAny();

		if (contactTechnical.isEmpty() || contactTechnical.get().getEmailAddressArray() == null
				|| contactTechnical.get().getEmailAddressArray().length == 0)
		{
			return null;
		}

		String emailValue = contactTechnical.get().getEmailAddressArray()[0];
		if (emailValue.startsWith("mailto:"))
		{
			return emailValue.substring(7);

		} else
		{
			return emailValue;
		}
	}

	@SuppressWarnings("unchecked")
	public Optional<EndpointType> getSaml2AttributeService(AttributeAuthorityDescriptorType idpDef)
	{
		if (idpDef.getProtocolSupportEnumeration().stream().filter(s -> SAMLConstants.PROTOCOL_NS.equals(s))
				.findAny().isEmpty())
		{
			return Optional.empty();
		}

		return Stream.of(idpDef.getAttributeServiceArray())
				.filter(a -> a.getBinding().equals(SAMLConstants.BINDING_SOAP)).findAny();

	}

	public EntitiesDescriptorDocument getMetadaFromUri(String rawUri) throws Exception
	{
		log.debug("Getting saml metada from uri " + rawUri);
		
		URI uri = parseURI(rawUri);

		if (!isWebReady(uri))
		{
			return parseMetadataFile(readFile(uri));
		}

		String filePath = DigestUtils.md5Hex(uri.toString());
		if (fileMan.exists(filePath) && fileMan.getLastModifiedTime(filePath)
				.isAfter(Instant.now().minus(config.metadataValidityTime)))
		{
			return parseMetadataFile(fileMan.readFile(filePath));

		} else
		{
			ByteArrayInputStream metadataFileContent = readURI(uri);
			cacheMetadata(uri, metadataFileContent, filePath);
			return parseMetadataFile(metadataFileContent);
		}

	}

	private void cacheMetadata(URI uri, ByteArrayInputStream metadataFileContent, String filePath)
			throws IOException
	{
		log.info("Cache metadata file from: " + uri);
		fileMan.saveFile(metadataFileContent.readAllBytes(), filePath);
		metadataFileContent.reset();
	}

	private ByteArrayInputStream readFile(URI uri) throws IOException
	{
		log.info("Read metadata file from external file: " + uri);
		Path toRead = Paths.get(getPathFromURI(uri));
		return new ByteArrayInputStream(Files.readAllBytes(toRead));
	}

	private EntitiesDescriptorDocument parseMetadataFile(InputStream file)
			throws XmlException, IOException, InterruptedException
	{
		InputStream is = new BufferedInputStream(file);
		String metadata = IOUtils.toString(is, Charset.defaultCharset());
		EntitiesDescriptorDocument doc;
		try
		{
			doc = EntitiesDescriptorDocument.Factory.parse(metadata);
		} catch (XmlException e)
		{
			log.debug("Can not parse metadata as entities descriptor document, trying to parse as a single entity descriptor");
			try
			{
				EntityDescriptorType descType = EntityDescriptorType.Factory.parse(metadata);
				doc = EntitiesDescriptorDocument.Factory.newInstance();
				EntitiesDescriptorType newEntitiesDescriptor = doc.addNewEntitiesDescriptor();
				newEntitiesDescriptor.set(descType.copy());

			} catch (XmlException e2)
			{
				log.error("Can not parse metadata", e);
				throw e;
			}

		}
		is.close();
		return doc;
	}

	private ByteArrayInputStream readURI(URI uri) throws IOException
	{
		if (uri.getScheme().equals("http") || uri.getScheme().equals("https"))
		{
			try
			{
				return download(uri.toURL());
			} catch (Exception e)
			{
				throw new IllegalArgumentException("Can not read URL, uri: " + uri.toString(), e);
			}

		} else if (uri.getScheme().equals("data"))
		{

			try
			{
				return readDataScheme(getPathFromURI(uri));
			} catch (Exception e)
			{
				throw new IllegalArgumentException("Can not read data uri: " + uri.toString(), e);
			}
		}

		throw new IllegalArgumentException("Not supported uri schema");

	}

	private ByteArrayInputStream readDataScheme(String data) throws IllegalArgumentException
	{
		if (data == null)
			throw new IllegalArgumentException("Data element of uri can not be empty");

		String pureBase64 = data.contains(",") ? data.substring(data.indexOf(",") + 1) : data;
		return new ByteArrayInputStream(Base64.getDecoder().decode(pureBase64));

	}

	private String getPathFromURI(URI uri)
	{
		return uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath();
	}

	private ByteArrayInputStream download(URL url) throws Exception
	{
		log.info("Download file from: " + url);
		HttpGet request = new HttpGet(url.toString());
		HttpResponse response = networkClient.getClient(url.toString()).execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		{
			String body = response.getEntity().getContentLength() < META_CONTENT_SIZE_LIMIT
					? EntityUtils.toString(response.getEntity())
					: "";

			throw new IOException("File download from " + url + " error: "
					+ response.getStatusLine().toString() + "; " + body);
		}

		return new ByteArrayInputStream(IOUtils.toByteArray(response.getEntity().getContent()));
	}

	private URI parseURI(String rawURI) throws IllegalArgumentException
	{
		URI uri;
		try
		{
			uri = new URI(rawURI);
		} catch (URISyntaxException e)
		{
			throw new IllegalArgumentException("Not supported uri schema");
		}

		return uri;
	}

	private boolean isWebReady(URI uri)
	{
		if (uri != null && uri.getScheme() != null)
		{
			String scheme = uri.getScheme();
			if (SUPPORTED_URL_SCHEMES.contains(scheme))
			{
				return true;
			}
		}

		return false;
	}
}
