/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.hook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.imunity.deprovisionig.DeprovisioningConfiguration;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
public class GroovyHookExecutor
{
	private static final Logger log = LogManager.getLogger(GroovyHookExecutor.class);

	private DeprovisioningConfiguration config;
	

	public GroovyHookExecutor(DeprovisioningConfiguration config)
	{
		this.config = config;
	}

	public void runHook(UnityUser user, EntityState newStatus, SAMLIdpInfo idpInfo, Instant removeTime)
	{	
		try(Reader scriptReader = getFileReader();)
		{
			runScript(scriptReader, getBinding(user, newStatus, idpInfo, removeTime));
		} catch (Exception e)
		{
			log.error("Can not execute groovy script", e);
		} 
	}

	private Reader getFileReader()
	{
		try
		{
			InputStream is = new FileInputStream(config.hookScript);
			return new InputStreamReader(is);
		} catch (IOException e)
		{
			throw new ConfigurationException("Error loading script " + config.hookScript, e);
		}
	}

	private void runScript(Reader scriptReader, Binding binding)
	{
		GroovyShell shell = new GroovyShell(binding);
		log.info("Triggers invocation of Groovy script: {}", config.hookScript);
		shell.evaluate(scriptReader);
		log.info("Groovy script: {} finished", config.hookScript);
	}

	private Binding getBinding(UnityUser user, EntityState newStatus, SAMLIdpInfo idpInfo, Instant removeTime)
	{
		Binding binding = new Binding();

		binding.setVariable("log", log);
		binding.setVariable("user", user);
		binding.setVariable("newUserStatus", newStatus);
		binding.setVariable("idpInfo", idpInfo);
		binding.setVariable("removeTime", removeTime);
		binding.setVariable("configuration", config);
		return binding;
	}
}
