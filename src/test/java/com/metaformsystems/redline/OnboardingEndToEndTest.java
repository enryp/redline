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

package com.metaformsystems.redline;

import com.metaformsystems.redline.client.dataplane.DataPlaneApiClient;
import com.metaformsystems.redline.client.identityhub.IdentityHubClient;
import com.metaformsystems.redline.client.management.ManagementApiClient;
import com.metaformsystems.redline.client.management.dto.ContractRequest;
import com.metaformsystems.redline.client.management.dto.Criterion;
import com.metaformsystems.redline.client.management.dto.NewAsset;
import com.metaformsystems.redline.client.management.dto.NewCelExpression;
import com.metaformsystems.redline.client.management.dto.NewContractDefinition;
import com.metaformsystems.redline.client.management.dto.NewPolicyDefinition;
import com.metaformsystems.redline.client.management.dto.Offer;
import com.metaformsystems.redline.client.management.dto.PolicySet;
import com.metaformsystems.redline.dao.DataplaneRegistration;
import com.metaformsystems.redline.dao.NewDataspaceInfo;
import com.metaformsystems.redline.dao.NewParticipantDeployment;
import com.metaformsystems.redline.dao.NewTenantRegistration;
import com.metaformsystems.redline.model.Dataspace;
import com.metaformsystems.redline.model.ServiceProvider;
import com.metaformsystems.redline.repository.DataspaceRepository;
import com.metaformsystems.redline.repository.ServiceProviderRepository;
import com.metaformsystems.redline.service.TenantService;
import org.jetbrains.annotations.NotNull;
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

