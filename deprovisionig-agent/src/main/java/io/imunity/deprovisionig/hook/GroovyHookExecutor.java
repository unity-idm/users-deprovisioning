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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.imunity.deprovisionig.TimesConfiguration;
import io.imunity.deprovisionig.common.exception.InternalException;
import io.imunity.deprovisionig.saml.metadata.SAMLIdpInfo;
import io.imunity.deprovisionig.unity.types.EntityState;
import io.imunity.deprovisionig.unity.types.UnityUser;

@Component
public class GroovyHookExecutor
{
	private static final Logger log = LogManager.getLogger(GroovyHookExecutor.class);

	private final String hookScript;
	private final TimesConfiguration timesConfig;

	public GroovyHookExecutor(TimesConfiguration timesConfig, @Value("${hookScript:}") String hookScript)
	{
		this.hookScript = hookScript;
		this.timesConfig = timesConfig;
	}

	public void runHook(UnityUser user, EntityState newStatus, SAMLIdpInfo idpInfo, Instant removeTime)
	{
		Reader scriptReader = getFileReader();
		try
		{
			runScript(scriptReader, getBinding(user, newStatus, idpInfo, removeTime));
		} catch (InternalException e)
		{
			log.error("Can not execute groovy script", e);
		} finally
		{
			try
			{
				scriptReader.close();
			} catch (IOException e)
			{
				log.error("Problem closing the stream used to read Groovy script", e);
			}
		}
	}

	private Reader getFileReader()
	{
		try
		{
			InputStream is = new FileInputStream(hookScript);
			return new InputStreamReader(is);
		} catch (IOException e)
		{
			throw new ConfigurationException("Error loading script " + hookScript, e);
		}
	}

	private void runScript(Reader scriptReader, Binding binding) throws InternalException
	{
		GroovyShell shell = new GroovyShell(binding);
		log.info("Triggers invocation of Groovy script: {}", hookScript);

		try
		{
			shell.evaluate(scriptReader);
		} catch (Exception e)
		{
			throw new InternalException("Failed to execute Groovy " + " script: " + hookScript
					+ ": reason: " + e.getMessage(), e);
		}
		log.info("Groovy script: {} finished", hookScript);
	}

	private Binding getBinding(UnityUser user, EntityState newStatus, SAMLIdpInfo idpInfo, Instant removeTime)
	{
		Binding binding = new Binding();

		binding.setVariable("log", log);
		binding.setVariable("user", user);
		binding.setVariable("newUserStatus", newStatus);
		binding.setVariable("idpInfo", idpInfo);
		binding.setVariable("removeTime", removeTime);
		binding.setVariable("timesConfiguration", timesConfig);
		return binding;
	}
}
