/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package com.metaformsystems.redline.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaformsystems.redline.api.dto.request.DataspaceInfo;
import com.metaformsystems.redline.api.dto.request.ParticipantDeployment;
import com.metaformsystems.redline.api.dto.request.PartnerReferenceRequest;
import com.metaformsystems.redline.api.dto.request.ServiceProvider;
import com.metaformsystems.redline.api.dto.request.TenantRegistration;
import com.metaformsystems.redline.application.service.TokenProvider;
import com.metaformsystems.redline.domain.entity.ClientCredentials;
import com.metaformsystems.redline.domain.entity.Dataspace;
import com.metaformsystems.redline.domain.entity.DeploymentState;
import com.metaformsystems.redline.domain.entity.Participant;
import com.metaformsystems.redline.domain.entity.Tenant;
import com.metaformsystems.redline.domain.entity.VirtualParticipantAgent;
import com.metaformsystems.redline.domain.repository.DataspaceRepository;
import com.metaformsystems.redline.domain.repository.ParticipantRepository;
import com.metaformsystems.redline.domain.repository.ServiceProviderRepository;
import com.metaformsystems.redline.domain.repository.TenantRepository;
import com.metaformsystems.redline.domain.service.WebDidResolver;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class TenantControllerIntegrationTest {
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

    private com.metaformsystems.redline.domain.entity.ServiceProvider serviceProvider;
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
        serviceProvider = new com.metaformsystems.redline.domain.entity.ServiceProvider();
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
        var newProvider = new ServiceProvider("New Provider");

        mockMvc.perform(post("/api/ui/service-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProvider)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Provider"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldRegisterTenant() throws Exception {
        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("New Tenant", infos);

        mockMvc.perform(post("/api/ui/service-providers/{serviceProviderId}/tenants", serviceProvider.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Tenant"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.participants", hasSize(1)));
    }

    @Test
    void shouldRegisterTenant_withProperties() throws Exception {
        var info = new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of());
        var infos = List.of(info);
        var registration = new TenantRegistration("New Tenant", infos, Map.of("foo", "bar", "bar", 42));

        mockMvc.perform(post("/api/ui/service-providers/{serviceProviderId}/tenants", serviceProvider.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Tenant"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.properties.foo").value("bar"))
                .andExpect(jsonPath("$.properties.bar").value(42));
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
    void shouldGetAllTenantsByServiceProvider() throws Exception {
        // Create tenants for the service provider
        var tenant1 = new Tenant();
        tenant1.setName("Tenant One");
        tenant1.setServiceProvider(serviceProvider);
        tenantRepository.save(tenant1);

        var participant1 = new Participant();
        participant1.setIdentifier("Participant One");
        tenant1.addParticipant(participant1);
        participantRepository.save(participant1);

        var tenant2 = new Tenant();
        tenant2.setName("Tenant Two");
        tenant2.setServiceProvider(serviceProvider);
        tenantRepository.save(tenant2);

        var participant2 = new Participant();
        participant2.setIdentifier("Participant Two");
        tenant2.addParticipant(participant2);
        participantRepository.save(participant2);

        //create a tenant for another service provider
        var otherServiceProvider = new com.metaformsystems.redline.domain.entity.ServiceProvider();
        otherServiceProvider.setName("Other Provider");
        otherServiceProvider = serviceProviderRepository.save(otherServiceProvider);
        var otherTenant = new Tenant();
        otherTenant.setName("Other Tenant");
        otherTenant.setServiceProvider(otherServiceProvider);
        tenantRepository.save(otherTenant);
        var otherParticipant = new Participant();
        otherParticipant.setIdentifier("Other Participant");
        otherTenant.addParticipant(otherParticipant);
        participantRepository.save(otherParticipant);


        mockMvc.perform(get("/api/ui/service-providers/{serviceProviderId}/tenants",
                        serviceProvider.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.name == 'Tenant One')].participants", hasSize(1)))
                .andExpect(jsonPath("$[?(@.name == 'Tenant Two')].participants", hasSize(1)));
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

        var deployment = new ParticipantDeployment(participant.getId(), "did:web:example.com:participant");

        mockMvc.perform(post("/api/ui/service-providers/{serviceProviderId}/tenants/{tenantId}/participants/{participantId}/deployments",
                        serviceProvider.getId(), tenant.getId(), participant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deployment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("did:web:example.com:participant"))
                .andExpect(jsonPath("$.agents", hasSize(3)));
    }

    @Test
    void shouldGetParticipantDataspaces() throws Exception {
        // Create a tenant and participant with dataspace info
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        
        // Add dataspace info to participant
        var dataspaceInfo = new com.metaformsystems.redline.domain.entity.DataspaceInfo();
        dataspaceInfo.setDataspaceId(dataspace.getId());
        participant.getDataspaceInfos().add(dataspaceInfo);
        
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        mockMvc.perform(get("/api/ui/service-providers/{serviceProviderId}/tenants/{tenantId}/participants/{participantId}/dataspaces",
                        serviceProvider.getId(), tenant.getId(), participant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(dataspace.getId()))
                .andExpect(jsonPath("$[0].name").value("Test Dataspace"));
    }

    @Test
    void shouldGetParticipantDataspaces_withMultipleDataspaces() throws Exception {
        // Create additional dataspace
        var dataspace2 = new Dataspace();
        dataspace2.setName("Second Dataspace");
        dataspace2 = dataspaceRepository.save(dataspace2);

        // Create a tenant and participant with multiple dataspace infos
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        
        // Add first dataspace info
        var dataspaceInfo1 = new com.metaformsystems.redline.domain.entity.DataspaceInfo();
        dataspaceInfo1.setDataspaceId(dataspace.getId());
        participant.getDataspaceInfos().add(dataspaceInfo1);
        
        // Add second dataspace info
        var dataspaceInfo2 = new com.metaformsystems.redline.domain.entity.DataspaceInfo();
        dataspaceInfo2.setDataspaceId(dataspace2.getId());
        participant.getDataspaceInfos().add(dataspaceInfo2);
        
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        mockMvc.perform(get("/api/ui/service-providers/{serviceProviderId}/tenants/{tenantId}/participants/{participantId}/dataspaces",
                        serviceProvider.getId(), tenant.getId(), participant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.containsInAnyOrder(dataspace.getId().intValue(), dataspace2.getId().intValue())))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.containsInAnyOrder("Test Dataspace", "Second Dataspace")));
    }

    @Test
    void shouldCreatePartnerReference() throws Exception {
        // Create a tenant and participant with dataspace info
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        
        // Add dataspace info to participant
        var dataspaceInfo = new com.metaformsystems.redline.domain.entity.DataspaceInfo();
        dataspaceInfo.setDataspaceId(dataspace.getId());
        participant.getDataspaceInfos().add(dataspaceInfo);
        
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name", Map.of("key", "value"));

        mockMvc.perform(post("/api/ui/service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/partners/{dataspaceId}",
                        serviceProvider.getId(), tenant.getId(), participant.getId(), dataspace.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("did:web:partner.com"))
                .andExpect(jsonPath("$.nickname").value("Partner Name"))
                .andExpect(jsonPath("$.properties.key").value("value"));

        // Verify partner was saved
        var savedParticipant = participantRepository.findById(participant.getId()).orElseThrow();
        var savedDataspaceInfo = savedParticipant.getDataspaceInfos().iterator().next();
        assertThat(savedDataspaceInfo.getPartners()).hasSize(1);
        assertThat(savedDataspaceInfo.getPartners().get(0).identifier()).isEqualTo("did:web:partner.com");
        assertThat(savedDataspaceInfo.getPartners().get(0).nickname()).isEqualTo("Partner Name");
    }

    @Test
    void shouldCreatePartnerReference_withProperties() throws Exception {
        // Create a tenant and participant with dataspace info
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        
        var dataspaceInfo = new com.metaformsystems.redline.domain.entity.DataspaceInfo();
        dataspaceInfo.setDataspaceId(dataspace.getId());
        participant.getDataspaceInfos().add(dataspaceInfo);
        
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        var properties = Map.<String, Object>of(
                "region", "EU",
                "compliance", "GDPR",
                "metadata", Map.of("createdBy", "admin", "tags", List.of("partner", "trusted"))
        );
        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name", properties);

        mockMvc.perform(post("/api/ui/service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/partners/{dataspaceId}",
                        serviceProvider.getId(), tenant.getId(), participant.getId(), dataspace.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("did:web:partner.com"))
                .andExpect(jsonPath("$.nickname").value("Partner Name"))
                .andExpect(jsonPath("$.properties.region").value("EU"))
                .andExpect(jsonPath("$.properties.compliance").value("GDPR"));
    }

    @Test
    void shouldNotCreatePartnerReference_whenParticipantNotFound() throws Exception {
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name");

        mockMvc.perform(post("/api/ui/service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/partners/{dataspaceId}",
                        serviceProvider.getId(), tenant.getId(), 999L, dataspace.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Participant not found")));
    }

    @Test
    void shouldGetParticipantDataspaces_whenParticipantNotFound() throws Exception {
        // Create a tenant without participant
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        mockMvc.perform(get("/api/ui/service-providers/{serviceProviderId}/tenants/{tenantId}/participants/{participantId}/dataspaces",
                        serviceProvider.getId(), tenant.getId(), 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Participant not found")));
    }

}
