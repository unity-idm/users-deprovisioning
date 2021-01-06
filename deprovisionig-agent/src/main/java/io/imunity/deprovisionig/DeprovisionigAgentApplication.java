package io.imunity.deprovisionig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import io.imunity.deprovisionig.extractor.UnityUserExtractor;
import io.imunity.deprovisionig.verificator.MainUserVerificator;

@SpringBootApplication
public class DeprovisionigAgentApplication implements CommandLineRunner
{

	private static final Logger log = LogManager.getLogger(DeprovisionigAgentApplication.class);

	private final UnityUserExtractor extractor;
	private final MainUserVerificator verificator;
	
	@Autowired
	DeprovisionigAgentApplication(UnityUserExtractor extractor, MainUserVerificator verificator)
	{	
		this.extractor = extractor;
		this.verificator = verificator;
	}

	public static void main(String[] args)
	{
		new SpringApplicationBuilder(DeprovisionigAgentApplication.class).bannerMode(Mode.OFF)
				.web(WebApplicationType.NONE).run(args);
	}

	@Override
	public void run(String... args) throws Exception
	{
		log.info("Starting unity deprovisioning agent");
		verificator.verifyUsers(extractor.extractUnityUsers());
	}

}
