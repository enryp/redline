package com.metaformsystems.redline.client.tenantmanager.v1alpha1;

import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.ModelQuery;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1Cell;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1DataspaceProfile;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1NewCell;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1NewTenant;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1ParticipantProfile;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1Tenant;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1TenantPropertiesDiff;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class TenantManagerClientImpl implements TenantManagerClient {

    private static final String API_BASE = "/api/v1alpha1";
    private final WebClient webClient;

    public TenantManagerClientImpl(WebClient tenantManagerWebClient) {
        this.webClient = tenantManagerWebClient;
    }

    @Override
    public List<V1Alpha1Cell> listCells() {
        return webClient.get()
                .uri(API_BASE + "/cells")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<V1Alpha1Cell>>() {
                })
                .block();
    }

    @Override
    public V1Alpha1Cell createCell(V1Alpha1NewCell newCell) {
        return webClient.post()
                .uri(API_BASE + "/cells")
                .bodyValue(newCell)
                .retrieve()
                .bodyToMono(V1Alpha1Cell.class)
                .block();
    }

    @Override
    public List<V1Alpha1DataspaceProfile> listDataspaceProfiles() {
        return webClient.get()
                .uri(API_BASE + "/dataspace-profiles")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<V1Alpha1DataspaceProfile>>() {
                })
                .block();
    }

    @Override
    public V1Alpha1DataspaceProfile getDataspaceProfile(String id) {
        return webClient.get()
                .uri(API_BASE + "/dataspace-profiles/{id}", id)
                .retrieve()
                .bodyToMono(V1Alpha1DataspaceProfile.class)
                .block();
    }

    @Override
    public void deployDataspaceProfile(String id) {
        webClient.post()
                .uri(API_BASE + "/dataspace-profiles/{id}/deployments", id)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public List<V1Alpha1ParticipantProfile> queryParticipantProfiles(ModelQuery query) {
        return webClient.post()
                .uri(API_BASE + "/participant-profiles/query")
                .bodyValue(query)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<V1Alpha1ParticipantProfile>>() {
                })
                .block();
    }

    @Override
    public List<V1Alpha1ParticipantProfile> listParticipantProfiles(String tenantId) {
        return webClient.get()
                .uri(API_BASE + "/tenants/{id}/participant-profiles", tenantId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<V1Alpha1ParticipantProfile>>() {
                })
                .block();
    }

    @Override
    public V1Alpha1ParticipantProfile getParticipantProfile(String tenantId, String participantId) {
        return webClient.get()
                .uri(API_BASE + "/tenants/{id}/participant-profiles/{participantID}", tenantId, participantId)
                .retrieve()
                .bodyToMono(V1Alpha1ParticipantProfile.class)
                .block();
    }

    @Override
    public V1Alpha1ParticipantProfile deployParticipantProfile(String tenantId, V1Alpha1ParticipantProfile profile) {
        return webClient.post()
                .uri(API_BASE + "/tenants/{id}/participant-profiles", tenantId)
                .bodyValue(profile)
                .retrieve()
                .bodyToMono(V1Alpha1ParticipantProfile.class)
                .block();
    }

    @Override
    public V1Alpha1ParticipantProfile deleteParticipantProfile(String tenantId, String participantId) {
        return webClient.delete()
                .uri(API_BASE + "/tenants/{id}/participant-profiles/{participantID}", tenantId, participantId)
                .retrieve()
                .bodyToMono(V1Alpha1ParticipantProfile.class)
                .block();
    }

    @Override
    public List<V1Alpha1Tenant> listTenants() {
        return webClient.get()
                .uri(API_BASE + "/tenants")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<V1Alpha1Tenant>>() {
                })
                .block();
    }

    @Override
    public V1Alpha1Tenant getTenant(String id) {
        return webClient.get()
                .uri(API_BASE + "/tenants/{id}", id)
                .retrieve()
                .bodyToMono(V1Alpha1Tenant.class)
                .block();
    }

    @Override
    public V1Alpha1Tenant createTenant(V1Alpha1NewTenant newTenant) {
        return webClient.post()
                .uri(API_BASE + "/tenants")
                .bodyValue(newTenant)
                .retrieve()
                .bodyToMono(V1Alpha1Tenant.class)
                .block();
    }

    @Override
    public V1Alpha1Tenant updateTenant(String id, V1Alpha1TenantPropertiesDiff diff) {
        return webClient.patch()
                .uri(API_BASE + "/tenants/{id}", id)
                .bodyValue(diff)
                .retrieve()
                .bodyToMono(V1Alpha1Tenant.class)
                .block();
    }

    @Override
    public List<V1Alpha1Tenant> queryTenants(ModelQuery query) {
        return webClient.post()
                .uri(API_BASE + "/tenants/query")
                .bodyValue(query)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<V1Alpha1Tenant>>() {
                })
                .block();
    }
}
