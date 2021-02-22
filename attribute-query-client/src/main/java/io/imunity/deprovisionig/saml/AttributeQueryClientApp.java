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

import io.imunity.deprovisionig.common.WorkdirFileManager;
import io.imunity.deprovisionig.common.saml.AttributeQueryClient;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

@SpringBootApplication
@ComponentScan(basePackages = "io.imunity.deprovisionig")
public class AttributeQueryClientApp implements CommandLineRunner
{
	private static final Logger log = LogManager.getLogger(AttributeQueryClientApp.class);

	private final AttributeQueryClient query;
	private final WorkdirFileManager fileMan;

	@Autowired
	public AttributeQueryClientApp(AttributeQueryClient query, WorkdirFileManager fileMan)
	{
		this.query = query;
		this.fileMan = fileMan;
	}

	public static void main(String[] args)
	{
		new SpringApplicationBuilder(AttributeQueryClientApp.class).bannerMode(Mode.OFF)
				.web(WebApplicationType.NONE).run(args);
	}

	public void run(String... args)
	{
		log.info("Starting attribute query client");

		if (args.length != 2)
		{
			throw new IllegalArgumentException("Invalid arguments used. The basic syntax is: attributeClient <UserIdentity <AttributeQueryServiceUrl>");
		}

		String userIdentity = args[0];
		String attributeQueryServiceUrl = args[1];

		ResponseDocument queryResult;
		try
		{
			queryResult = query.queryForRawAssertion(attributeQueryServiceUrl, userIdentity);
		} catch (Exception e)
		{
			log.error("Can not perform attribute query for user " + userIdentity, e);
			return;
		}

		saveFile(queryResult, userIdentity);
	}

	public void saveFile(ResponseDocument queryResult, String userIdentity)
	{
		try
		{
			String fileName = userIdentity + "_" + UUID.randomUUID().toString().substring(0, 5) + ".xml";
			fileMan.saveFile(queryResult.toString().getBytes(), fileName);
			log.info("Attribute query result for user " + userIdentity + "save in " + fileName);

		} catch (IOException e)
		{
			log.error("Can not save file with attribute query result", e);
		}
	}

}
