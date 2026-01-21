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

import java.util.List;

public class Obligation {
    @JsonProperty("@type")
    private final String type = "obligation";
    @JsonProperty("action")
    private String action;
    @JsonProperty("constraint")
    private List<Constraint> constraint;

    public String getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<Constraint> getConstraint() {
        return constraint;
    }

    public void setConstraint(List<Constraint> constraint) {
        this.constraint = constraint;
    }


    public static final class Builder {
        private final Obligation obligation;

        private Builder() {
            obligation = new Obligation();
        }

        public static Builder anObligation() {
            return new Builder();
        }

        public Builder action(String action) {
            obligation.setAction(action);
            return this;
        }

        public Builder constraint(List<Constraint> constraint) {
            obligation.setConstraint(constraint);
            return this;
        }

        public Obligation build() {
            return obligation;
        }
    }
}
