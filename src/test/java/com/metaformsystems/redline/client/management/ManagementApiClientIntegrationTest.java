package com.metaformsystems.redline.client.management;

import com.metaformsystems.redline.client.TokenProvider;
import com.metaformsystems.redline.client.management.dto.Criterion;
import com.metaformsystems.redline.client.management.dto.NewAsset;
import com.metaformsystems.redline.client.management.dto.NewCelExpression;
import com.metaformsystems.redline.client.management.dto.NewContractDefinition;
import com.metaformsystems.redline.client.management.dto.NewPolicyDefinition;
import com.metaformsystems.redline.client.management.dto.QuerySpec;
import com.metaformsystems.redline.dao.DataplaneRegistration;
import com.metaformsystems.redline.model.ClientCredentials;
import com.metaformsystems.redline.model.Participant;
import com.metaformsystems.redline.repository.ParticipantRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ManagementApiClientIntegrationTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private ManagementApiClient managementApiClient;

    @Autowired
    private ParticipantRepository participantRepository;

    @MockitoBean
    private TokenProvider tokenProvider;

    private Participant participant;
    private String participantContextId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        registry.add("controlplane.url", () -> mockWebServer.url("/").toString());
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        participantContextId = "test-participant-context-id";

        participant = new Participant();
        participant.setParticipantContextId(participantContextId);
        participant.setClientCredentials(new ClientCredentials("test-client-id", "test-client-secret"));
        participant = participantRepository.save(participant);

        // Mock token provider to return a test token
        when(tokenProvider.getToken(anyString(), anyString(), anyString())).thenReturn("test-token");
    }

    @Test
    void shouldCreateAsset() throws InterruptedException {
        // Arrange
        var asset = NewAsset.Builder.aNewAsset()
                .id("asset-123")
                .properties(Map.of("name", "Test Asset", "contenttype", "application/json"))
                .build();

        // Mock asset creation response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204)
                .addHeader("Content-Type", "application/json"));

        // Act
        managementApiClient.createAsset(participantContextId, asset);

        // Assert
        RecordedRequest assetRequest = mockWebServer.takeRequest();
        assertThat(assetRequest.getPath()).contains("/participants/" + participantContextId + "/assets");
        assertThat(assetRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(assetRequest.getBody().readUtf8()).contains("asset-123");
    }

    @Test
    void shouldQueryAssets() throws InterruptedException {
        // Arrange
        var query = QuerySpec.Builder.aQuerySpecDto()
                .filterExpression(List.of(new Criterion("id", "=", "asset-123")))
                .build();

        // Mock query response
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        [
                            {
                                "@id": "asset-123",
                                "@type": "Asset",
                                "properties": {
                                    "name": "Test Asset"
                                }
                            }
                        ]
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        var result = managementApiClient.queryAssets(participantContextId, query);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsEntry("@id", "asset-123");

        RecordedRequest queryRequest = mockWebServer.takeRequest();
        assertThat(queryRequest.getPath()).contains("/assets/request");
        assertThat(queryRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }

    @Test
    void shouldDeleteAsset() throws InterruptedException {
        // Arrange
        String assetId = "asset-123";


        // Mock delete response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));

        // Act
        managementApiClient.deleteAsset(participantContextId, assetId);

        // Assert

        RecordedRequest deleteRequest = mockWebServer.takeRequest();
        assertThat(deleteRequest.getPath()).contains("/assets/" + assetId);
        assertThat(deleteRequest.getMethod()).isEqualTo("DELETE");
        assertThat(deleteRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }

    @Test
    void shouldCreatePolicy() throws InterruptedException {
        // Arrange
        var policy = NewPolicyDefinition.Builder.aNewPolicyDefinition()
                .id("policy-123")
                .policy(new NewPolicyDefinition.PolicySet(List.of(new NewPolicyDefinition.PolicySet.Permission("use", List.of(new NewPolicyDefinition.PolicySet.Constraint("foo", "=", "bar"))))))
                .build();

        // Mock policy creation response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204)
                .addHeader("Content-Type", "application/json"));

        // Act
        managementApiClient.createPolicy(participantContextId, policy);

        // Assert

        RecordedRequest policyRequest = mockWebServer.takeRequest();
        assertThat(policyRequest.getPath()).contains("/participants/" + participantContextId + "/policydefinitions");
        assertThat(policyRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(policyRequest.getBody().readUtf8()).contains("policy-123");
    }

    @Test
    void shouldQueryPolicyDefinitions() throws InterruptedException {
        // Arrange
        var query = QuerySpec.Builder.aQuerySpecDto().build();

        // Mock query response
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        [
                            {
                                "@id": "policy-123",
                                "@type": "PolicyDefinition"
                            }
                        ]
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        var result = managementApiClient.queryPolicyDefinitions(participantContextId, query);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsEntry("@id", "policy-123");


        RecordedRequest queryRequest = mockWebServer.takeRequest();
        assertThat(queryRequest.getPath()).contains("/policydefinitions/request");
    }

    @Test
    void shouldDeletePolicyDefinition() throws InterruptedException {
        // Arrange
        String policyId = "policy-123";

        // Mock delete response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));

        // Act
        managementApiClient.deletePolicyDefinition(participantContextId, policyId);

        // Assert
        RecordedRequest deleteRequest = mockWebServer.takeRequest();
        assertThat(deleteRequest.getPath()).contains("/policydefinitions/" + policyId);
        assertThat(deleteRequest.getMethod()).isEqualTo("DELETE");
    }

    @Test
    void shouldCreateContractDefinition() throws InterruptedException {
        // Arrange
        var contractDef = NewContractDefinition.Builder.aNewContractDefinition()
                .id("contract-123")
                .accessPolicyId("policy-123")
                .contractPolicyId("policy-456")
                .assetsSelector(Set.of(new Criterion("id", "=", "asset-123")))
                .build();

        // Mock contract definition creation response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204)
                .addHeader("Content-Type", "application/json"));

        // Act
        managementApiClient.createContractDefinition(participantContextId, contractDef);

        // Assert
        RecordedRequest contractRequest = mockWebServer.takeRequest();
        assertThat(contractRequest.getPath()).contains("/participants/" + participantContextId + "/contractdefinitions");
        assertThat(contractRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(contractRequest.getBody().readUtf8()).contains("contract-123");
    }

    @Test
    void shouldQueryContractDefinitions() throws InterruptedException {
        // Arrange
        var query = QuerySpec.Builder.aQuerySpecDto().build();

        // Mock query response
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        [
                            {
                                "@id": "contract-123",
                                "@type": "ContractDefinition"
                            }
                        ]
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        var result = managementApiClient.queryContractDefinitions(participantContextId, query);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsEntry("@id", "contract-123");


        RecordedRequest queryRequest = mockWebServer.takeRequest();
        assertThat(queryRequest.getPath()).contains("/contractdefinitions/request");
    }

    @Test
    void shouldDeleteContractDefinition() throws InterruptedException {
        // Arrange
        String contractId = "contract-123";


        // Mock delete response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));

        // Act
        managementApiClient.deleteContractDefinition(participantContextId, contractId);

        // Assert

        RecordedRequest deleteRequest = mockWebServer.takeRequest();
        assertThat(deleteRequest.getPath()).contains("/contractdefinitions/" + contractId);
        assertThat(deleteRequest.getMethod()).isEqualTo("DELETE");
    }

    @Test
    void shouldInitiateContractNegotiation() throws InterruptedException {
        // Arrange
        Map<String, Object> negotiationRequest = new HashMap<>();
        negotiationRequest.put("@type", "ContractRequest");
        negotiationRequest.put("counterPartyAddress", "http://provider.com/dsp");
        negotiationRequest.put("protocol", "dataspace-protocol-http");


        // Mock negotiation initiation response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204)
                .addHeader("Content-Type", "application/json"));

        // Act
        managementApiClient.initiateContractNegotiation(participantContextId, negotiationRequest);

        // Assert

        RecordedRequest negotiationRequestRecorded = mockWebServer.takeRequest();
        assertThat(negotiationRequestRecorded.getPath()).contains("/contractnegotiations");
        assertThat(negotiationRequestRecorded.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(negotiationRequestRecorded.getBody().readUtf8()).contains("ContractRequest");
    }

    @Test
    void shouldGetContractNegotiation() throws InterruptedException {
        // Arrange
        String negotiationId = "negotiation-123";

        // Mock get negotiation response
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                            "@id": "negotiation-123",
                            "@type": "ContractNegotiation",
                            "state": "FINALIZED"
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        var result = managementApiClient.getContractNegotiation(participantContextId, negotiationId);

        // Assert
        assertThat(result).containsEntry("@id", "negotiation-123");
        assertThat(result).containsEntry("state", "FINALIZED");


        RecordedRequest getRequest = mockWebServer.takeRequest();
        assertThat(getRequest.getPath()).contains("/contractnegotiations/" + negotiationId);
        assertThat(getRequest.getMethod()).isEqualTo("GET");
    }

    @Test
    void shouldQueryContractNegotiations() throws InterruptedException {
        // Arrange
        var query = QuerySpec.Builder.aQuerySpecDto().build();

        // Mock query response
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        [
                            {
                                "@id": "negotiation-123",
                                "@type": "ContractNegotiation",
                                "state": "FINALIZED"
                            }
                        ]
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        var result = managementApiClient.queryContractNegotiations(participantContextId, query);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsEntry("@id", "negotiation-123");

        RecordedRequest queryRequest = mockWebServer.takeRequest();
        assertThat(queryRequest.getPath()).contains("/contractnegotiations/request");
    }

    @Test
    void shouldCreateCelExpression() throws InterruptedException {
        // Arrange
        var celExpression = NewCelExpression.Builder.aNewCelExpression()
                .id("cel-123")
                .expression("expression == 'test'")
                .leftOperand("test-left-operand")
                .description("Test expression")
                .scopes(Set.of("scope1", "scope2"))
                .build();

        // Mock CEL expression creation response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204)
                .addHeader("Content-Type", "application/json"));

        // Act
        managementApiClient.createCelExpression(celExpression);

        // Assert
        RecordedRequest celRequest = mockWebServer.takeRequest();
        assertThat(celRequest.getPath()).isEqualTo("/v4alpha/celexpressions");
        assertThat(celRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(celRequest.getBody().readUtf8()).contains("cel-123");
    }

    @Test
    void shouldPrepareDataplane() throws InterruptedException {
        // Arrange
        var dataplaneRegistration = DataplaneRegistration.Builder.aDataplaneRegistration()
                .url("http://localhost:8080")
                .allowedSourceTypes(List.of("HttpData"))
                .allowedTransferTypes(List.of("HttpData-PULL"))
                .build();

        // Mock dataplane registration response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204)
                .addHeader("Content-Type", "application/json"));

        // Act
        managementApiClient.prepareDataplane(participantContextId, dataplaneRegistration);

        // Assert
        RecordedRequest dataplaneRequest = mockWebServer.takeRequest();
        assertThat(dataplaneRequest.getPath()).isEqualTo("/v4alpha/dataplanes/" + participantContextId);
        assertThat(dataplaneRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }
}
