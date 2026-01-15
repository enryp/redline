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

import com.metaformsystems.redline.client.management.dto.Catalog;
import com.metaformsystems.redline.client.management.dto.NewAsset;
import com.metaformsystems.redline.client.management.dto.NewCelExpression;
import com.metaformsystems.redline.client.management.dto.NewContractDefinition;
import com.metaformsystems.redline.client.management.dto.NewPolicyDefinition;
import com.metaformsystems.redline.client.management.dto.QuerySpec;
import com.metaformsystems.redline.dao.DataplaneRegistration;

import java.util.List;
import java.util.Map;

public interface ManagementApiClient {
    // Assets
    void createAsset(String participantContextId, NewAsset asset);

    List<Map<String, Object>> queryAssets(String participantContextId, QuerySpec query);

    void deleteAsset(String participantContextId, String assetId);

    // Policy Definitions
    void createPolicy(String participantContextId, NewPolicyDefinition policy);

    List<Map<String, Object>> queryPolicyDefinitions(String participantContextId, QuerySpec query);

    void deletePolicyDefinition(String participantContextId, String policyId);

    // Contract Definitions
    void createContractDefinition(String participantContextId, NewContractDefinition contractDefinition);

    List<Map<String, Object>> queryContractDefinitions(String participantContextId, QuerySpec query);

    void deleteContractDefinition(String participantContextId, String contractDefinitionId);

    // Contract Negotiations
    void initiateContractNegotiation(String participantContextId, Map<String, Object> negotiationRequest);

    Map<String, Object> getContractNegotiation(String participantContextId, String negotiationId);

    List<Map<String, Object>> queryContractNegotiations(String participantContextId, QuerySpec query);

    // CEL expressions
    void createCelExpression(NewCelExpression celExpression);

    // Catalog
    Catalog getCatalog(String participantContextId, String counterPartyId);

    // others
    void prepareDataplane(String participantContextId, DataplaneRegistration dataplaneRegistration);

    Object getData(String participantContextId, String counterPartyId, String offerId);

    Map<String, String> setupTransfer(String participantContextId, String policyId, String providerId);
}
