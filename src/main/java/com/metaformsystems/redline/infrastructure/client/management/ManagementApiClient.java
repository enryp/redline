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

package com.metaformsystems.redline.infrastructure.client.management;

import com.metaformsystems.redline.infrastructure.client.management.dto.Asset;
import com.metaformsystems.redline.infrastructure.client.management.dto.Catalog;
import com.metaformsystems.redline.infrastructure.client.management.dto.CelExpression;
import com.metaformsystems.redline.infrastructure.client.management.dto.ContractAgreement;
import com.metaformsystems.redline.infrastructure.client.management.dto.ContractNegotiation;
import com.metaformsystems.redline.infrastructure.client.management.dto.ContractRequest;
import com.metaformsystems.redline.infrastructure.client.management.dto.DataplaneRegistration;
import com.metaformsystems.redline.infrastructure.client.management.dto.NewContractDefinition;
import com.metaformsystems.redline.infrastructure.client.management.dto.NewPolicyDefinition;
import com.metaformsystems.redline.infrastructure.client.management.dto.QuerySpec;
import com.metaformsystems.redline.infrastructure.client.management.dto.TransferProcess;
import com.metaformsystems.redline.infrastructure.client.management.dto.TransferRequest;

import java.util.List;
import java.util.Map;

public interface ManagementApiClient {
    // Assets
    void createAsset(String participantContextId, Asset asset);

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
    String initiateContractNegotiation(String participantContextId, ContractRequest negotiationRequest);

    ContractNegotiation getContractNegotiation(String participantContextId, String negotiationId);

    List<Map<String, Object>> queryContractNegotiations(String participantContextId, QuerySpec query);

    // CEL expressions
    void createCelExpression(CelExpression celExpression);

    // TransferProcess

    /**
     * @deprecated don't use this, as this is only available in JAD
     */
    @Deprecated
    Map<String, String> setupTransfer(String participantContextId, String policyId, String providerId);

    List<TransferProcess> listTransferProcesses(String participantContextId);

    String initiateTransferProcess(String participantContextId, TransferRequest request);

    TransferProcess getTransferProcess(String participantContextId, String transferProcessId);

    // Catalog
    Catalog getCatalog(String participantContextId, String counterPartyId);

    // others
    void prepareDataplane(String participantContextId, DataplaneRegistration dataplaneRegistration);

    Object getData(String participantContextId, String counterPartyId, String offerId);


    List<ContractNegotiation> listContracts(String participantContextId);

    ContractAgreement getAgreement(String participantContextId, String negotiationId);

    Map<String, Object> getEdr(String participantContextId, String transferProcessId);
}
