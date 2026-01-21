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

public class PolicySet {

    @JsonProperty("@type")
    private final String type = "Set";
    private List<Permission> permission;

    public PolicySet(List<Permission> permission) {
        this.permission = permission;
    }

    public List<Permission> getPermission() {
        return permission;
    }

    public void setPermission(List<Permission> permission) {
        this.permission = permission;
    }

    public static class Permission {
        private String action;
        private List<Constraint> constraint;

        public Permission(String action, List<Constraint> constraint) {
            this.action = action;
            this.constraint = constraint;
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
    }

    public static class Constraint {
        private String leftOperand;
        private String operator;
        private String rightOperand;

        public Constraint(String leftOperand, String operator, String rightOperand) {
            this.leftOperand = leftOperand;
            this.operator = operator;
            this.rightOperand = rightOperand;
        }

        public String getLeftOperand() {
            return leftOperand;
        }

        public void setLeftOperand(String leftOperand) {
            this.leftOperand = leftOperand;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getRightOperand() {
            return rightOperand;
        }

        public void setRightOperand(String rightOperand) {
            this.rightOperand = rightOperand;
        }
    }
}
