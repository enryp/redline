package com.metaformsystems.redline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaformsystems.redline.client.TokenProvider;
import com.metaformsystems.redline.dao.NewDataspaceInfo;
import com.metaformsystems.redline.dao.NewParticipantDeployment;
import com.metaformsystems.redline.dao.NewServiceProvider;
import com.metaformsystems.redline.dao.NewTenantRegistration;
import com.metaformsystems.redline.model.ClientCredentials;
import com.metaformsystems.redline.model.ConstraintDto;
import com.metaformsystems.redline.model.ContractRequestDto;
import com.metaformsystems.redline.model.Dataspace;
import com.metaformsystems.redline.model.DeploymentState;
import com.metaformsystems.redline.model.Participant;
import com.metaformsystems.redline.model.ServiceProvider;
import com.metaformsystems.redline.model.Tenant;
import com.metaformsystems.redline.model.UploadedFile;
import com.metaformsystems.redline.model.VirtualParticipantAgent;
import com.metaformsystems.redline.repository.DataspaceRepository;
import com.metaformsystems.redline.repository.ParticipantRepository;
import com.metaformsystems.redline.repository.ServiceProviderRepository;
import com.metaformsystems.redline.repository.TenantRepository;
import com.metaformsystems.redline.service.WebDidResolver;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class RedlineControllerIntegrationTest {
    static final String mockBackEndHost = "localhost";
    static final int mockBackEndPort = TestSocketUtils.findAvailableTcpPort();
    private MockWebServer mockWebServer;

    @MockitoBean
    private WebDidResolver webDidResolver;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceProviderRepository serviceProviderRepository;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @MockitoBean
    private TokenProvider tokenProvider;

    private ServiceProvider serviceProvider;
    private Dataspace dataspace;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        registry.add("tenant-manager.url", () -> "http://%s:%s".formatted(mockBackEndHost, mockBackEndPort));
        registry.add("vault.url", () -> "http://%s:%s/vault".formatted(mockBackEndHost, mockBackEndPort));
        registry.add("dataplane.url", () -> "http://%s:%s/dataplane".formatted(mockBackEndHost, mockBackEndPort));
        registry.add("dataplane.internal.url", () -> "http://%s:%s/dataplane".formatted(mockBackEndHost, mockBackEndPort));
        registry.add("controlplane.url", () -> "http://%s:%s/controlplane".formatted(mockBackEndHost, mockBackEndPort));

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
        when(tokenProvider.getToken(anyString(), anyString(), anyString())).thenReturn("test-token");
        when(webDidResolver.resolveProtocolEndpoints(anyString())).thenReturn("http://example.com/api");
    }

    @Test
    void shouldGetDataspaces() throws Exception {
        mockMvc.perform(get("/api/ui/dataspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(dataspace.getId()))
                .andExpect(jsonPath("$[0].name").value("Test Dataspace"));
    }

    @Test
    void shouldGetServiceProviders() throws Exception {
        mockMvc.perform(get("/api/ui/service-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(serviceProvider.getId()))
                .andExpect(jsonPath("$[0].name").value("Test Provider"));
    }

    @Test
    void shouldCreateServiceProvider() throws Exception {
        var newProvider = new NewServiceProvider("New Provider");

        mockMvc.perform(post("/api/ui/service-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProvider)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Provider"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldRegisterTenant() throws Exception {
        var infos = List.of(new NewDataspaceInfo(dataspace.getId(), List.of(), List.of()));
        var registration = new NewTenantRegistration("New Tenant", infos);

        mockMvc.perform(post("/api/ui/service-providers/{serviceProviderId}/tenants", serviceProvider.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Tenant"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.participants", hasSize(1)));
    }

    @Test
    void shouldGetTenant() throws Exception {
        // Create a tenant first
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        tenant.addParticipant(participant);
        participantRepository.save(participant);

        mockMvc.perform(get("/api/ui/service-providers/{serviceProviderId}/tenants/{tenantId}",
                        serviceProvider.getId(), tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenant.getId()))
                .andExpect(jsonPath("$.name").value("Test Tenant"))
                .andExpect(jsonPath("$.participants", hasSize(1)));
    }

    @Test
    void shouldGetParticipant() throws Exception {
        // Create a tenant and participant
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        participant.setParticipantContextId("test-participant-context-id");
        participant.setClientCredentials(new ClientCredentials("test-client", "test-secret"));
        participant.setAgents(Set.of(new VirtualParticipantAgent(VirtualParticipantAgent.VpaType.CONTROL_PLANE, DeploymentState.PENDING),
                new VirtualParticipantAgent(VirtualParticipantAgent.VpaType.CREDENTIAL_SERVICE, DeploymentState.PENDING),
                new VirtualParticipantAgent(VirtualParticipantAgent.VpaType.DATA_PLANE, DeploymentState.PENDING)));
        participant = participantRepository.save(participant);

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": "0d93930b-2c92-4421-93f2-533d392ca517",
                          "version": 0,
                          "identifier": "did:web:identityhub.edc-v.svc.cluster.local%3A7083:consumer",
                          "tenantId": "e2387a34-581b-48d0-864c-66b5c859af29",
                          "participantRoles": {},
                          "vpas": [
                            {
                              "id": "69b9dba2-35fc-47ea-b35a-93edb3804ab9",
                              "version": 0,
                              "state": "active",
                              "stateTimestamp": "2026-01-16T08:01:03.809045341Z",
                              "type": "cfm.connector",
                              "cellId": "1db5a032-0515-4e95-bd01-a1069d808bb9"
                            },
                            {
                              "id": "95f509ba-2374-4fe7-8275-e1265a6fcd95",
                              "version": 0,
                              "state": "active",
                              "stateTimestamp": "2026-01-16T08:01:03.809047551Z",
                              "type": "cfm.credentialservice",
                              "cellId": "1db5a032-0515-4e95-bd01-a1069d808bb9"
                            },
                            {
                              "id": "e7650ca5-8e20-4e98-9883-7f72d6362d0c",
                              "version": 0,
                              "state": "active",
                              "stateTimestamp": "2026-01-16T08:01:03.809049133Z",
                              "type": "cfm.dataplane",
                              "cellId": "1db5a032-0515-4e95-bd01-a1069d808bb9"
                            }
                          ],
                          "properties": {
                            "cfm.vpa.state": {
                              "credentialRequest": "http://identityhub.edc-v.svc.cluster.local:7081/v1alpha/participants/ODM0YzkzMDhmMjllNDgwMGI0ZmY3MTRkZTkwNzQ0MzM/credentials/request/c248a998-b73b-4288-8f44-e83812d4448f",
                              "holderPid": "c248a998-b73b-4288-8f44-e83812d4448f",
                              "participantContextId": "834c9308f29e4800b4ff714de9074433"
                            }
                          },
                          "error": false
                        }
                        """));

        mockMvc.perform(get("/api/ui/service-providers/{serviceProviderId}/tenants/{tenantId}/participants/{participantId}",
                        serviceProvider.getId(), tenant.getId(), participant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(participant.getId()))
                .andExpect(jsonPath("$.agents").value(hasSize(3)))
                .andExpect(jsonPath("$.agents[*].state").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalToIgnoringCase("active"))))
                .andExpect(jsonPath("$.identifier").value("Test Participant"));

        assertThat(participantRepository.findById(participant.getId()))
                .isPresent()
                .hasValueSatisfying(p -> assertThat(p.getAgents())
                        .hasSize(3)
                        .allMatch(vpa -> vpa.getState().equals(DeploymentState.ACTIVE)));
    }

    @Test
    void shouldDeployParticipant() throws Exception {
        // Create a tenant and participant
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        // Mock tenant creation response
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                           "id": "1e546b5a-0f7a-466b-bf54-aca8df8c3117",
                           "version": 0,
                           "properties": {
                             "location": "eu",
                             "name": "Test Tenant"
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

        var deployment = new NewParticipantDeployment(participant.getId(), "did:web:example.com:participant");

        mockMvc.perform(post("/api/ui/service-providers/{serviceProviderId}/tenants/{tenantId}/participants/{participantId}/deployments",
                        serviceProvider.getId(), tenant.getId(), participant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deployment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("did:web:example.com:participant"))
                .andExpect(jsonPath("$.agents", hasSize(3)));
    }

    @Test
    void shouldUploadFile() throws Exception {
        // Create a tenant and participant
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        participant.setParticipantContextId("test-participant-context-id");
        participant.setClientCredentials(new ClientCredentials("test-client", "test-secret"));
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        // Create mock file
        var resourcePath = getClass().getClassLoader().getResource("testdocument.pdf").getPath();
        var fileContent = Files.readAllBytes(Paths.get(resourcePath));
        var mockFile = new MockMultipartFile(
                "file",
                "testdocument.pdf",
                "application/pdf",
                fileContent
        );

        // Create metadata
        var metadataPart = new MockPart("metadata", "{\"foo\": \"bar\"}".getBytes());
        metadataPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // mock create-cel-expression
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // mock create-asset
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // mock create-policy
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        //mock create-contractdef
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // Mock the upload response from the dataplane
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\": \"generated-file-id-123\"}")
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(multipart("/api/ui/service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/files",
                        serviceProvider.getId(), tenant.getId(), participant.getId())
                        .file(mockFile)
                        .part(metadataPart))
                .andExpect(status().isOk());

        assertThat(participantRepository.findById(participant.getId())).isPresent()
                .hasValueSatisfying(p -> assertThat(p.getUploadedFiles()).hasSize(1));
    }

    @Test
    void shouldUploadFile_whenPolicyAndContractDefExist() throws Exception {
        // Create a tenant and participant
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        participant.setParticipantContextId("test-participant-context-id");
        participant.setClientCredentials(new ClientCredentials("test-client", "test-secret"));
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        // Create mock file
        var resourcePath = getClass().getClassLoader().getResource("testdocument.pdf").getPath();
        var fileContent = Files.readAllBytes(Paths.get(resourcePath));
        var mockFile = new MockMultipartFile(
                "file",
                "testdocument.pdf",
                "application/pdf",
                fileContent
        );

        // Create metadata
        var metadataPart = new MockPart("metadata", "{\"foo\": \"bar\"}".getBytes());
        metadataPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // mock create-cel-expression
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // mock create-asset
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // mock create-policy
        mockWebServer.enqueue(new MockResponse().setResponseCode(409));

        //mock create-contractdef
        mockWebServer.enqueue(new MockResponse().setResponseCode(409));

        // Mock the upload response from the dataplane
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\": \"generated-file-id-123\"}")
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(multipart("/api/ui/service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/files",
                        serviceProvider.getId(), tenant.getId(), participant.getId())
                        .file(mockFile)
                        .part(metadataPart))
                .andExpect(status().isOk());

        assertThat(participantRepository.findById(participant.getId())).isPresent()
                .hasValueSatisfying(p -> assertThat(p.getUploadedFiles()).hasSize(1));
    }

    @Test
    void shouldGetAllFiles() throws Exception {
        // Create a tenant and participant
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        participant.setParticipantContextId("test-participant-context-id");
        participant.setClientCredentials(new ClientCredentials("test-client", "test-secret"));
        participant.getUploadedFiles().add(new UploadedFile("test-file-id", "test-file-name", "test-file-content-type", Map.of()));
        participant.getUploadedFiles().add(new UploadedFile("test-file-id2", "test-file-name2", "test-file-content-type2", Map.of()));
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        mockMvc.perform(get("/api/ui/service-providers/{serviceProviderId}/tenants/{tenantId}/participants/{participantId}/files",
                        serviceProvider.getId(), tenant.getId(), participant.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].fileId").value(containsInAnyOrder("test-file-id", "test-file-id2")))
                .andExpect(jsonPath("$[*].fileName").value(containsInAnyOrder("test-file-name", "test-file-name2")))
                .andExpect(jsonPath("$[*].contentType").value(containsInAnyOrder("test-file-content-type", "test-file-content-type2")));
    }

    @Test
    void shouldRequestContract() throws Exception {
        // Create a tenant and participant
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        participant.setParticipantContextId("test-participant-context-id");
        participant.setClientCredentials(new ClientCredentials("test-client", "test-secret"));
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        // Mock contract negotiation response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"@id\" : \"negotiation-id-123\"}")
                .addHeader("Content-Type", "application/json"));

        var contractRequest = new ContractRequestDto();
        contractRequest.setAssetId("asset-123");
        contractRequest.setOfferId("offer-456");
        contractRequest.setProviderId("did:web:provider-789");

        mockMvc.perform(post("/api/ui/service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/contracts",
                        serviceProvider.getId(), tenant.getId(), participant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequestContractWithConstraints() throws Exception {
        // Create a tenant and participant
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        participant.setParticipantContextId("test-participant-context-id");
        participant.setClientCredentials(new ClientCredentials("test-client", "test-secret"));
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        // Mock contract negotiation response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"@id\" : \"negotiation-id-123\"}")
                .addHeader("Content-Type", "application/json"));

        var prohibition = new ConstraintDto(
                "purpose",
                "NEQ",
                "commercial"
        );
        var permission = new ConstraintDto(
                "action",
                "EQ",
                "read"
        );
        var obligation = new ConstraintDto(
                "consequence",
                "EQ",
                "audit"
        );

        var contractRequest = new ContractRequestDto();
        contractRequest.setAssetId("asset-123");
        contractRequest.setOfferId("offer-456");
        contractRequest.setProviderId("did:web:provider-789");
        contractRequest.setProhibitions(List.of(prohibition));
        contractRequest.setPermissions(List.of(permission));
        contractRequest.setObligations(List.of(obligation));

        mockMvc.perform(post("/api/ui/service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/contracts",
                        serviceProvider.getId(), tenant.getId(), participant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractRequest)))
                .andExpect(status().isOk());
    }
}
