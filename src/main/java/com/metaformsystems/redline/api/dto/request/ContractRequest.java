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

import java.util.List;

public class ContractRequest {
    private String offerId;
    private String providerId;
    private String assetId;
    private List<Constraint> permissions;
    private List<Constraint> prohibitions;
    private List<Constraint> obligations;

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public List<Constraint> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Constraint> permissions) {
        this.permissions = permissions;
    }

    public List<Constraint> getProhibitions() {
        return prohibitions;
    }

    public void setProhibitions(List<Constraint> prohibitions) {
        this.prohibitions = prohibitions;
    }

    public List<Constraint> getObligations() {
        return obligations;
    }

    public void setObligations(List<Constraint> obligations) {
        this.obligations = obligations;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public static final class Builder {
        private final ContractRequest contractRequest;

        private Builder() {
            contractRequest = new ContractRequest();
        }

        public static Builder aContractRequest() {
            return new Builder();
        }

        public Builder offerId(String offerId) {
            contractRequest.setOfferId(offerId);
            return this;
        }

        public Builder providerId(String providerId) {
            contractRequest.setProviderId(providerId);
            return this;
        }

        public Builder assetId(String assetId) {
            contractRequest.setAssetId(assetId);
            return this;
        }

        public Builder permissions(List<Constraint> permissions) {
            contractRequest.setPermissions(permissions);
            return this;
        }

        public Builder prohibitions(List<Constraint> prohibitions) {
            contractRequest.setProhibitions(prohibitions);
            return this;
        }

        public Builder obligations(List<Constraint> obligations) {
            contractRequest.setObligations(obligations);
            return this;
        }

        public ContractRequest build() {
            return contractRequest;
        }
    }
}
