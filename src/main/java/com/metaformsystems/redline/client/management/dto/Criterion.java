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

public final class Criterion {
    @JsonProperty("@type")
    private final String type = "Criterion";
    private String operandLeft;
    private String operator;
    private String operandRight;

    public Criterion() {
    }

    public Criterion(String operandLeft, String operator, String operandRight) {
        this.operandLeft = operandLeft;
        this.operator = operator;
        this.operandRight = operandRight;
    }

    public String getType() {
        return type;
    }

    public String getOperandLeft() {
        return operandLeft;
    }

    public void setOperandLeft(String operandLeft) {
        this.operandLeft = operandLeft;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperandRight() {
        return operandRight;
    }

    public void setOperandRight(String operandRight) {
        this.operandRight = operandRight;
    }
}
