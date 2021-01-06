/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.deprovisionig.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkdirFileManager
{
	private static final Logger log = LogManager.getLogger(WorkdirFileManager.class);

	private final String workdir;

	public WorkdirFileManager(@Value("${workdir:data}") String workdir)
	{
		this.workdir = workdir;
	}

	public void saveFile(byte[] content, String filename) throws IOException
	{
		Path fullFilePath = getFullFilePath(filename);
		log.debug("Save file to: " + fullFilePath.toString());
		Files.createDirectories(fullFilePath.getParent());
		Files.write(fullFilePath, new ByteArrayInputStream(content).readAllBytes());
	}

	public boolean exists(String fileName)
	{
		return Files.exists(getFullFilePath(fileName));
	}

	public Instant getLastModifiedTime(String fileName) throws IOException
	{
		return Files.getLastModifiedTime((getFullFilePath(fileName))).toInstant();
	}

	private Path getFullFilePath(String filename)
	{
		return Paths.get(workdir, filename);
	}

	public ByteArrayInputStream readFile(String filename) throws IOException
	{
		Path fileToRead = getFullFilePath(filename);
		log.info("Read file from: " + fileToRead.toString());
		return new ByteArrayInputStream(Files.readAllBytes(fileToRead));
	}
}
