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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Offer {
    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type = "Offer";

    @JsonProperty("permission")
    private List<Permission> permission;

    @JsonProperty("prohibition")
    private List<Prohibition> prohibition;

    @JsonProperty("obligation")
    private List<Obligation> obligation;
    private String target;
    private String assigner;

    public List<Prohibition> getProhibition() {
        return prohibition;
    }

    public void setProhibition(List<Prohibition> prohibition) {
        this.prohibition = prohibition;
    }

    public List<Obligation> getObligation() {
        return obligation;
    }

    public void setObligation(List<Obligation> obligation) {
        this.obligation = obligation;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getAssigner() {
        return assigner;
    }

    public void setAssigner(String assigner) {
        this.assigner = assigner;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Permission> getPermission() {
        return permission;
    }

    public void setPermission(List<Permission> permission) {
        this.permission = permission;
    }


    public static final class Builder {
        private Offer offer;

        private Builder() {
            offer = new Offer();
        }

        public static Builder anOffer() {
            return new Builder();
        }

        public Builder id(String id) {
            offer.setId(id);
            return this;
        }

        public Builder type(String type) {
            offer.setType(type);
            return this;
        }

        public Builder permission(List<Permission> permission) {
            offer.setPermission(permission);
            return this;
        }

        public Builder prohibition(List<Prohibition> prohibition) {
            offer.setProhibition(prohibition);
            return this;
        }

        public Builder obligation(List<Obligation> obligation) {
            offer.setObligation(obligation);
            return this;
        }

        public Builder target(String target) {
            offer.setTarget(target);
            return this;
        }

        public Builder assigner(String assigner) {
            offer.setAssigner(assigner);
            return this;
        }

        public Offer build() {
            return offer;
        }
    }
}
