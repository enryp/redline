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

package com.metaformsystems.redline.domain.service;

import com.metaformsystems.redline.api.dto.request.DataspaceInfo;
import com.metaformsystems.redline.api.dto.request.ParticipantDeployment;
import com.metaformsystems.redline.api.dto.request.PartnerReferenceRequest;
import com.metaformsystems.redline.api.dto.request.TenantRegistration;
import com.metaformsystems.redline.api.dto.response.VirtualParticipantAgent;
import com.metaformsystems.redline.application.service.TokenProvider;
import com.metaformsystems.redline.domain.entity.Dataspace;
import com.metaformsystems.redline.domain.entity.Participant;
import com.metaformsystems.redline.domain.entity.PartnerReference;
import com.metaformsystems.redline.domain.entity.ServiceProvider;
import com.metaformsystems.redline.domain.entity.Tenant;
import com.metaformsystems.redline.domain.exception.ObjectNotFoundException;
import com.metaformsystems.redline.domain.repository.DataspaceRepository;
import com.metaformsystems.redline.domain.repository.ParticipantRepository;
import com.metaformsystems.redline.domain.repository.ServiceProviderRepository;
import com.metaformsystems.redline.domain.repository.TenantRepository;
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
        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);

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

    @Test
    void shouldGetTenantsByServiceProvider() {
        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        tenantService.registerTenant(serviceProvider.getId(), registration);

        var tenants = tenantService.getTenants(serviceProvider.getId());

        assertThat(tenants).isNotNull();
        assertThat(tenants).hasSize(1);
        assertThat(tenants.get(0).name()).isEqualTo("Test Tenant");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void shouldDeployParticipant() {

        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
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

        var deployment = new ParticipantDeployment(participant.id(), "did:web:example.com:participant");


        var result = tenantService.deployParticipant(deployment);


        assertThat(result).isNotNull();
        assertThat(result.identifier()).isEqualTo("did:web:example.com:participant");
        assertThat(result.agents()).hasSize(3);
        assertThat(result.agents()).anyMatch(agent -> agent.type() == VirtualParticipantAgent.Type.CONTROL_PLANE);
        assertThat(result.agents()).anyMatch(agent -> agent.type() == VirtualParticipantAgent.Type.CREDENTIAL_SERVICE);
        assertThat(result.agents()).anyMatch(agent -> agent.type() == VirtualParticipantAgent.Type.DATA_PLANE);

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

        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
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

        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);


        var result = tenantService.getTenant(tenant.id());


        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Tenant");
        assertThat(result.participants()).hasSize(1);
        assertThat(result.participants().getFirst().identifier()).isEqualTo("Test Tenant");
    }

    @Test
    void shouldDeployParticipantWithExistingTenantCorrelationId() {

        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
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

        var deployment = new ParticipantDeployment(participant.id(), "did:web:example.com:participant2");


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

        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participantId = tenant.participants().iterator().next().id();

        // Add partners to the participant's dataspace info
        var participant = participantRepository.findById(participantId).orElseThrow();
        var dataspaceInfo = participant.getDataspaceInfos().iterator().next();
        var references = new ArrayList<PartnerReference>();
        var partner1Properties = Map.<String, Object>of("key1", "value1", "key2", 123);
        var partner2Properties = Map.<String, Object>of("key3", "value3");
        references.add(new PartnerReference("did:web:partner1.com", "Partner One", partner1Properties));
        references.add(new PartnerReference("did:web:partner2.com", "Partner Two", partner2Properties));
        dataspaceInfo.setPartners(references);
        participantRepository.save(participant);


        var result = tenantService.getPartnerReferences(participantId, dataspace.getId());


        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(ref -> ref.identifier().equals("did:web:partner1.com") 
                && ref.nickname().equals("Partner One")
                && ref.properties().equals(partner1Properties));
        assertThat(result).anyMatch(ref -> ref.identifier().equals("did:web:partner2.com") 
                && ref.nickname().equals("Partner Two")
                && ref.properties().equals(partner2Properties));
    }

    @Test
    void shouldGetPartnerReferences_withEmptyProperties() {

        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participantId = tenant.participants().iterator().next().id();

        // Add partners to the participant's dataspace info with empty properties
        var participant = participantRepository.findById(participantId).orElseThrow();
        var dataspaceInfo = participant.getDataspaceInfos().iterator().next();
        var references = new ArrayList<PartnerReference>();
        references.add(new PartnerReference("did:web:partner1.com", "Partner One"));
        references.add(new PartnerReference("did:web:partner2.com", "Partner Two", Map.of()));
        dataspaceInfo.setPartners(references);
        participantRepository.save(participant);


        var result = tenantService.getPartnerReferences(participantId, dataspace.getId());


        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(ref -> ref.identifier().equals("did:web:partner1.com") 
                && ref.nickname().equals("Partner One")
                && ref.properties() != null
                && ref.properties().isEmpty());
        assertThat(result).anyMatch(ref -> ref.identifier().equals("did:web:partner2.com") 
                && ref.nickname().equals("Partner Two")
                && ref.properties() != null
                && ref.properties().isEmpty());
    }

    @Test
    void shouldGetParticipantDataspaces() {
        // Setup: create tenant with participant and dataspace info
        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participantId = tenant.participants().iterator().next().id();

        // Test
        var result = tenantService.getParticipantDataspaces(participantId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(dataspace.getId());
        assertThat(result.get(0).name()).isEqualTo("Test Dataspace");
    }

    @Test
    void shouldGetParticipantDataspaces_withMultipleDataspaces() {
        // Create additional dataspace
        final var dataspace2 = new Dataspace();
        dataspace2.setName("Second Dataspace");
        dataspaceRepository.save(dataspace2);

        // Setup: create tenant with participant and multiple dataspace infos
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

        // Test
        var result = tenantService.getParticipantDataspaces(participant.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(ds -> ds.id().equals(dataspace.getId()) && ds.name().equals("Test Dataspace"));
        assertThat(result).anyMatch(ds -> ds.id().equals(dataspace2.getId()) && ds.name().equals("Second Dataspace"));
    }

    @Test
    void shouldGetParticipantDataspaces_whenNoDataspaces() {
        // Setup: create participant without dataspace infos
        var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        tenant = tenantRepository.save(tenant);

        var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(tenant);
        tenant.addParticipant(participant);
        participant = participantRepository.save(participant);

        // Test
        var result = tenantService.getParticipantDataspaces(participant.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void shouldCreatePartnerReference() {
        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participantId = tenant.participants().iterator().next().id();

        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name", Map.of("key", "value"));

        var result = tenantService.createPartnerReference(serviceProvider.getId(), tenant.id(), participantId, dataspace.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.identifier()).isEqualTo("did:web:partner.com");
        assertThat(result.nickname()).isEqualTo("Partner Name");
        assertThat(result.properties()).containsEntry("key", "value");

        // Verify partner was saved in database
        var participant = participantRepository.findById(participantId).orElseThrow();
        var dataspaceInfo = participant.getDataspaceInfos().iterator().next();
        assertThat(dataspaceInfo.getPartners()).hasSize(1);
        assertThat(dataspaceInfo.getPartners().get(0).identifier()).isEqualTo("did:web:partner.com");
        assertThat(dataspaceInfo.getPartners().get(0).nickname()).isEqualTo("Partner Name");
    }

    @Test
    void shouldCreatePartnerReference_withProperties() {
        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participantId = tenant.participants().iterator().next().id();

        var properties = Map.<String, Object>of(
                "region", "EU",
                "compliance", "GDPR",
                "active", true
        );
        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name", properties);

        var result = tenantService.createPartnerReference(serviceProvider.getId(), tenant.id(), participantId, dataspace.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.properties()).containsEntry("region", "EU");
        assertThat(result.properties()).containsEntry("compliance", "GDPR");
        assertThat(result.properties()).containsEntry("active", true);
    }

    @Test
    void shouldCreatePartnerReference_withoutProperties() {
        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participantId = tenant.participants().iterator().next().id();

        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name");

        var result = tenantService.createPartnerReference(serviceProvider.getId(), tenant.id(), participantId, dataspace.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.identifier()).isEqualTo("did:web:partner.com");
        assertThat(result.nickname()).isEqualTo("Partner Name");
        assertThat(result.properties()).isNotNull();
        assertThat(result.properties()).isEmpty();
    }

    @Test
    void shouldNotCreatePartnerReference_whenParticipantNotFound() {
        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);

        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                tenantService.createPartnerReference(serviceProvider.getId(), tenant.id(), 999L, dataspace.getId(), request)))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("Participant not found with id: 999");
    }

    @Test
    void shouldNotCreatePartnerReference_whenParticipantDoesNotBelongToTenant() {
        var infos1 = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration1 = new TenantRegistration("Tenant One", infos1);
        var tenant1 = tenantService.registerTenant(serviceProvider.getId(), registration1);
        var participantId = tenant1.participants().iterator().next().id();

        var infos2 = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration2 = new TenantRegistration("Tenant Two", infos2);
        var tenant2 = tenantService.registerTenant(serviceProvider.getId(), registration2);

        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                tenantService.createPartnerReference(serviceProvider.getId(), tenant2.id(), participantId, dataspace.getId(), request)))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("does not belong to tenant");
    }

    @Test
    void shouldCreatePartnerReference_whenTenantDoesNotBelongToProvider() {
        // Create another service provider
        var otherProvider = new ServiceProvider();
        otherProvider.setName("Other Provider");
        otherProvider = serviceProviderRepository.save(otherProvider);

        var infos = List.of(new DataspaceInfo(dataspace.getId(), List.of(), List.of(), Map.of()));
        var registration = new TenantRegistration("Test Tenant", infos);
        var tenant = tenantService.registerTenant(otherProvider.getId(), registration);
        var participantId = tenant.participants().iterator().next().id();

        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                tenantService.createPartnerReference(serviceProvider.getId(), tenant.id(), participantId, dataspace.getId(), request)))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("does not belong to service provider");
    }

    @Test
    void shouldCreatePartnerReference_whenDataspaceInfoNotFound() {
        final var tenant = new Tenant();
        tenant.setName("Test Tenant");
        tenant.setServiceProvider(serviceProvider);
        final var savedTenant = tenantRepository.save(tenant);

        final var participant = new Participant();
        participant.setIdentifier("Test Participant");
        participant.setTenant(savedTenant);
        // No dataspace info added
        savedTenant.addParticipant(participant);
        final var savedParticipant = participantRepository.save(participant);

        var request = new PartnerReferenceRequest("did:web:partner.com", "Partner Name");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                tenantService.createPartnerReference(serviceProvider.getId(), savedTenant.getId(), savedParticipant.getId(), dataspace.getId(), request)))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("Dataspace info not found");
    }

    @Test
    void shouldGetParticipantDataspaces_whenParticipantNotFound() {
        // Test with non-existent participant ID
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> 
                tenantService.getParticipantDataspaces(999L)))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("Participant not found with id: 999");
    }

}
