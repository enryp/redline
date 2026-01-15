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

public final class QuerySpec {
    @JsonProperty("@context")
    private final String[] context = new String[]{
            "https://w3id.org/edc/connector/management/v2",
    };
    @JsonProperty("@type")
    private final String type = "QuerySpec";
    private int offset;
    private int limit;
    private String sortField;
    private String sortOrder;
    private List<Criterion> filterExpression;

    public String[] getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<Criterion> getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(List<Criterion> filterExpression) {
        this.filterExpression = filterExpression;
    }

    public static final class Builder {
        private final QuerySpec querySpec;

        private Builder() {
            querySpec = new QuerySpec();
        }

        public static Builder aQuerySpecDto() {
            return new Builder();
        }

        public Builder offset(int offset) {
            querySpec.setOffset(offset);
            return this;
        }

        public Builder limit(int limit) {
            querySpec.setLimit(limit);
            return this;
        }

        public Builder sortField(String sortField) {
            querySpec.setSortField(sortField);
            return this;
        }

        public Builder sortOrder(String sortOrder) {
            querySpec.setSortOrder(sortOrder);
            return this;
        }

        public Builder filterExpression(List<Criterion> filterExpression) {
            querySpec.setFilterExpression(filterExpression);
            return this;
        }

        public QuerySpec build() {
            return querySpec;
        }
    }
}
