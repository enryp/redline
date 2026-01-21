package com.metaformsystems.redline.client.identityhub;

import com.metaformsystems.redline.client.TokenProvider;
import com.metaformsystems.redline.client.identityhub.dto.CredentialRequestDto;
import com.metaformsystems.redline.client.identityhub.dto.DidRequestPayload;
import com.metaformsystems.redline.client.identityhub.dto.IdentityHubParticipantContext;
import com.metaformsystems.redline.client.identityhub.dto.KeyDescriptor;
import com.metaformsystems.redline.client.identityhub.dto.KeyPairResource;
import com.metaformsystems.redline.client.identityhub.dto.VerifiableCredentialResource;
import com.metaformsystems.redline.repository.ParticipantRepository;
import com.metaformsystems.redline.service.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.nimbusds.jose.util.Base64URL.encode;

@Component
public class IdentityHubClientImpl implements IdentityHubClient {

    private static final String IDENTITY_API_BASE = "/api/identity/v1alpha";
    private final WebClient webClient;
    private final TokenProvider tokenProvider;
    private final String adminClientId;
    private final String adminClientSecret;
    private final ParticipantRepository participantRepository;

    public IdentityHubClientImpl(WebClient identityHubWebClient,
                                 TokenProvider tokenProvider,
                                 ParticipantRepository participantRepository,
                                 @Value("${controlplane.admin.client-id:admin}") String adminClientId,
                                 @Value("${controlplane.admin.client-secret:edc-v-admin-secret}") String adminClientSecret) {
        this.webClient = identityHubWebClient;
        this.tokenProvider = tokenProvider;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
        this.participantRepository = participantRepository;
    }

    @Override
    public List<IdentityHubParticipantContext> getAllParticipants() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(IDENTITY_API_BASE + "/participants")
                        .build())
                .header("Authorization", "Bearer " + getToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<IdentityHubParticipantContext>>() {
                })
                .block();
    }

    @Override
    public IdentityHubParticipantContext getParticipant(String participantContextId) {
        return webClient.get()
                .uri(IDENTITY_API_BASE + "/participants/{participantContextId}", encode(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .retrieve()
                .bodyToMono(IdentityHubParticipantContext.class)
                .block();
    }

    @Override
    public List<VerifiableCredentialResource> getAllCredentials() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(IDENTITY_API_BASE + "/credentials")
                        .build())
                .header("Authorization", "Bearer " + getToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<VerifiableCredentialResource>>() {
                })
                .block();
    }

    @Override
    public List<VerifiableCredentialResource> queryCredentialsByType(String participantContextId, String type) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(IDENTITY_API_BASE + "/participants/%s/credentials".formatted(encode(participantContextId)));
                    if (type != null) {
                        builder.queryParam("type", type);
                    }
                    return builder.build();
                })
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<VerifiableCredentialResource>>() {
                })
                .block();
    }

    @Override
    public VerifiableCredentialResource getCredentialRequest(String participantContextId, String holderPid) {
        return webClient.get()
                .uri(IDENTITY_API_BASE + "/participants/{participantContextId}/credentials/request/{holderPid}",
                        encode(participantContextId), holderPid)
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .retrieve()
                .bodyToMono(VerifiableCredentialResource.class)
                .block();
    }

    @Override
    public void requestCredential(String participantContextId, CredentialRequestDto request) {
        webClient.post()
                .uri(IDENTITY_API_BASE + "/participants/{participantContextId}/credentials/request", encode(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public List<KeyPairResource> getAllKeyPairs() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(IDENTITY_API_BASE + "/keypairs")
                        .build())
                .header("Authorization", "Bearer " + getToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<KeyPairResource>>() {
                })
                .block();
    }

    @Override
    public List<KeyPairResource> queryKeyPairByParticipantContextId(String participantContextId) {
        return webClient.get()
                .uri(IDENTITY_API_BASE + "/participants/{participantContextId}/keypairs", encode(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<KeyPairResource>>() {
                })
                .block();
    }

    @Override
    public KeyPairResource getKeyPair(String participantContextId, String keyPairId) {
        return webClient.get()
                .uri(IDENTITY_API_BASE + "/participants/{participantContextId}/keypairs/{keyPairId}",
                        encode(participantContextId), keyPairId)
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .retrieve()
                .bodyToMono(KeyPairResource.class)
                .block();
    }

    @Override
    public void addKeyPair(String participantContextId, KeyDescriptor keyDescriptor, Boolean makeDefault) {
        webClient.put()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(IDENTITY_API_BASE + "/participants/%s/keypairs".formatted(encode(participantContextId)));
                    if (makeDefault != null) {
                        builder.queryParam("makeDefault", makeDefault);
                    }
                    return builder.build();
                })
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(keyDescriptor)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public void rotateKeyPair(String participantContextId, String keyPairId, KeyDescriptor keyDescriptor, Long duration) {
        webClient.post()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(IDENTITY_API_BASE + "/participants/%s/keypairs/%s/rotate".formatted(encode(participantContextId), keyPairId));
                    if (duration != null) {
                        builder.queryParam("duration", duration);
                    }
                    return builder.build();
                })
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(keyDescriptor)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public void revokeKeyPair(String participantContextId, String keyPairId, KeyDescriptor keyDescriptor) {
        webClient.post()
                .uri(IDENTITY_API_BASE + "/participants/{participantContextId}/keypairs/{keyPairId}/revoke",
                        encode(participantContextId), keyPairId)
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(keyDescriptor)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public void getDidState(String participantContextId, DidRequestPayload payload) {
        webClient.post()
                .uri(IDENTITY_API_BASE + "/participants/{participantContextId}/dids/state", encode(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String getToken() {
        return tokenProvider.getToken(adminClientId, adminClientSecret, "identity-api:read");
    }

    private String getToken(String participantContextId) {
        var participantProfile = participantRepository.findByParticipantContextId(participantContextId)
                .orElseThrow(() -> new ObjectNotFoundException("Participant not found with context id: " + participantContextId));

        var token = tokenProvider.getToken(participantProfile.getClientCredentials().clientId(), participantProfile.getClientCredentials().clientSecret(), "identity-api:write identity-api:read");
        return token;
    }
}
