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
import com.metaformsystems.redline.api.dto.request.Constraint;
import com.metaformsystems.redline.api.dto.request.ContractRequest;
import com.metaformsystems.redline.application.service.TokenProvider;
import com.metaformsystems.redline.domain.entity.ClientCredentials;
import com.metaformsystems.redline.domain.entity.Dataspace;
import com.metaformsystems.redline.domain.entity.Participant;
import com.metaformsystems.redline.domain.entity.ServiceProvider;
import com.metaformsystems.redline.domain.entity.Tenant;
import com.metaformsystems.redline.domain.entity.UploadedFile;
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
public class EdcDataControllerTest {
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
        var publicMetadata = new MockPart("publicMetadata", "{\"foo\": \"bar\"}".getBytes());
        publicMetadata.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var privateMetadata = new MockPart("privateMetadata", "{\"private\": \"value\"}".getBytes());
        privateMetadata.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Mock the upload response from the dataplane
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\": \"generated-file-id-123\"}")
                .addHeader("Content-Type", "application/json"));

        // mock create-cel-expression
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // mock create-asset
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // mock create-policy
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        //mock create-contractdef
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));


        mockMvc.perform(multipart("/api/ui/service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/files",
                        serviceProvider.getId(), tenant.getId(), participant.getId())
                        .file(mockFile)
                        .part(publicMetadata, privateMetadata))
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
                .andExpect(status().isInternalServerError());
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

        var contractRequest = new ContractRequest();
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

        var prohibition = new Constraint(
                "purpose",
                "NEQ",
                "commercial"
        );
        var permission = new Constraint(
                "action",
                "EQ",
                "read"
        );
        var obligation = new Constraint(
                "consequence",
                "EQ",
                "audit"
        );

        var contractRequest = new ContractRequest();
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