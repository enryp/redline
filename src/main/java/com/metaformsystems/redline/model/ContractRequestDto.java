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

package com.metaformsystems.redline.model;

import java.util.List;

public class ContractRequestDto {
    private String offerId;
    private String providerId;
    private String assetId;
    private List<ConstraintDto> permissions;
    private List<ConstraintDto> prohibitions;
    private List<ConstraintDto> obligations;

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

    public List<ConstraintDto> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<ConstraintDto> permissions) {
        this.permissions = permissions;
    }

    public List<ConstraintDto> getProhibitions() {
        return prohibitions;
    }

    public void setProhibitions(List<ConstraintDto> prohibitions) {
        this.prohibitions = prohibitions;
    }

    public List<ConstraintDto> getObligations() {
        return obligations;
    }

    public void setObligations(List<ConstraintDto> obligations) {
        this.obligations = obligations;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }
}
