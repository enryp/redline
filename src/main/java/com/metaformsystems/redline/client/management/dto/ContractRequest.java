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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class ContractRequest {

    @JsonProperty("@context")
    private final String[] context = new String[]{
            "https://w3id.org/edc/connector/management/v2",
    };
    @JsonProperty("@type")
    private final String type = "ContractRequest";
    private String protocol = "dataspace-protocol-http:2025-1";
    private String counterPartyAddress;
    private String providerId;
    private Set<String> callbackAddresses;
    private Offer policy;

    public String[] getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public void setCounterPartyAddress(String counterPartyAddress) {
        this.counterPartyAddress = counterPartyAddress;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Set<String> getCallbackAddresses() {
        return callbackAddresses;
    }

    public void setCallbackAddresses(Set<String> callbackAddresses) {
        this.callbackAddresses = callbackAddresses;
    }

    public Offer getPolicy() {
        return policy;
    }

    public void setPolicy(Offer policy) {
        this.policy = policy;
    }


    public static final class Builder {
        private final ContractRequest contractRequest;

        private Builder() {
            contractRequest = new ContractRequest();
        }

        public static Builder aContractRequest() {
            return new Builder();
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            contractRequest.setCounterPartyAddress(counterPartyAddress);
            return this;
        }

        public Builder protocol(String protocol) {
            contractRequest.setProtocol(protocol);
            return this;
        }

        public Builder providerId(String providerId) {
            contractRequest.setProviderId(providerId);
            return this;
        }

        public Builder callbackAddresses(Set<String> callbackAddresses) {
            contractRequest.setCallbackAddresses(callbackAddresses);
            return this;
        }

        public Builder policy(Offer policy) {
            contractRequest.setPolicy(policy);
            return this;
        }

        public ContractRequest build() {
            return contractRequest;
        }
    }
}