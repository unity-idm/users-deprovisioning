package io.imunity.deprovisionig;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class DeprovisioningAgentApplicationTests
{
	public static void main(String[] args)
	{
		new SpringApplicationBuilder(DeprovisioningAgentApplicationTests.class).bannerMode(Mode.OFF)
		.web(WebApplicationType.NONE).run(args);
	}
}