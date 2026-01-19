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

package com.metaformsystems.redline.client.management.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO class for an EDC Transfer Process.
 */
public class TransferProcess {
    private String type;
    private String protocol;
    private String correlationId;
    private String counterPartyAddress;
    private Map<String, Object> dataDestination = new HashMap<>();
    private String assetId;
    private String contractId;
    private Map<String, Object> contentDataAddress;
    private Map<String, Object> privateProperties = new HashMap<>();
    private String transferType;
    private String dataPlaneId;
    private Map<String, Object> dataplaneMetadata = new HashMap<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public void setCounterPartyAddress(String counterPartyAddress) {
        this.counterPartyAddress = counterPartyAddress;
    }

    public Map<String, Object> getDataDestination() {
        return dataDestination;
    }

    public void setDataDestination(Map<String, Object> dataDestination) {
        this.dataDestination = dataDestination;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public Map<String, Object> getContentDataAddress() {
        return contentDataAddress;
    }

    public void setContentDataAddress(Map<String, Object> contentDataAddress) {
        this.contentDataAddress = contentDataAddress;
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    public void setPrivateProperties(Map<String, Object> privateProperties) {
        this.privateProperties = privateProperties;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public String getDataPlaneId() {
        return dataPlaneId;
    }

    public void setDataPlaneId(String dataPlaneId) {
        this.dataPlaneId = dataPlaneId;
    }

    public Map<String, Object> getDataplaneMetadata() {
        return dataplaneMetadata;
    }

    public void setDataplaneMetadata(Map<String, Object> dataplaneMetadata) {
        this.dataplaneMetadata = dataplaneMetadata;
    }
}
