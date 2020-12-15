/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package imunity.io.deprovisionig;

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
	
	@Value("${workdir:data}")
	private String workdir;
	
	
	public void saveFile(byte[] content, String filename) throws IOException
	{
		log.debug("Save file to: " + filename);
		Path fullFilePath = getFullFilePath(filename);	
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
}
