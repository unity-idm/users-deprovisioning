/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.saml;

import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

import eu.unicore.samly2.assertion.AttributeAssertionParser;
import io.imunity.deprovisionig.common.WorkdirFileManager;
import io.imunity.deprovisionig.common.exception.InternalException;
import io.imunity.deprovisionig.common.saml.AttributeQueryClient;

@SpringBootApplication
@ComponentScan(basePackages = "io.imunity.deprovisionig")
public class AttributeQueryClientApp implements CommandLineRunner
{
	private static final Logger log = LogManager.getLogger(AttributeQueryClientApp.class);

	private AttributeQueryClient query;
	private WorkdirFileManager fileMan;

	@Autowired
	public AttributeQueryClientApp(AttributeQueryClient query, WorkdirFileManager fileMan)
	{
		this.query = query;
		this.fileMan = fileMan;
	}

	public static void main(String[] args)
	{
		new SpringApplicationBuilder(AttributeQueryClientApp.class).bannerMode(Mode.OFF).web(WebApplicationType.NONE).run(args);
	}

	public void run(String... args)
	{
		log.info("Starting attribute query");

		if (args.length < 2)
		{
			throw new IllegalArgumentException("UserId and attribute service url are required");
		}

		String user = args[0];
		String url = args[1];

		AttributeAssertionParser queryResult;
		try
		{
			queryResult = query.query(url, user);
		} catch (InternalException e)
		{
			log.error("Attribute query failed", e);
			return;
		}

		saveFile(queryResult, user);
	}

	public void saveFile(AttributeAssertionParser attributesDoc, String user)
	{
		try
		{
			String fileName = user + "_" + UUID.randomUUID().toString() + ".xml";
			fileMan.saveFile(attributesDoc.getXMLBeanDoc().toString().getBytes(), fileName);
			log.info("Query result save in " + fileName);

		} catch (IOException e)
		{
			log.error("Can not save file with attribute query result", e);
		}

	}

}
