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

public class NewContractDefinition {
    @JsonProperty("@context")
    private final String[] context = new String[]{
            "https://w3id.org/edc/connector/management/v2",
    };

    @JsonProperty("@type")
    private final String type = "ContractDefinition";
    @JsonProperty("@id")
    private String id;

    private String accessPolicyId;
    private String contractPolicyId;
    private Set<Criterion> assetsSelector;

    public String[] getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccessPolicyId() {
        return accessPolicyId;
    }

    public void setAccessPolicyId(String accessPolicyId) {
        this.accessPolicyId = accessPolicyId;
    }

    public String getContractPolicyId() {
        return contractPolicyId;
    }

    public void setContractPolicyId(String contractPolicyId) {
        this.contractPolicyId = contractPolicyId;
    }

    public Set<Criterion> getAssetsSelector() {
        return assetsSelector;
    }

    public void setAssetsSelector(Set<Criterion> assetsSelector) {
        this.assetsSelector = assetsSelector;
    }


    public static final class Builder {
        private final NewContractDefinition newContractDefinition;

        private Builder() {
            newContractDefinition = new NewContractDefinition();
        }

        public static Builder aNewContractDefinition() {
            return new Builder();
        }

        public Builder id(String id) {
            newContractDefinition.setId(id);
            return this;
        }

        public Builder accessPolicyId(String accessPolicyId) {
            newContractDefinition.setAccessPolicyId(accessPolicyId);
            return this;
        }

        public Builder contractPolicyId(String contractPolicyId) {
            newContractDefinition.setContractPolicyId(contractPolicyId);
            return this;
        }

        public Builder assetsSelector(Set<Criterion> assetsSelector) {
            newContractDefinition.setAssetsSelector(assetsSelector);
            return this;
        }

        public NewContractDefinition build() {
            return newContractDefinition;
        }
    }
}
