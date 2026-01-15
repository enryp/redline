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

package com.metaformsystems.redline.client.management;

import com.metaformsystems.redline.client.TokenProvider;
import com.metaformsystems.redline.client.management.dto.Catalog;
import com.metaformsystems.redline.client.management.dto.NewAsset;
import com.metaformsystems.redline.client.management.dto.NewCelExpression;
import com.metaformsystems.redline.client.management.dto.NewContractDefinition;
import com.metaformsystems.redline.client.management.dto.NewPolicyDefinition;
import com.metaformsystems.redline.client.management.dto.QuerySpec;
import com.metaformsystems.redline.dao.DataplaneRegistration;
import com.metaformsystems.redline.model.ClientCredentials;
import com.metaformsystems.redline.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static com.nimbusds.jose.util.Base64URL.encode;

@Component
public class ManagementApiClientImpl implements ManagementApiClient {

    private final WebClient controlPlaneWebClient;
    private final TokenProvider tokenProvider;
    private final ParticipantRepository participantRepository;
    private final ClientCredentials adminCredentials;

    public ManagementApiClientImpl(WebClient controlPlaneWebClient,
                                   TokenProvider tokenProvider,
                                   ParticipantRepository participantRepository,
                                   @Value("${controlplane.admin.client-id:admin}") String adminClientId,
                                   @Value("${controlplane.admin.client-secret:edc-v-admin-secret}") String adminClientSecret) {
        this.controlPlaneWebClient = controlPlaneWebClient;
        this.tokenProvider = tokenProvider;
        this.participantRepository = participantRepository;
        this.adminCredentials = new ClientCredentials(adminClientId, adminClientSecret);
    }

    @Override
    public void createAsset(String participantContextId, NewAsset asset) {
        var token = getToken(participantContextId);

        controlPlaneWebClient.post()
                .uri("/v4alpha/participants/%s/assets".formatted(participantContextId))
                .header("Authorization", "Bearer %s".formatted(token))
                .bodyValue(asset)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public List<Map<String, Object>> queryAssets(String participantContextId, QuerySpec query) {
        return controlPlaneWebClient.post()
                .uri("/v4alpha/participants/{participantContextId}/assets/request", encode(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(query)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .block();
    }

    @Override
    public void deleteAsset(String participantContextId, String assetId) {
        controlPlaneWebClient.delete()
                .uri("/v4alpha/participants/{participantContextId}/assets/{assetId}", encode(participantContextId), assetId)
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public void createPolicy(String participantContextId, NewPolicyDefinition policy) {
        controlPlaneWebClient.post()
                .uri("/v4alpha/participants/%s/policydefinitions".formatted(participantContextId))
                .header("Authorization", "Bearer %s".formatted(getToken(participantContextId)))
                .bodyValue(policy)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public List<Map<String, Object>> queryPolicyDefinitions(String participantContextId, QuerySpec query) {
        return controlPlaneWebClient.post()
                .uri("/v4alpha/participants/{participantContextId}/policydefinitions/request", encode(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(query)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .block();
    }

    @Override
    public void deletePolicyDefinition(String participantContextId, String policyId) {
        controlPlaneWebClient.delete()
                .uri("/v4alpha/participants/{participantContextId}/policydefinitions/{policyId}", encode(participantContextId), policyId)
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void createContractDefinition(String participantContextId, NewContractDefinition contractDefinition) {
        controlPlaneWebClient.post()
                .uri("/v4alpha/participants/%s/contractdefinitions".formatted(participantContextId))
                .header("Authorization", "Bearer %s".formatted(getToken(participantContextId)))
                .bodyValue(contractDefinition)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public List<Map<String, Object>> queryContractDefinitions(String participantContextId, QuerySpec query) {
        return controlPlaneWebClient.post()
                .uri("/v4alpha/participants/{participantContextId}/contractdefinitions/request", encode(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(query)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .block();
    }

    @Override
    public void deleteContractDefinition(String participantContextId, String contractDefinitionId) {
        controlPlaneWebClient.delete()
                .uri("/v4alpha/participants/{participantContextId}/contractdefinitions/{id}", encode(participantContextId), contractDefinitionId)
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public void initiateContractNegotiation(String participantContextId, Map<String, Object> negotiationRequest) {
        controlPlaneWebClient.post()
                .uri("/v4alpha/participants/{participantContextId}/contractnegotiations", encode(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(negotiationRequest)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public Map<String, Object> getContractNegotiation(String participantContextId, String negotiationId) {
        return controlPlaneWebClient.get()
                .uri("/v4alpha/participants/{participantContextId}/contractnegotiations/{id}", encode(participantContextId), negotiationId)
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();
    }

    @Override
    public List<Map<String, Object>> queryContractNegotiations(String participantContextId, QuerySpec query) {
        return controlPlaneWebClient.post()
                .uri("/v4alpha/participants/{participantContextId}/contractnegotiations/request", encode(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(query)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .block();
    }

    @Override
    public void createCelExpression(NewCelExpression celExpression) {

        var token = tokenProvider.getToken(adminCredentials.clientId(), adminCredentials.clientSecret(), "management-api:write management-api:read");
        controlPlaneWebClient.post()
                .uri("/v4alpha/celexpressions")
                .header("Authorization", "Bearer %s".formatted(token))
                .bodyValue(celExpression)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public Catalog getCatalog(String participantContextId, String counterPartyDid) {
        return controlPlaneWebClient.post()
                .uri("/v1alpha/participants/%s/catalog".formatted(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(Map.of("counterPartyDid", counterPartyDid))
                .retrieve()
                .bodyToMono(Catalog.class)
                .block();
    }

    @Override
    public void prepareDataplane(String participantContextId, DataplaneRegistration dataplaneRegistration) {
        controlPlaneWebClient.post()
                .uri("/v4alpha/dataplanes/%s".formatted(participantContextId))
                .header("Authorization", "Bearer %s".formatted(getToken(participantContextId)))
                .bodyValue(dataplaneRegistration)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public Object getData(String participantContextId, String counterPartyId, String policyId) {
        return controlPlaneWebClient.post()
                .uri("/v1alpha/participants/%s/data".formatted(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(Map.of(
                        "providerId", counterPartyId,
                        "policyId", policyId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                })
                .block();
    }

    @Override
    public Map<String, String> setupTransfer(String participantContextId, String policyId, String providerId) {
        return controlPlaneWebClient.post()
                .uri("/v1alpha/participants/%s/transfer".formatted(participantContextId))
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .bodyValue(Map.of(
                        "policyId", policyId,
                        "providerId", providerId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
                })
                .block();
    }


    private String getToken(String participantContextId) {
        var participantProfile = participantRepository.findByParticipantContextId(participantContextId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with context id: " + participantContextId));

        return tokenProvider.getToken(participantProfile.getClientCredentials().clientId(), participantProfile.getClientCredentials().clientSecret(), "management-api:write management-api:read");
    }

}
