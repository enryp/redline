package com.metaformsystems.redline.service;

import com.metaformsystems.redline.client.TokenProvider;
import com.metaformsystems.redline.client.management.dto.Constraint;
import com.metaformsystems.redline.client.management.dto.ContractRequest;
import com.metaformsystems.redline.client.management.dto.Obligation;
import com.metaformsystems.redline.client.management.dto.Offer;
import com.metaformsystems.redline.dao.NewDataspaceInfo;
import com.metaformsystems.redline.dao.NewParticipantDeployment;
import com.metaformsystems.redline.dao.NewTenantRegistration;
import com.metaformsystems.redline.dao.VPAResource;
import com.metaformsystems.redline.model.ClientCredentials;
import com.metaformsystems.redline.model.Dataspace;
import com.metaformsystems.redline.model.Participant;
import com.metaformsystems.redline.model.PartnerReference;
import com.metaformsystems.redline.model.ServiceProvider;
import com.metaformsystems.redline.model.Tenant;
import com.metaformsystems.redline.model.UploadedFile;
import com.metaformsystems.redline.repository.DataspaceRepository;
import com.metaformsystems.redline.repository.ParticipantRepository;
import com.metaformsystems.redline.repository.ServiceProviderRepository;
import com.metaformsystems.redline.repository.TenantRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.metaformsystems.redline.TestData.PARTICIPANT_PROFILE_RESPONSE;
import static com.metaformsystems.redline.TestData.VAULT_CREDENTIAL_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TenantServiceIntegrationTest {

    static final String mockBackEndHost = "localhost";
    static final int mockBackEndPort = TestSocketUtils.findAvailableTcpPort();
    private static final String CATALOG_RESPONSE = """
            {
                "@type": "dcat:Catalog",
                "dcat:dataset": [],
                "dcat:service": []
            }
            """;
    private MockWebServer mockWebServer;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private DataspaceRepository dataspaceRepository;
    @Autowired
    private ServiceProviderRepository serviceProviderRepository;
    private ServiceProvider serviceProvider;
    private Dataspace dataspace;
    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private WebDidResolver webDidResolver;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        registry.add("tenant-manager.url", () -> "http://%s:%s/tm".formatted(mockBackEndHost, mockBackEndPort));
        registry.add("vault.url", () -> "http://%s:%s/vault".formatted(mockBackEndHost, mockBackEndPort));
        registry.add("controlplane.url", () -> "http://%s:%s/cp".formatted(mockBackEndHost, mockBackEndPort));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        // Create test data
        serviceProvider = new ServiceProvider();
        serviceProvider.setName("Test Provider");
        serviceProvider = serviceProviderRepository.save(serviceProvider);

        dataspace = new Dataspace();
        dataspace.setName("Test Dataspace");
        dataspace = dataspaceRepository.save(dataspace);

        mockWebServer = new MockWebServer();
        mockWebServer.start(InetAddress.getByName(mockBackEndHost), mockBackEndPort);
        when(tokenProvider.getToken(anyString(), anyString(), anyString())).thenReturn("mock-token");
    }

    @Test
    void shouldRegisterTenant() {
        var infos = List.of(new NewDataspaceInfo(dataspace.getId(), List.of(), List.of()));
        var registration = new NewTenantRegistration("Test Tenant", infos);

        var tenantResource = tenantService.registerTenant(serviceProvider.getId(), registration);
        var tenant = tenantRepository.findById(tenantResource.id()).orElseThrow();

        assertThat(tenantResource).isNotNull();
        assertThat(tenant.getName()).isEqualTo("Test Tenant");
        assertThat(tenant.getServiceProvider()).isEqualTo(serviceProvider);
        assertThat(tenant.getParticipants()).hasSize(1);

        var participant = tenant.getParticipants().iterator().next();
        assertThat(participant.getIdentifier()).isEqualTo("Test Tenant");
        assertThat(participant.getDataspaceInfos()).hasSize(1);
        assertThat(participant.getDataspaceInfos().iterator().next().getDataspaceId()).isEqualTo(dataspace.getId());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void shouldDeployParticipant() {

        var infos = List.of(new NewDataspaceInfo(dataspace.getId(), List.of(), List.of()));
        var registration = new NewTenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participant = tenant.participants().iterator().next();

        // Mock tenant creation response
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                           "id": "1e546b5a-0f7a-466b-bf54-aca8df8c3117",
                           "version": 0,
                           "properties": {
                             "location": "eu",
                             "name": "Consumer Tenant"
                           }
                         }
                        """)
                .addHeader("Content-Type", "application/json"));

        // Mock participant profile creation response
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                            "id": "dfe5b1c2-112c-4556-947a-ddbe92f9358a",
                            "version": 0,
                            "identifier": "did:web:example.com:participant",
                            "tenantId": "1e546b5a-0f7a-466b-bf54-aca8df8c3117",
                            "services": {},
                            "vpas": [
                                {
                                      "id": "ac77f6e0-e631-4fe8-904d-8dff5425a978",
                                      "version": 0,
                                      "state": "pending",
                                      "type": "cfm.connector",
                                      "cellId": "621bbfd1-7e97-4934-93f1-86d19954c9b1"
                                    },
                                    {
                                      "id": "6e36d1ca-ca41-4a71-a70c-bf0956e1a875",
                                      "version": 0,
                                      "state": "pending",
                                      "type": "cfm.credentialservice",
                                      "cellId": "621bbfd1-7e97-4934-93f1-86d19954c9b1"
                                    },
                                    {
                                      "id": "1ebcd321-cb01-47ee-add6-95199acea06e",
                                      "version": 0,
                                      "state": "pending",
                                      "type": "cfm.dataplane",
                                      "cellId": "621bbfd1-7e97-4934-93f1-86d19954c9b1"
                                    }
                            ]
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        var deployment = new NewParticipantDeployment(participant.id(), "did:web:example.com:participant");


        var result = tenantService.deployParticipant(deployment);


        assertThat(result).isNotNull();
        assertThat(result.identifier()).isEqualTo("did:web:example.com:participant");
        assertThat(result.agents()).hasSize(3);
        assertThat(result.agents()).anyMatch(agent -> agent.type() == VPAResource.Type.CONTROL_PLANE);
        assertThat(result.agents()).anyMatch(agent -> agent.type() == VPAResource.Type.CREDENTIAL_SERVICE);
        assertThat(result.agents()).anyMatch(agent -> agent.type() == VPAResource.Type.DATA_PLANE);

        // Verify database state
        var updatedTenant = tenantRepository.findById(tenant.id()).orElseThrow();
        assertThatCode(() -> UUID.fromString(updatedTenant.getCorrelationId())).doesNotThrowAnyException();

        var updatedParticipant = participantRepository.findById(participant.id()).orElseThrow();
        assertThatCode(() -> UUID.fromString(updatedParticipant.getCorrelationId())).doesNotThrowAnyException();
        assertThat(updatedParticipant.getIdentifier()).isEqualTo("did:web:example.com:participant");
        assertThat(updatedParticipant.getAgents()).hasSize(3);
    }

    @Test
    void shouldGetParticipant() {

        var infos = List.of(new NewDataspaceInfo(dataspace.getId(), List.of(), List.of()));
        var registration = new NewTenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participant = tenant.participants().iterator().next();

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).addHeader("content-type", "application/json").setBody(PARTICIPANT_PROFILE_RESPONSE));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).addHeader("content-type", "application/json").setBody(VAULT_CREDENTIAL_RESPONSE));


        var result = tenantService.getParticipant(participant.id());


        assertThat(result).isNotNull();
        assertThat(result.identifier()).isEqualTo("Test Tenant");
    }

    @Test
    void shouldGetTenant() {

        var infos = List.of(new NewDataspaceInfo(dataspace.getId(), List.of(), List.of()));
        var registration = new NewTenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);


        var result = tenantService.getTenant(tenant.id());


        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Tenant");
        assertThat(result.participants()).hasSize(1);
        assertThat(result.participants().getFirst().identifier()).isEqualTo("Test Tenant");
    }

    @Test
    void shouldDeployParticipantWithExistingTenantCorrelationId() {

        var infos = List.of(new NewDataspaceInfo(dataspace.getId(), List.of(), List.of()));
        var registration = new NewTenantRegistration("Test Tenant", infos);
        var tenantResource = tenantService.registerTenant(serviceProvider.getId(), registration);
        var tenant = tenantRepository.findById(tenantResource.id()).orElseThrow();
        tenant.setCorrelationId("existing-tenant-id");
        tenantRepository.save(tenant);

        var participant = tenantResource.participants().iterator().next();

        // Mock only participant profile creation (tenant already exists)
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                           "id": "dfe5b1c2-112c-4556-947a-ddbe92f9358a",
                           "version": 0,
                           "identifier": "did:web:example.com:participant2",
                           "tenantId": "1e546b5a-0f7a-466b-bf54-aca8df8c3117",
                           "participantRoles": {},
                           "vpas": [
                             {
                               "id": "ac77f6e0-e631-4fe8-904d-8dff5425a978",
                               "version": 0,
                               "state": "pending",
                               "type": "cfm.connector",
                               "cellId": "621bbfd1-7e97-4934-93f1-86d19954c9b1"
                             },
                             {
                               "id": "6e36d1ca-ca41-4a71-a70c-bf0956e1a875",
                               "version": 0,
                               "state": "pending",
                               "type": "cfm.credentialservice",
                               "cellId": "621bbfd1-7e97-4934-93f1-86d19954c9b1"
                             },
                             {
                               "id": "1ebcd321-cb01-47ee-add6-95199acea06e",
                               "version": 0,
                               "state": "pending",
                               "type": "cfm.dataplane",
                               "cellId": "621bbfd1-7e97-4934-93f1-86d19954c9b1"
                             }
                           ],
                           "error": false
                         }
                        """)
                .addHeader("Content-Type", "application/json"));

        var deployment = new NewParticipantDeployment(participant.id(), "did:web:example.com:participant2");


        var result = tenantService.deployParticipant(deployment);


        assertThat(result).isNotNull();
        assertThat(result.identifier()).isEqualTo("did:web:example.com:participant2");
    }

    @Test
    void shouldGetParticipantContextId() {

        var tenantId = "tenant-123";
        var tenant = new Tenant();
        tenant.setCorrelationId(tenantId);
        var participantId = "participant-456";
        var expectedContextId = "ctx-789";

        var entity = new Participant();
        entity.setCorrelationId(participantId);
        entity.setTenant(tenant);
        entity = participantRepository.save(entity);
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                            "id": "%s",
                            "properties": {
                                "cfm.vpa.state": {
                                    "participantContextId": "%s",
                                    "holderPid": "pid-1",
                                    "credentialRequestUrl": "http://example.com/requests/1"
                                }
                            }
                        }
                        """.formatted(participantId, expectedContextId))
                .addHeader("Content-Type", "application/json"));


        var result = tenantService.getParticipantContextId(entity.getId());


        assertThat(result).isEqualTo(expectedContextId);
    }

    @Test
    void shouldGetParticipantContextId_notReadyYet() {

        var tenantId = "tenant-123";
        var tenant = new Tenant();
        tenant.setCorrelationId(tenantId);
        var participantId = "participant-456";
        var expectedContextId = "ctx-789";
        var entity = new Participant();
        entity.setCorrelationId(participantId);
        entity.setTenant(tenant);
        entity = participantRepository.save(entity);
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                            "id": "%s"
                        }
                        """.formatted(participantId, expectedContextId))
                .addHeader("Content-Type", "application/json"));


        var result = tenantService.getParticipantContextId(entity.getId());


        assertThat(result).isNull();
    }

    @Test
    void shouldGetClientCredentials() {
        var vaultResponse = """
                {
                  "request_id": "eb024e5f-a5ab-3f8d-3e60-0539db2e14c8",
                  "lease_id": "",
                  "renewable": false,
                  "lease_duration": 0,
                  "data": {
                    "data": {
                      "content": "0b1dbc7e87fc60080f8cd409e475a0b1ac018079eab17491ff87a5c383f9d802"
                    },
                    "metadata": {
                      "created_time": "2026-01-14T08:17:18.993403846Z",
                      "custom_metadata": null,
                      "deletion_time": "",
                      "destroyed": false,
                      "version": 1
                    }
                  },
                  "wrap_info": null,
                  "warnings": null,
                  "auth": null,
                  "mount_type": "kv"
                }
                """;

        mockWebServer.enqueue(new MockResponse().setBody(vaultResponse).addHeader("Content-Type", "application/json"));

        var entity = new Participant();
        entity.setParticipantContextId("test-participant-context-id");
        entity = participantRepository.save(entity);

        var creds = tenantService.getClientCredentials("test-participant-context-id");
        assertThat(creds).isNotNull();
        assertThat(creds.clientId()).isEqualTo("test-participant-context-id");
        assertThat(creds.clientSecret()).isEqualTo("0b1dbc7e87fc60080f8cd409e475a0b1ac018079eab17491ff87a5c383f9d802");
        assertThat(participantRepository.findById(entity.getId()).orElseThrow().getClientCredentials()).isNotNull();
    }

    @Test
    void shouldNotGetCredentials_when404() {
        var response = """
                {
                  "errors": []
                }
                """;
        mockWebServer.enqueue(new MockResponse().setBody(response).addHeader("Content-Type", "application/json").setResponseCode(404));

        assertThat(tenantService.getClientCredentials("test-participant-context-id")).isNull();
    }

    @Test
    void shouldGetPartnerReferences() {

        var infos = List.of(new NewDataspaceInfo(dataspace.getId(), List.of(), List.of()));
        var registration = new NewTenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participantId = tenant.participants().iterator().next().id();

        // Add partners to the participant's dataspace info
        var participant = participantRepository.findById(participantId).orElseThrow();
        var dataspaceInfo = participant.getDataspaceInfos().iterator().next();
        var references = new ArrayList<PartnerReference>();
        references.add(new PartnerReference("did:web:partner1.com", "Partner One"));
        references.add(new PartnerReference("did:web:partner2.com", "Partner Two"));
        dataspaceInfo.setPartners(references);
        participantRepository.save(participant);


        var result = tenantService.getPartnerReferences(participantId, dataspace.getId());


        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(ref -> ref.identifier().equals("did:web:partner1.com") && ref.nickname().equals("Partner One"));
        assertThat(result).anyMatch(ref -> ref.identifier().equals("did:web:partner2.com") && ref.nickname().equals("Partner Two"));
    }

    @Test
    void shouldRequestCatalog_andCacheIt() throws InterruptedException {

        var participant = createAndSaveParticipant("ctx-1", "did:web:me");
        var counterParty = "did:web:them";

        // First call: Expect fetch from remote
        mockWebServer.enqueue(new MockResponse()
                .setBody(CATALOG_RESPONSE)
                .addHeader("Content-Type", "application/json"));


        var catalog1 = tenantService.requestCatalog(participant.getId(), counterParty, "max-age=3600");
        var catalog2 = tenantService.requestCatalog(participant.getId(), counterParty, "max-age=3600");


        assertThat(catalog1).isNotNull();
        assertThat(catalog2).isNotNull();
        // only 1 catalog request, second one is cached
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void shouldRequestCatalog_andBypassCacheWithNoCache() {
        var participant = createAndSaveParticipant("ctx-2", "did:web:me");
        var counterParty = "did:web:them";

        mockWebServer.enqueue(new MockResponse().setBody(CATALOG_RESPONSE).addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody(CATALOG_RESPONSE).addHeader("Content-Type", "application/json"));

        tenantService.requestCatalog(participant.getId(), counterParty, "max-age=3600");
        tenantService.requestCatalog(participant.getId(), counterParty, "no-cache");

        // both requests hit the remote catalog, no cache
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void shouldRequestCatalog_andRefreshWhenMaxAgeIsZero() {
        var participant = createAndSaveParticipant("ctx-3", "did:web:me");
        var counterParty = "did:web:them";

        mockWebServer.enqueue(new MockResponse().setBody(CATALOG_RESPONSE).addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody(CATALOG_RESPONSE).addHeader("Content-Type", "application/json"));

        tenantService.requestCatalog(participant.getId(), counterParty, "max-age=3600");
        // max-age=0 should trigger expiration check effectively immediately or force refresh logic
        tenantService.requestCatalog(participant.getId(), counterParty, "max-age=0");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void shouldListContracts() throws InterruptedException {
        var participant = createAndSaveParticipant("ctx-4", "did:web:me");

        var contractsResponse = """
                [
                    {
                        "@id": "negotiation-1",
                        "@type": "ContractNegotiation",
                        "type": "CONSUMER",
                        "state": "FINALIZED",
                        "counterPartyId": "did:web:provider",
                        "counterPartyAddress": "http://provider.example.com/api/dsp",
                        "contractAgreementId": "agreement-1"
                    },
                    {
                        "@id": "negotiation-2",
                        "@type": "ContractNegotiation",
                        "type": "PROVIDER",
                        "state": "REQUESTED",
                        "counterPartyId": "did:web:consumer",
                        "counterPartyAddress": "http://consumer.example.com/api/dsp"
                    }
                ]
                """;

        var agreementResponse = """
                {
                    "@id": "agreement-1",
                    "@type": "ContractAgreement",
                    "providerId": "did:web:provider",
                    "consumerId": "did:web:consumer",
                    "assetId": "asset-1",
                    "policy": {
                        "@type": "Policy"
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse().setBody(contractsResponse).addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setBody(agreementResponse).addHeader("Content-Type", "application/json"));

        var result = tenantService.listContracts(participant.getId());

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(cn -> cn.getId().equals("negotiation-1") && cn.getState().equals("FINALIZED"));
        assertThat(result).anyMatch(cn -> cn.getId().equals("negotiation-2") && cn.getState().equals("REQUESTED"));
        assertThat(result.stream().filter(cn -> cn.getId().equals("negotiation-1")).findFirst().orElseThrow().getContractAgreement()).isNotNull();
        assertThat(result.stream().filter(cn -> cn.getId().equals("negotiation-2")).findFirst().orElseThrow().getContractAgreement()).isNull();

        var contractsRequest = mockWebServer.takeRequest();
        assertThat(contractsRequest.getPath()).isEqualTo("/cp/v4alpha/participants/ctx-4/contractnegotiations/request");
        assertThat(contractsRequest.getMethod()).isEqualTo("POST");

        var agreementRequest = mockWebServer.takeRequest();
        assertThat(agreementRequest.getPath()).isEqualTo("/cp/v4alpha/participants/ctx-4/contractnegotiations/negotiation-1/agreement");
        assertThat(agreementRequest.getMethod()).isEqualTo("GET");

    }

    @Test
    void shouldListFiles() {
        var participant = createAndSaveParticipant("ctx-5", "did:web:me");

        participant.setUploadedFiles(new ArrayList<>(List.of(
                new UploadedFile("file-id-1", "foobar.jpg", "image/jpeg", Map.of("bar", "baz")),
                new UploadedFile("file-id-2", "barbaz.pdf", "application/pdf", Map.of("quizz", "qazz"))
        )));

        participant = participantRepository.save(participant);

        var result = tenantService.listFilesForParticipant(participant.getId());

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(f -> f.fileId().equals("file-id-1") &&
                f.fileName().equals("foobar.jpg") &&
                f.contentType().equals("image/jpeg") &&
                f.metadata().get("bar").equals("baz"));
        assertThat(result).anyMatch(f -> f.fileId().equals("file-id-2") &&
                f.fileName().equals("barbaz.pdf") &&
                f.contentType().equals("application/pdf") &&
                f.metadata().get("quizz").equals("qazz"));
    }

    @Test
    void shouldInitiateContractNegotiation() throws InterruptedException {
        var participant = createAndSaveParticipant("ctx-6", "did:web:me");
        var providerId = "did:web:provider";
        var assetId = "asset-123";
        var offerId = "offer-456";

        when(webDidResolver.resolveProtocolEndpoints(eq(providerId)))
                .thenReturn("http://provider.example.com/api/dsp");

        var contractRequest = ContractRequest.Builder.aContractRequest()
                .providerId(providerId)
                .policy(Offer.Builder.anOffer()
                        .id(offerId)
                        .target(assetId)
                        .assigner(providerId)
                        .obligation(List.of(Obligation.Builder.anObligation()
                                .action("use")
                                .constraint(List.of(new Constraint("foo", "=", "bar")))
                                .build()))
                        .build())
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setBody("{ \"@id\": \"negotiation-123\"}")
                .addHeader("Content-Type", "application/json"));

        var result = tenantService.initiateContractNegotiation(participant.getId(), contractRequest);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("negotiation-123");

        var request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/cp/v4alpha/participants/ctx-6/contractnegotiations");
        assertThat(request.getMethod()).isEqualTo("POST");
    }


    private Participant createAndSaveParticipant(String contextId, String identifier) {
        var p = new Participant();
        p.setParticipantContextId(contextId);
        p.setIdentifier(identifier);
        p.setClientCredentials(new ClientCredentials("client-id", "client-secret"));
        p.setTenant(serviceProvider.getTenants().stream().findFirst().orElseGet(() -> {
            var t = new Tenant();
            t.setName("Test");
            t.setServiceProvider(serviceProvider);
            return tenantRepository.save(t);
        }));
        return participantRepository.save(p);
    }
}
