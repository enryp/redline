package com.metaformsystems.redline.client.dataplane;

import com.metaformsystems.redline.client.TokenProvider;
import com.metaformsystems.redline.client.management.dto.QuerySpec;
import com.metaformsystems.redline.model.ClientCredentials;
import com.metaformsystems.redline.model.Participant;
import com.metaformsystems.redline.repository.ParticipantRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class DataPlaneApiClientIntegrationTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private DataPlaneApiClient dataPlaneApiClient;

    @Autowired
    private ParticipantRepository participantRepository;

    @MockitoBean
    private TokenProvider tokenProvider;

    private String participantContextId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        registry.add("dataplane.url", () -> mockWebServer.url("/").toString());
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

        var participant = new Participant();
        participant.setParticipantContextId(participantContextId);
        participant.setClientCredentials(new ClientCredentials("test-client-id", "test-client-secret"));
        participantRepository.save(participant);

        // Mock token provider to return a test token
        when(tokenProvider.getToken(anyString(), anyString(), anyString())).thenReturn("test-token");
    }

    @Test
    void shouldGetAllUploads() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        [
                            {
                                "id": "upload-123",
                                "contentType": "application/pdf",
                                "properties": {
                                    "filename": "document.pdf",
                                    "size": 1024
                                }
                            },
                            {
                                "id": "upload-456",
                                "contentType": "image/png",
                                "properties": {
                                    "filename": "image.png",
                                    "size": 2048
                                }
                            }
                        ]
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        var result = dataPlaneApiClient.getAllUploads();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().id()).isEqualTo("upload-123");
        assertThat(result.get(0).contentType()).isEqualTo("application/pdf");
        assertThat(result.get(0).properties()).containsEntry("filename", "document.pdf");
        assertThat(result.get(1).id()).isEqualTo("upload-456");
        assertThat(result.get(1).contentType()).isEqualTo("image/png");

        var request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/app/internal/api/control/certs/request");
        assertThat(request.getMethod()).isEqualTo("POST");
    }

    @Test
    void shouldQueryProviderFiles() throws InterruptedException {
        // Arrange
        var querySpec = QuerySpec.Builder.aQuerySpecDto()
                .limit(10)
                .offset(0)
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        [
                            {
                                "id": "file-789",
                                "contentType": "text/plain",
                                "properties": {
                                    "filename": "readme.txt"
                                }
                            }
                        ]
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        var result = dataPlaneApiClient.queryProviderFiles(participantContextId, querySpec);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("file-789");
        assertThat(result.getFirst().contentType()).isEqualTo("text/plain");
        assertThat(result.getFirst().properties()).containsEntry("filename", "readme.txt");

        var request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/app/internal/api/control/certs/request");
        assertThat(request.getMethod()).isEqualTo("POST");
    }

    @Test
    void shouldDownloadFile() throws InterruptedException {
        // Arrange
        var fileId = "file-123";
        var expectedFileData = "This is the file content".getBytes();

        mockWebServer.enqueue(new MockResponse()
                .setBody(new String(expectedFileData))
                .addHeader("Content-Type", "application/octet-stream"));

        // Act
        var result = dataPlaneApiClient.downloadFile(participantContextId, fileId);

        // Assert
        assertThat(result).isEqualTo(expectedFileData);

        var request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/app/public/api/data/certs/" + fileId);
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    void shouldDownloadEmptyFile() throws InterruptedException {
        // Arrange
        var fileId = "empty-file";

        mockWebServer.enqueue(new MockResponse()
                .setBody("")
                .addHeader("Content-Type", "application/octet-stream"));

        // Act
        var result = dataPlaneApiClient.downloadFile(participantContextId, fileId);

        // Assert
        assertThat(result).isNullOrEmpty();

        var request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/app/public/api/data/certs/" + fileId);
    }
}
