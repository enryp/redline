package com.metaformsystems.redline.dao;

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
