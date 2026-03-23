/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml.metadata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.unicore.samly2.SAMLConstants;
import io.imunity.deprovisionig.unity.types.I18nString;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.ExtensionsType;
import xmlbeans.org.oasis.saml2.metadata.IDPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.LocalizedNameType;
import xmlbeans.org.oasis.saml2.metadata.OrganizationType;
import xmlbeans.org.oasis.saml2.metadata.SSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoDocument;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;

public class SAMLIdPNameFromMetaExtractor
{
	private static final Logger log = LogManager.getLogger(SAMLIdPNameFromMetaExtractor.class);
	private static final String defaultLocaleCode = "en";

	public static I18nString getLocalizedNamesAsI18nString(String entityId, EntityDescriptorType mainDescriptor)
	{
		I18nString ret = new I18nString();
		List<IDPSSODescriptorType> idpssoDescriptorList = mainDescriptor.getIDPSSODescriptorList();
		IDPSSODescriptorType idpDef = idpssoDescriptorList.stream()
				.filter(d -> supportsSaml2(d))
				.findFirst()
				.orElse(null);
		UIInfoType uiInfo = null;
		if (idpDef != null)
			uiInfo = parseMDUIInfo(idpDef.getExtensions(), entityId);
		Map<String, String> localizedNames = getLocalizedNames(uiInfo, idpDef, mainDescriptor);
		ret.addAllValues(localizedNames);
		if (localizedNames.containsKey(""))
			ret.setDefaultValue(localizedNames.get(""));
		else if (!localizedNames.isEmpty())
			ret.setDefaultValue(localizedNames.values()
					.iterator()
					.next());
		else
		{
			ret.setDefaultValue("Unnamed Identity Provider");
			log.warn("IdP {} has no name set", entityId);
		}
		return ret;
	}

	private static UIInfoType parseMDUIInfo(ExtensionsType extensions, String entityId)
	{
		if (extensions == null)
			return null;
		NodeList nl = extensions.getDomNode()
				.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++)
		{
			Node elementN = nl.item(i);
			if (elementN.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element element = (Element) elementN;
			if ("UIInfo".equals(element.getLocalName())
					&& "urn:oasis:names:tc:SAML:metadata:ui".equals(element.getNamespaceURI()))
			{
				try
				{
					return UIInfoDocument.Factory.parse(element)
							.getUIInfo();
				} catch (XmlException e)
				{
					log.warn("Can not parse UIInfo metadata extension for " + entityId, e);
				}
			}
		}
		return null;
	}

	private static boolean supportsSaml2(SSODescriptorType idpDef)
	{
		List<?> supportedProtocols = idpDef.getProtocolSupportEnumeration();
		for (Object supported : supportedProtocols)
			if (SAMLConstants.PROTOCOL_NS.equals(supported))
				return true;
		return false;
	}

	private static Map<String, String> getLocalizedNames(UIInfoType uiInfo, SSODescriptorType idpDesc,
			EntityDescriptorType mainDescriptor)
	{
		Map<String, String> ret = new LinkedHashMap<>();
		OrganizationType mainOrg = mainDescriptor.getOrganization();
		if (mainOrg != null)
		{
			addLocalizedNames(mainOrg.getOrganizationNameArray(), ret);
			addLocalizedNames(mainOrg.getOrganizationDisplayNameArray(), ret);
		}
		if (idpDesc != null)
		{
			OrganizationType org = idpDesc.getOrganization();
			if (org != null)
			{
				addLocalizedNames(org.getOrganizationNameArray(), ret);
				addLocalizedNames(org.getOrganizationDisplayNameArray(), ret);
			}
		}
		if (uiInfo != null)
		{
			addLocalizedNames(uiInfo.getDisplayNameArray(), ret);
		}
		return ret;
	}

	private static void addLocalizedNames(LocalizedNameType[] names, Map<String, String> ret)
	{
		if (names == null)
			return;
		String enName = null;
		for (LocalizedNameType name : names)
		{
			String lang = name.getLang();
			if (lang != null)
			{
				ret.put(lang, name.getStringValue());
				if (lang.equals(defaultLocaleCode))
					ret.put("", name.getStringValue());
				if (lang.equals("en"))
					enName = name.getStringValue();
			} else
			{
				ret.put("", name.getStringValue());
			}
		}
		if (enName != null && !ret.containsKey(""))
			ret.put("", enName);
	}
}
