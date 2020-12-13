package imunity.io.deprovisionig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import javax.swing.event.ListSelectionEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.sun.xml.ws.policy.privateutil.PolicyUtils.Collections;

import imunity.io.deprovisionig.extractor.UnityUserExtractor;
import imunity.io.deprovisionig.hook.GroovyHookExecutor;
import imunity.io.deprovisionig.saml.AttributeQueryClient;
import imunity.io.deprovisionig.saml.metadata.SAMLIdpInfo;
import imunity.io.deprovisionig.saml.metadata.SAMLMetadataManager;
import imunity.io.deprovisionig.unity.UnityApiClient;
import imunity.io.deprovisionig.unity.types.Attribute;
import imunity.io.deprovisionig.unity.types.EntityState;
import imunity.io.deprovisionig.unity.types.Identity;
import imunity.io.deprovisionig.unity.types.UnityUser;
import imunity.io.deprovisionig.verificator.MainUserVerificator;

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
	private MainUserVerificator onlineUserVerificator;

	@Autowired
	private GroovyHookExecutor groovyHook;

	public static void main(String[] args)
	{
		SpringApplication.run(DeprovisionigAgentApplication.class, args);
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

		// query.query("https://localhost:2443/soapidp/saml2idp-soap/AssertionQueryService",
		// "379bee81-3833-4bd4-9ed0-9693104f0106");

		// onlineUserVerificator.verifyUsers(extractor.extractUnityUsers());

		// apiClient.sendEmail("fb7d3791-d419-4213-af7f-7db11a34b295",
		// "demo", Map.of("a","b"));

		// Attribute attr = new Attribute();
		// attr.setGroupPath("/");
		// attr.setName("deprovisioning:LastHomeIdPVerification");
		// attr.setValues(Arrays.asList(LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

		Identity id = new Identity();
		id.setTypeId("email");
		id.setValue("demo@demo.pl");
		groovyHook.run(new UnityUser(1L, EntityState.disabled, Arrays.asList(id),
				new HashSet<>(Arrays.asList("/")), LocalDateTime.now(), LocalDateTime.now(),
				LocalDateTime.now()), EntityState.valid,
				new SAMLIdpInfo("idp", "queryService", "demo@demo.pl"));

		// apiClient.updateAttribute("c840d9f5-323d-449f-9e75-d73745c8b7ac",
		// attr);

	}

}
