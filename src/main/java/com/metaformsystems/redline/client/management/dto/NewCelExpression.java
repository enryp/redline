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

public class NewCelExpression {
    @JsonProperty("@context")
    private final String[] context = new String[]{
            "https://w3id.org/edc/connector/management/v2",
            "https://w3id.org/edc/virtual-connector/management/v2"
    };

    @JsonProperty("@type")
    private final String type = "CelExpression";
    @JsonProperty("@id")
    private String id;

    private String leftOperand;
    private String description;
    private String expression;
    private Set<String> scopes;

    public String getLeftOperand() {
        return leftOperand;
    }

    public void setLeftOperand(String leftOperand) {
        this.leftOperand = leftOperand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static final class Builder {
        private final NewCelExpression newCelExpression;

        private Builder() {
            newCelExpression = new NewCelExpression();
        }

        public static Builder aNewCelExpression() {
            return new Builder();
        }

        public Builder id(String id) {
            newCelExpression.setId(id);
            return this;
        }

        public Builder leftOperand(String leftOperand) {
            newCelExpression.setLeftOperand(leftOperand);
            return this;
        }

        public Builder description(String description) {
            newCelExpression.setDescription(description);
            return this;
        }

        public Builder expression(String expression) {
            newCelExpression.setExpression(expression);
            return this;
        }

        public Builder scopes(Set<String> scopes) {
            newCelExpression.setScopes(scopes);
            return this;
        }

        public NewCelExpression build() {
            return newCelExpression;
        }
    }
}

