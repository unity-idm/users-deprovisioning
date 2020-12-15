package io.imunity.deprovisionig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import io.imunity.deprovisionig.common.saml.AttributeQueryClient;
import io.imunity.deprovisionig.extractor.UnityUserExtractor;
import io.imunity.deprovisionig.hook.GroovyHookExecutor;
import io.imunity.deprovisionig.saml.metadata.SAMLMetadataManager;
import io.imunity.deprovisionig.unity.UnityApiClient;
import io.imunity.deprovisionig.verificator.MainUserVerificator;

@SpringBootApplication
public class DeprovisionigAgentApplication implements CommandLineRunner
{

	private static final Logger log = LogManager.getLogger(DeprovisionigAgentApplication.class);

	@Autowired
	private UnityUserExtractor extractor;

	@Autowired
	private SAMLMetadataManager samlMetaManager;

	@Autowired
	AttributeQueryClient query;

	@Autowired
	UnityApiClient apiClient;

	@Autowired
	private MainUserVerificator verificator;

	@Autowired
	private GroovyHookExecutor groovyHook;

	public static void main(String[] args)
	{
		new SpringApplicationBuilder(DeprovisionigAgentApplication.class).bannerMode(Mode.OFF)
				.web(WebApplicationType.NONE).run(args);
	}

	@Override
	public void run(String... args) throws Exception
	{
		log.info("Starting unity deprovisioning agent");

		// Set<UnityUser> extractUnityUsers =
		// extractor.extractUnityUsers();
		// System.out.println(extractUnityUsers.toString());

		// Map<String, SAMLIdpInfo> entitiesDescriptorDocument =
		// samlMetaManager.getAttributeQueryAddressesAsMap();
		// System.out.println(entitiesDescriptorDocument);

		// query.getAttributes("https://localhost:2443/soapidp/saml2idp-soap/AssertionQueryService",
		// "16c0c3d2-c7fe-47a2-b3d5-2c5370bff8d8");
		// query.queryToFile(args[0], args[1]);
		// onlineUserVerificator.verifyUsers(extractor.extractUnityUsers());

		// apiClient.sendEmail("fb7d3791-d419-4213-af7f-7db11a34b295",
		// "demo", Map.of("a","b"));

		// Attribute attr = new Attribute();
		// attr.setGroupPath("/");
		// attr.setName("deprovisioning:LastHomeIdPVerification");
		// attr.setValues(Arrays.asList(LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

		// Identity id = new Identity();
		// id.setTypeId("email");
		// id.setValue("demo@demo.pl");
		// groovyHook.run(new UnityUser(1L, EntityState.disabled,
		// Arrays.asList(id),
		// new HashSet<>(Arrays.asList("/")), LocalDateTime.now(),
		// LocalDateTime.now(),
		// LocalDateTime.now()), EntityState.valid,
		// new SAMLIdpInfo("idp", "queryService", "demo@demo.pl"));
		//
		// //
		// apiClient.updateAttribute("c840d9f5-323d-449f-9e75-d73745c8b7ac",
		// attr);

	}

}
