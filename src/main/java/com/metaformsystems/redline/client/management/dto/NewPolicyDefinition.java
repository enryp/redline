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

public class NewPolicyDefinition {
    @JsonProperty("@context")
    private final String[] context = new String[]{
            "https://w3id.org/edc/connector/management/v2",
    };

    @JsonProperty("@type")
    private final String type = "PolicyDefinition";
    @JsonProperty("@id")
    private String id;

    private PolicySet policy;

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

    public PolicySet getPolicy() {
        return policy;
    }

    public void setPolicy(PolicySet policy) {
        this.policy = policy;
    }

    public static final class Builder {
        private final NewPolicyDefinition newPolicyDefinition;

        private Builder() {
            newPolicyDefinition = new NewPolicyDefinition();
        }

        public static Builder aNewPolicyDefinition() {
            return new Builder();
        }

        public Builder id(String id) {
            newPolicyDefinition.setId(id);
            return this;
        }

        public Builder policy(PolicySet policy) {
            newPolicyDefinition.setPolicy(policy);
            return this;
        }

        public NewPolicyDefinition build() {
            return newPolicyDefinition;
        }
    }
}