import java.io.IOException;
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
    private ManagementApiClient managementApiClient;
    @Autowired
    private DataspaceRepository dataspaceRepository;
    @Autowired
    private ServiceProviderRepository serviceProviderRepository;
    @Autowired
    private IdentityHubClient identityHubClient;
    @Autowired
    private DataPlaneApiClient dataPlaneApiClient;

    private ServiceProvider serviceProvider;
    private Dataspace dataspace;


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("tenant-manager.url", () -> "http://tm.vps.beardyinc.com");
        registry.add("vault.url", () -> "http://vault.vps.beardyinc.com");
        registry.add("identityhub.url", () -> "http://ih.vps.beardyinc.com/cs");
        registry.add("controlplane.url", () -> "http://cp.vps.beardyinc.com/api/mgmt");
        registry.add("dataplane.internal.url", () -> "http://dp.vps.beardyinc.com/app/internal/api/control");
        registry.add("dataplane.url", () -> "http://dp.vps.beardyinc.com/app/public/api/data");
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

        var participantInfo = onboardParticipant();

        // now for some test assertions:
        assertThat(identityHubClient.getParticipant(participantInfo.contextId())).isNotNull();
        assertThat(identityHubClient.queryCredentialsByType(participantInfo.contextId(), "MembershipCredential")).hasSize(1);
    }

    @Test
    void shouldDownloadTodo() {
        var providerInfo = onboardParticipant();
        var consumerInfo = onboardParticipant();

        // prepare consumer: create CEL expression
        managementApiClient.createCelExpression(NewCelExpression.Builder.aNewCelExpression()
                .id("membership_expr_" + consumerInfo.contextId())
                .leftOperand("MembershipCredential")
                .description("Expression for evaluating membership credential")
                .scopes(Set.of("catalog", "contract.negotiation", "transfer.process"))
                .expression("ctx.agent.claims.vc.filter(c, c.type.exists(t, t == 'MembershipCredential')).exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now))")
                .build());

        //prepare provider - create asset, policy etc.
        var todoAssetId = "todo_asset_" + providerInfo.contextId();
        publishHttpAsset(providerInfo.contextId(), todoAssetId);
        registerDataPlane(providerInfo.contextId());

        // now acting as the consumer, getting the provider's catalog
        var catalog = tenantService.requestCatalog(consumerInfo.id(), providerInfo.webDid(), "no-cache");

        // get the asset with id "todo_asset"
        var dataset = catalog.getDataset().stream().filter(ds -> ds.getId().equals(todoAssetId)).findFirst().orElseThrow();
        var offers = dataset.getHasPolicy();

        var policyId = offers.getFirst().getId();
        assertThat(policyId).isNotNull();

        // start transfer using the all-in-one API from JAD
        var result = managementApiClient.getData(consumerInfo.contextId(), providerInfo.webDid(), policyId);
        assertThat(result).isNotNull().isInstanceOf(List.class);

        // check transfer process
        var transferProcesses = tenantService.listTransferProcesses(consumerInfo.id());
        assertThat(transferProcesses).isNotEmpty();
        assertThat(transferProcesses.getFirst().getContractId()).isNotNull();

        //check contracts
        var contracts = tenantService.listContracts(consumerInfo.id());
        assertThat(contracts).isNotEmpty();
        assertThat(contracts).allSatisfy(c -> assertThat(c.getContractAgreement()).isNotNull());
    }

    @Test
    void shouldInitiateContractNegotiation() {
        var providerInfo = onboardParticipant();
        var consumerInfo = onboardParticipant();

        // prepare consumer: create CEL expression
        managementApiClient.createCelExpression(NewCelExpression.Builder.aNewCelExpression()
                .id("membership_expr_" + consumerInfo.contextId())
                .leftOperand("MembershipCredential")
                .description("Expression for evaluating membership credential")
                .scopes(Set.of("catalog", "contract.negotiation", "transfer.process"))
                .expression("ctx.agent.claims.vc.filter(c, c.type.exists(t, t == 'MembershipCredential')).exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now))")
                .build());

        //prepare provider - create asset, policy etc.
        var todoAssetId = "todo_asset_" + providerInfo.contextId();
        publishHttpAsset(providerInfo.contextId(), todoAssetId);
        registerDataPlane(providerInfo.contextId());

        // now acting as the consumer, getting the provider's catalog
        var catalog = tenantService.requestCatalog(consumerInfo.id(), providerInfo.webDid(), "no-cache");

        // get the asset with id "todo_asset"
        var dataset = catalog.getDataset().stream().filter(ds -> ds.getId().equals(todoAssetId)).findFirst().orElseThrow();
        var offers = dataset.getHasPolicy();

        var first = offers.getFirst();
        var policyId = first.getId();
        assertThat(policyId).isNotNull();

        // start transfer using the all-in-one API from JAD
        var cr = ContractRequest.Builder.aContractRequest()
                .providerId(providerInfo.webDid())
                .counterPartyAddress("foobar")
                .callbackAddresses(Set.of())
                .policy(Offer.Builder.anOffer()
                        .id(policyId)
                        .assigner(providerInfo.webDid())
                        .obligation(first.getObligation())
                        .permission(first.getPermission())
                        .prohibition(first.getProhibition())
                        .target(todoAssetId)
                        .build())
                .build();
        var id = managementApiClient.initiateContractNegotiation(consumerInfo.contextId(), cr);
        assertThat(id).isNotNull();
    }

    @Test
    void shouldDownloadCert() throws IOException {
        var providerInfo = onboardParticipant();
        var consumerInfo = onboardParticipant();

        // prepare consumer: create CEL expression
        managementApiClient.createCelExpression(NewCelExpression.Builder.aNewCelExpression()
                .id("membership_expr_" + consumerInfo.contextId())
                .leftOperand("MembershipCredential")
                .description("Expression for evaluating membership credential")
                .scopes(Set.of("catalog", "contract.negotiation", "transfer.process"))
                .expression("ctx.agent.claims.vc.filter(c, c.type.exists(t, t == 'MembershipCredential')).exists(c, c.credentialSubject.exists(cs, timestamp(cs.membershipStartDate) < now))")
                .build());

        //prepare provider - create asset, policy etc.
        var certAssertId = "cert_asset_" + providerInfo.contextId();
        var fileId = publishCertificateAsset(providerInfo.contextId(), certAssertId, "testdocument.pdf");
        registerDataPlane(providerInfo.contextId());

        // now acting as the consumer, getting the provider's catalog
        var catalog = managementApiClient.getCatalog(consumerInfo.contextId(), providerInfo.webDid());

        // get the asset with id "cert_asset"
        var dataset = catalog.getDataset().stream().filter(ds -> ds.getId().equals(certAssertId)).findFirst().orElseThrow();
        var offers = dataset.getHasPolicy();

        var policyId = offers.getFirst().getId();
        assertThat(policyId).isNotNull();

        var endpointDataReference = managementApiClient.setupTransfer(consumerInfo.contextId(), policyId, providerInfo.webDid());
        var bytes = dataPlaneApiClient.downloadFile(endpointDataReference.get("https://w3id.org/edc/v0.0.1/ns/authorization"), fileId);
        var expectedBytes = Thread.currentThread().getContextClassLoader().getResourceAsStream("testdocument.pdf").readAllBytes();
        assertThat(bytes).isEqualTo(expectedBytes);
    }

    private String publishCertificateAsset(String participantContextId, String certificateAssetId, String resourceName) {
        // create HTTP asset
        var permission = "membership_asset";
        managementApiClient.createAsset(participantContextId, NewAsset.Builder.aNewAsset()
                .id(certificateAssetId)
                .properties(Map.of("description", "This asset requires the Membership credential to access"))
                .privateProperties(Map.of("permission", permission))
                .dataAddress(Map.of(
                        "@type", "DataAddress",
                        "type", "HttpCertData"))
                .build());
        // create policy
        var membershipPolicy = "membership_policy_" + participantContextId;
        managementApiClient.createPolicy(participantContextId, NewPolicyDefinition.Builder.aNewPolicyDefinition()
                .id(membershipPolicy)
                .policy(new PolicySet(List.of(new PolicySet.Permission("use", List.of(new PolicySet.Constraint("MembershipCredential", "eq", "active"))))))
                .build());

        // create contract definition
        var membershipContractDef = "membership_contract_def_" + participantContextId;
        managementApiClient.createContractDefinition(participantContextId, NewContractDefinition.Builder.aNewContractDefinition()
                .id(membershipContractDef)
                .accessPolicyId(membershipPolicy)
                .contractPolicyId(membershipPolicy)
                .assetsSelector(Set.of(new Criterion("privateProperties.'https://w3id.org/edc/v0.0.1/ns/permission'",
                        "=", permission)))
                .build());

        var fileId = new AtomicReference<String>();
        try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            var response = dataPlaneApiClient.uploadMultipart(participantContextId, Map.of(), is);
            fileId.set(response.id());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return fileId.get();
    }

    @NotNull
    private ParticipantInfo onboardParticipant() {
        var slug = UUID.randomUUID().toString();
        var infos = List.of(new NewDataspaceInfo(dataspace.getId(), List.of(), List.of()));
        var tenantName = "Test Tenant " + slug;
        var registration = new NewTenantRegistration(tenantName, infos);
        var tenant = tenantService.registerTenant(serviceProvider.getId(), registration);
        var participant = tenant.participants().getFirst();

        var webDid = "did:web:identityhub.edc-v.svc.cluster.local%3A7083:test-participant-" + slug;
        var deployment = new NewParticipantDeployment(participant.id(), webDid);

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

        return new ParticipantInfo(participantContextId.get(), webDid, participant.id());
    }

    private void registerDataPlane(String participantContextId) {
        managementApiClient.prepareDataplane(participantContextId, DataplaneRegistration.Builder.aDataplaneRegistration()
                .allowedSourceTypes(List.of("HttpData", "HttpCertData"))
                .allowedTransferTypes(List.of("HttpData-PULL"))
                .destinationProvisionTypes(List.of("HttpData", "HttpCertData", "httpData", "httpCertData"))
                .url("http://dataplane.edc-v.svc.cluster.local:8083/api/control/v1/dataflows") //todo: replace with config
                .build());
    }

    private void publishHttpAsset(String participantContextId, String assetId) {
        // create HTTP asset
        var permission = "membership_asset";
        managementApiClient.createAsset(participantContextId, NewAsset.Builder.aNewAsset()
                .id(assetId)
                .properties(Map.of("description", "This asset requires the Membership credential to access"))
                .privateProperties(Map.of("permission", permission))
                .dataAddress(Map.of(
                        "@type", "DataAddress",
                        "type", "HttpData",
                        "baseUrl", "https://jsonplaceholder.typicode.com/todos",
                        "proxyPath", "true",
                        "proxyQueryParams", "true"))
                .build());
        // create policy
        var membershipPolicy = "membership_policy_" + participantContextId;
        managementApiClient.createPolicy(participantContextId, NewPolicyDefinition.Builder.aNewPolicyDefinition()
                .id(membershipPolicy)
                .policy(new PolicySet(List.of(new PolicySet.Permission("use", List.of(new PolicySet.Constraint("MembershipCredential", "eq", "active"))))))
                .build());

        // create contract definition
        var membershipContractDef = "membership_contract_def_" + participantContextId;
        managementApiClient.createContractDefinition(participantContextId, NewContractDefinition.Builder.aNewContractDefinition()
                .id(membershipContractDef)
                .accessPolicyId(membershipPolicy)
                .contractPolicyId(membershipPolicy)
                .assetsSelector(Set.of(new Criterion("privateProperties.'https://w3id.org/edc/v0.0.1/ns/permission'",
                        "=", permission)))
                .build());
    }

    private record ParticipantInfo(String contextId, String webDid, Long id) {
    }
}
