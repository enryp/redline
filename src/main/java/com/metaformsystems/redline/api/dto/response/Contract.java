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

package com.metaformsystems.redline.api.dto.response;

import java.time.Instant;
import java.util.Map;

public class Contract {
    private boolean isPending = true;

    private String id;
    private String counterParty;
    private String type;
    private String agreementId;
    private String assetId;
    private Instant signingDate;
    private String consumer;
    private String provider;
    private Map<String, Object> policy;


    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCounterParty() {
        return counterParty;
    }

    public void setCounterParty(String counterParty) {
        this.counterParty = counterParty;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(String agreementId) {
        this.agreementId = agreementId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public Instant getSigningDate() {
        return signingDate;
    }

    public void setSigningDate(Instant signingDate) {
        this.signingDate = signingDate;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, Object> getPolicy() {
        return policy;
    }

    public void setPolicy(Map<String, Object> policy) {
        this.policy = policy;
    }

    public static final class Builder {
        private final Contract contract;

        private Builder() {
            contract = new Contract();
        }

        public static Builder aContract() {
            return new Builder();
        }

        public Builder counterParty(String counterParty) {
            contract.setCounterParty(counterParty);
            return this;
        }

        public Builder type(String type) {
            contract.setType(type);
            return this;
        }

        public Builder id(String id) {
            contract.setId(id);
            return this;
        }

        public Builder agreementId(String agreementId) {
            contract.setAgreementId(agreementId);
            return this;
        }

        public Builder assetId(String assetId) {
            contract.setAssetId(assetId);
            return this;
        }

        public Builder signingDate(Instant signingDate) {
            contract.setSigningDate(signingDate);
            return this;
        }

        public Builder consumer(String consumer) {
            contract.setConsumer(consumer);
            return this;
        }

        public Builder provider(String provider) {
            contract.setProvider(provider);
            return this;
        }

        public Builder policy(Map<String, Object> policy) {
            contract.setPolicy(policy);
            return this;
        }

        public Builder pending(boolean pending) {
            contract.setPending(pending);
            return this;
        }

        public Contract build() {
            return contract;
        }
    }
}
