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

import java.util.Map;

public class NewAsset {
    @JsonProperty("@context")
    private final String[] context = new String[]{
            "https://w3id.org/edc/connector/management/v2",
    };

    @JsonProperty("@type")
    private final String type = "Asset";
    @JsonProperty("@id")
    private String id;

    private Map<String, Object> properties;
    private Map<String, Object> privateProperties;
    //todo: replace with proper object
    private Map<String, Object> dataAddress;

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

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    public void setPrivateProperties(Map<String, Object> privateProperties) {
        this.privateProperties = privateProperties;
    }

    public Map<String, Object> getDataAddress() {
        return dataAddress;
    }

    public void setDataAddress(Map<String, Object> dataAddress) {
        this.dataAddress = dataAddress;
    }


    public static final class Builder {
        private final NewAsset newAsset;

        private Builder() {
            newAsset = new NewAsset();
        }

        public static Builder aNewAsset() {
            return new Builder();
        }

        public Builder id(String id) {
            newAsset.setId(id);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            newAsset.setProperties(properties);
            return this;
        }

        public Builder privateProperties(Map<String, Object> privateProperties) {
            newAsset.setPrivateProperties(privateProperties);
            return this;
        }

        public Builder dataAddress(Map<String, Object> dataAddress) {
            newAsset.setDataAddress(dataAddress);
            return this;
        }

        public NewAsset build() {
            return newAsset;
        }
    }
}
