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

import com.metaformsystems.redline.api.dto.request.ContractRequest;
import com.metaformsystems.redline.api.dto.request.CounterPartyIdWrapper;
import com.metaformsystems.redline.api.dto.request.ParticipantDeployment;
import com.metaformsystems.redline.api.dto.request.TenantRegistration;
import com.metaformsystems.redline.api.dto.request.TransferProcessRequest;
import com.metaformsystems.redline.api.dto.response.ContractNegotiation;
import com.metaformsystems.redline.api.dto.response.DeploymentState;
import com.metaformsystems.redline.api.dto.response.FileResource;
import com.metaformsystems.redline.api.dto.response.Participant;
import com.metaformsystems.redline.api.dto.response.Tenant;
import com.metaformsystems.redline.infrastructure.client.management.dto.Catalog;
import com.metaformsystems.redline.infrastructure.client.management.dto.Constraint;
import com.metaformsystems.redline.infrastructure.client.management.dto.TransferProcess;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * This test runs through a full participant deployment for consumer and provider plus a data transfer between them.
 * For this test a running instance of JAD is required, and the Redline API Server must be reachable at http://redline.localhost.
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_E2E_TESTS", matches = "true", disabledReason = "This can only run if ENABLE_E2E_TESTS=true is set in the environment.")
public class TransferEndToEndTest {
    private static final String BASE_URL = "http://redline.localhost:8080";
    private static final long SERVICE_PROVIDER_ID = 1;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);
    private static final Logger log = LoggerFactory.getLogger(TransferEndToEndTest.class);

    private static RequestSpecification redlineApi() {
        return baseRequest()
                .header("Content-Type", "application/json");
    }

    private static RequestSpecification baseRequest() {
        return given()
                .baseUri(BASE_URL);
    }

    @Test
    void testTransferFile() throws Exception {
        var slug = Instant.now().toEpochMilli();
        var consumerDid = "did:web:identityhub.edc-v.svc.cluster.local%3A7083:test-consumer-" + slug;
        var providerDid = "did:web:identityhub.edc-v.svc.cluster.local%3A7083:test-provider-" + slug;

        log.info("deploying consumer");
        var consumer = deployParticipant(consumerDid, "Test Consumer Tenant " + slug);
        log.info("deploying provider");
        var provider = deployParticipant(providerDid, "Test Provider Tenant " + slug);

        log.info("uploading file to provider");
        // upload file for consumer - this creates asset, policy, contract-def, etc.
        baseRequest()
                .contentType(ContentType.MULTIPART)
                .multiPart("file", "testfile.txt", "This is a test file.".getBytes())
                .multiPart("publicMetadata", "{\"slug\": \"%s\"}".formatted(slug), "application/json")
                .multiPart("privateMetadata", "{\"privateSlug\": \"%s\"}".formatted(slug), "application/json")
                .post("/api/ui/service-providers/%s/tenants/%s/participants/%s/files".formatted(SERVICE_PROVIDER_ID, provider.tenantId(), provider.participantId()))
                .then()
                .statusCode(200);

        // list all files to make sure it's there
        log.info("checking provider files");
        var files = redlineApi()
                .get("/api/ui/service-providers/%s/tenants/%s/participants/%s/files".formatted(SERVICE_PROVIDER_ID, provider.tenantId(), provider.participantId()))
                .then()
                .statusCode(200)
                .extract().body().as(FileResource[].class);
        assertThat(files).anyMatch(uf -> uf.metadata().get("slug").equals(String.valueOf(slug)));
        var fileResource = Arrays.stream(files).filter(fr -> fr.metadata().get("slug").equals(String.valueOf(slug))).findFirst().orElseThrow();

        var assetId = fileResource.metadata().get("assetId").toString();
        var fileId = fileResource.fileId();
        assertThat(assetId).isNotEmpty();

        // get catalog for consumer
        log.info("obtain catalog for consumer");
        var catalog = redlineApi()
                .body(new CounterPartyIdWrapper(providerDid))
                .post("/api/ui/service-providers/%s/tenants/%s/participants/%s/catalog".formatted(SERVICE_PROVIDER_ID, consumer.tenantId(), consumer.participantId()))
                .then()
                .statusCode(200)
                .extract().body().as(Catalog.class);
        assertThat(catalog).isNotNull();
        assertThat(catalog.getDataset()).isNotNull().isNotEmpty();
        assertThat(catalog.getDataset()).anyMatch(ds -> ds.getId().equals(assetId));
        var offeredAsset = catalog.getDataset().stream().filter(ds -> ds.getId().equals(assetId)).findFirst().orElseThrow();
        var offeredPolicy = offeredAsset.getHasPolicy().getFirst();
        var offerId = offeredPolicy.getId();

        // initiate contract negotiation.
        log.info("initiate contract negotiation for asset {}", assetId);
        var contractRequest = ContractRequest.Builder.aContractRequest()
                .assetId(assetId)
                .offerId(offerId)
                .providerId(providerDid)
                .permissions(offeredPolicy.getPermission().getFirst().getConstraint().stream().map(this::mapConstraint).toList())
                // these are likely null, and we're not using them currently anyway
                //  .prohibitions(offeredPolicy.getProhibition().getFirst().getConstraint().stream().map(this::mapConstraint).toList())
                //  .obligations(offeredPolicy.getObligation().getFirst().getConstraint().stream().map(this::mapConstraint).toList())
                .build();

        var negotiationId = redlineApi()
                .body(contractRequest)
                .post("/api/ui/service-providers/%s/tenants/%s/participants/%s/contracts".formatted(SERVICE_PROVIDER_ID, consumer.tenantId, consumer.participantId()))
                .then()
                .statusCode(200)
                .extract().body().asString();
        assertThat(negotiationId).isNotEmpty();

        // wait until contract negotiation reaches a terminal state (FINALIZED)
        log.info("wait for contract negotiation '{}' to finalize", negotiationId);
        AtomicReference<ContractNegotiation> finishedNegotiation = new AtomicReference<>();
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    var negotiation = redlineApi()
                            .get("/api/ui/service-providers/%s/tenants/%s/participants/%s/contracts/%s".formatted(SERVICE_PROVIDER_ID, consumer.tenantId(), consumer.participantId(), negotiationId))
                            .then()
                            .statusCode(200)
                            .extract().body().as(ContractNegotiation.class);
                    assertThat(negotiation.getState()).isEqualTo("FINALIZED");
                    finishedNegotiation.set(negotiation);
                });

        var agreementId = finishedNegotiation.get().getContractAgreementId();

        // start transfer process
        log.info("initiate transfer process for agreement {}", agreementId);
        var transferRequest = TransferProcessRequest.Builder.aNewTransferRequest()
                .transferType(offeredAsset.getDistribution().getFirst().getFormat()) // should be "HttpData-PULL"
                .contractId(agreementId)
                .counterPartyId(providerDid)
                .dataDestination(Map.of(
                        "@type", "DataAddress",
                        "type", "HttpData"))
                .build();

        var transferProcessId = redlineApi()
                .body(transferRequest)
                .post("/api/ui/service-providers/%s/tenants/%s/participants/%s/transfers".formatted(SERVICE_PROVIDER_ID, consumer.tenantId(), consumer.participantId()))
                .then()
                .statusCode(200)
                .extract().body().asString();
        assertThat(transferProcessId).isNotNull().isNotEmpty();

        // wait for transfer to reach state STARTED
        log.info("wait for transfer process '{}' to start", transferProcessId);
        var startedProcess = new AtomicReference<TransferProcess>();
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    var transferProcess = redlineApi()
                            .get("/api/ui/service-providers/%s/tenants/%s/participants/%s/transfers/%s".formatted(SERVICE_PROVIDER_ID, consumer.tenantId(), consumer.participantId(), transferProcessId))
                            .then()
                            .statusCode(200)
                            .extract().body().as(TransferProcess.class);
                    assertThat(transferProcess.getState()).isEqualTo("STARTED");
                    startedProcess.set(transferProcess);
                });

        // download the file from the provider dataplane
        log.info("downloading transferred file from consumer dataplane");
        var properties = startedProcess.get().getContentDataAddress().get("properties");
        assertThat(properties).isNotNull().isInstanceOf(Map.class);
        var authToken = ((Map<?, ?>) properties).get("https://w3id.org/edc/v0.0.1/ns/authorization").toString();
        assertThat(authToken).isNotEmpty();

        var bytes = redlineApi()
                .header("Authorization", authToken)
                .get("/api/ui/service-providers/%s/tenants/%s/participants/%s/files/%s"
                        .formatted(SERVICE_PROVIDER_ID, consumer.tenantId, consumer.participantId(), fileId))
                .then()
                .statusCode(200)
                .extract().body().asByteArray();
        assertThat(bytes).isNotEmpty().satisfies(b -> {
            var content = new String(b);
            assertThat(content).isEqualTo("This is a test file.");
        });
    }


    private ParticipantInfo deployParticipant(String consumerDid, String tenantName) {
        // register tenant
        var tenant = redlineApi()
                .body(new TenantRegistration(tenantName, List.of()))
                .post("/api/ui/service-providers/%s/tenants".formatted(SERVICE_PROVIDER_ID))
                .then()
                .statusCode(200)
                .extract().body().as(Tenant.class);

        assertThat(tenant).isNotNull();

        // deploy participant
        var participantId = tenant.participants().getFirst().id();
        var tenantId = tenant.id();
        var deployment = redlineApi()
                .body(new ParticipantDeployment(participantId, consumerDid))
                .post("/api/ui/service-providers/%s/tenants/%s/participants/%s/deployments".formatted(SERVICE_PROVIDER_ID, tenantId, participantId))
                .then()
                .statusCode(200)
                .extract().body().as(Participant.class);
        assertThat(deployment).isNotNull();

        // wait for deployment to be ACTIVE
        await().atMost(TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    var updatedParticipant = redlineApi()
                            .get("/api/ui/service-providers/%s/tenants/%s/participants/%s".formatted(SERVICE_PROVIDER_ID, tenantId, participantId))
                            .then()
                            .statusCode(200)
                            .extract().body().as(Participant.class);
                    assertThat(updatedParticipant.agents()).allMatch(agent -> agent.state().equals(DeploymentState.ACTIVE));

                });

        // register dataplane
        redlineApi()
                .post("/api/ui/service-providers/%s/tenants/%s/participants/%s/dataplanes".formatted(SERVICE_PROVIDER_ID, tenantId, participantId))
                .then()
                .statusCode(200);

        return new ParticipantInfo(participantId, tenantId, consumerDid);
    }

    private com.metaformsystems.redline.api.dto.request.Constraint mapConstraint(Constraint constraint) {
        return new com.metaformsystems.redline.api.dto.request.Constraint(constraint.getLeftOperand(), constraint.getOperator(), constraint.getRightOperand());
    }

    private record ParticipantInfo(Long participantId, Long tenantId, String did) {
    }
}
