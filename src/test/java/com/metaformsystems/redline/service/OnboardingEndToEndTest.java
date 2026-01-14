package com.metaformsystems.redline.service;

import com.metaformsystems.redline.client.identityhub.IdentityHubClient;
import com.metaformsystems.redline.dao.DataplaneRegistration;
import com.metaformsystems.redline.dao.NewAsset;
import com.metaformsystems.redline.dao.NewCelExpression;
import com.metaformsystems.redline.dao.NewContractDefinition;
import com.metaformsystems.redline.dao.NewParticipantDeployment;
import com.metaformsystems.redline.dao.NewPolicyDefinition;
import com.metaformsystems.redline.dao.NewTenantRegistration;
import com.metaformsystems.redline.dao.Criterion;
import com.metaformsystems.redline.model.Dataspace;
import com.metaformsystems.redline.model.ServiceProvider;
import com.metaformsystems.redline.repository.DataspaceRepository;
import com.metaformsystems.redline.repository.ServiceProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EnabledIfEnvironmentVariable(named = "ENABLE_ONBOARDING_TESTS", matches = "true", disabledReason = "This can only run if ENABLE_ONBOARDING_TESTS=true is set in the environment.")
@SpringBootTest
@ActiveProfiles("dev")
// disable transactional tests, because awaitility is used, and opening new threads creates new transactions.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class OnboardingEndToEndTest {
    @Autowired
    private TenantService tenantService;
    @Autowired
    private ControlPlaneService controlPlaneService;
    @Autowired
    private DataspaceRepository dataspaceRepository;
    @Autowired
    private ServiceProviderRepository serviceProviderRepository;
    @Autowired
    private IdentityHubClient identityHubClient;

    private ServiceProvider serviceProvider;
    private Dataspace dataspace;


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("tenant-manager.url", () -> "http://tm.vps.beardyinc.com");
        registry.add("vault.url", () -> "http://vault.vps.beardyinc.com");
        registry.add("identityhub.url", () -> "http://ih.vps.beardyinc.com/cs");
        registry.add("controlplane.url", () -> "http://cp.vps.beardyinc.com/api/mgmt/v4alpha");
        registry.add("keycloak.tokenurl", () -> "http://auth.vps.beardyinc.com/realms/edcv/protocol/openid-connect/token");
    }

    @BeforeEach
    void setUp() {
        // Create test data
        serviceProvider = new ServiceProvider();
        serviceProvider.setName("Test Provider");
        serviceProvider = serviceProviderRepository.save(serviceProvider);

        dataspace = new Dataspace();
        dataspace.setName("Test Dataspace");
        dataspace = dataspaceRepository.save(dataspace);
    }

    @Test
    void shouldOnboard() {

        var registration = new NewTenantRegistration("Test Tenant", List.of(dataspace.getId()));
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participant = tenant.getParticipants().iterator().next();

        var webDid = "did:web:identityhub.edc-v.svc.cluster.local%3A7083:test-participant-" + UUID.randomUUID();
        var deployment = new NewParticipantDeployment(participant.getId(), webDid);

        // deploy the profile to CFM
        var result = tenantService.deployParticipant(deployment);

        // wait for it to complete
        var participantContextId = new AtomicReference<String>();
        await()
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> {
                    var pcId = tenantService.getParticipantContextId(result.id());
                    assertThat(pcId).isNotNull();
                    participantContextId.set(pcId);
                });

        // fetch the secret from the vault
        var clientCredentials = tenantService.getClientCredentials(participantContextId.get());
        assertThat(clientCredentials).isNotNull();

        // create CEL expression
        var membershipExpr = "membership_expr_" + participantContextId.get();
        controlPlaneService.createCelExpression(NewCelExpression.Builder.aNewCelExpression()
                .id(membershipExpr)
                .leftOperand("MembershipCredential")
                .description("Expression for evaluating membership credential")
                .scopes(Set.of("catalog", "contract.negotiation", "transfer.process"))
                .expression("ctx.agent.claims.vc.filter(c, c.type.exists(t, t == 'MembershipCredential')).exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now))")
                .build());

        // create HTTP asset
        var todoAsset = "todo_asset_" + participantContextId.get();
        var permission = "membership_asset";
        controlPlaneService.createAsset(participantContextId.get(), NewAsset.Builder.aNewAsset()
                .id(todoAsset)
                .properties(Map.of("description", "This asset requires the Membership credential to access"))
                .privateProperties(Map.of("permission", permission))
                .dataAddress(Map.of(
                        "@type", "DataAddress",
                        "type", "HttpDataAddress",
                        "baseUrl", "https://jsonplaceholder.typicode.com/todos",
                        "proxyPath", "true",
                        "proxyQueryParams", "true"))
                .build());
        // create policy
        var membershipPolicy = "membership_policy_" + participantContextId.get();
        controlPlaneService.createPolicy(participantContextId.get(), NewPolicyDefinition.Builder.aNewPolicyDefinition()
                .id(membershipPolicy)
                .policy(new NewPolicyDefinition.PolicySet(List.of(new NewPolicyDefinition.PolicySet.Permission("use", List.of(new NewPolicyDefinition.PolicySet.Constraint("MembershipCredential", "eq", "active"))))))
                .build());

        // create contract definition
        var membershipContractDef = "membership_contract_def_" + participantContextId.get();
        controlPlaneService.createContractDefinition(participantContextId.get(), NewContractDefinition.Builder.aNewContractDefinition()
                .id(membershipContractDef)
                .accessPolicyId(membershipPolicy)
                .contractPolicyId(membershipPolicy)
                .assetsSelector(Set.of(new Criterion("privateProperties.'https://w3id.org/edc/v0.0.1/ns/permission'",
                        "=", permission)))
                .build());


        controlPlaneService.prepareDataplane(participantContextId.get(), DataplaneRegistration.Builder.aDataplaneRegistration()
                .allowedSourceTypes(List.of("HttpData", "HttpCertData"))
                .allowedTransferTypes(List.of("HttpData-PULL"))
                .url("http://dataplane.edc-v.svc.cluster.local:8083/api/control/v1/dataflows") //todo: replace with config
                .build());

        // now for some test assertions:
        assertThat(identityHubClient.getParticipant(participantContextId.get())).isNotNull();
        assertThat(identityHubClient.queryCredentialsByType(participantContextId.get(), "MembershipCredential")).hasSize(1);
    }
}
