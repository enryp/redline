package com.metaformsystems.redline.client.tenantmanager.v1alpha1;

import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.ModelQuery;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1Cell;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1DataspaceProfile;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1NewCell;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1NewTenant;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1ParticipantProfile;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1Tenant;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1TenantPropertiesDiff;

import java.util.List;

/**
 * HTTP client for the Tenant Manager API
 */
public interface TenantManagerClient {

    // Cell operations
    List<V1Alpha1Cell> listCells();

    V1Alpha1Cell createCell(V1Alpha1NewCell newCell);

    // Dataspace Profile operations
    List<V1Alpha1DataspaceProfile> listDataspaceProfiles();

    V1Alpha1DataspaceProfile getDataspaceProfile(String id);

    void deployDataspaceProfile(String id);

    // Participant Profile operations
    List<V1Alpha1ParticipantProfile> queryParticipantProfiles(ModelQuery query);

    List<V1Alpha1ParticipantProfile> listParticipantProfiles(String tenantId);

    V1Alpha1ParticipantProfile getParticipantProfile(String tenantId, String participantId);

    V1Alpha1ParticipantProfile deployParticipantProfile(String tenantId, V1Alpha1ParticipantProfile profile);

    V1Alpha1ParticipantProfile deleteParticipantProfile(String tenantId, String participantId);

    // Tenant operations
    List<V1Alpha1Tenant> listTenants();

    V1Alpha1Tenant getTenant(String id);

    V1Alpha1Tenant createTenant(V1Alpha1NewTenant newTenant);

    V1Alpha1Tenant updateTenant(String id, V1Alpha1TenantPropertiesDiff diff);

    List<V1Alpha1Tenant> queryTenants(ModelQuery query);
}
