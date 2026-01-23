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

package com.metaformsystems.redline.api.dto.request;

import java.util.Map;

public class TransferProcessRequest {
    /**
     * Web DID of the counter party
     */
    private String counterPartyId;
    private String contractId;
    private Map<String, Object> dataDestination;
    private String transferType;

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public void setCounterPartyId(String counterPartyId) {
        this.counterPartyId = counterPartyId;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public Map<String, Object> getDataDestination() {
        return dataDestination;
    }

    public void setDataDestination(Map<String, Object> dataDestination) {
        this.dataDestination = dataDestination;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public static final class Builder {
        private final TransferProcessRequest transferProcessRequest;

        private Builder() {
            transferProcessRequest = new TransferProcessRequest();
        }

        public static Builder aNewTransferRequest() {
            return new Builder();
        }

        public Builder counterPartyId(String counterPartyId) {
            transferProcessRequest.setCounterPartyId(counterPartyId);
            return this;
        }

        public Builder contractId(String contractId) {
            transferProcessRequest.setContractId(contractId);
            return this;
        }

        public Builder dataDestination(Map<String, Object> dataDestination) {
            transferProcessRequest.setDataDestination(dataDestination);
            return this;
        }

        public Builder transferType(String transferType) {
            transferProcessRequest.setTransferType(transferType);
            return this;
        }

        public TransferProcessRequest build() {
            return transferProcessRequest;
        }
    }
}
