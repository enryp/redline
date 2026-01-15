package com.metaformsystems.redline.dao;

import java.util.List;

public class DataplaneRegistration {
    private List<String> allowedSourceTypes;
    private List<String> allowedTransferTypes;
    private String url;
    private List<String> destinationProvisionTypes;

    public List<String> getAllowedSourceTypes() {
        return allowedSourceTypes;
    }

    public void setAllowedSourceTypes(List<String> allowedSourceTypes) {
        this.allowedSourceTypes = allowedSourceTypes;
    }

    public List<String> getAllowedTransferTypes() {
        return allowedTransferTypes;
    }

    public void setAllowedTransferTypes(List<String> allowedTransferTypes) {
        this.allowedTransferTypes = allowedTransferTypes;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getDestinationProvisionTypes() {
        return destinationProvisionTypes;
    }

    public void setDestinationProvisionTypes(List<String> destinationProvisionTypes) {
        this.destinationProvisionTypes = destinationProvisionTypes;
    }

    public static final class Builder {
        private final DataplaneRegistration dataplaneRegistration;

        private Builder() {
            dataplaneRegistration = new DataplaneRegistration();
        }

        public static Builder aDataplaneRegistration() {
            return new Builder();
        }

        public Builder allowedSourceTypes(List<String> allowedSourceTypes) {
            dataplaneRegistration.setAllowedSourceTypes(allowedSourceTypes);
            return this;
        }

        public Builder allowedTransferTypes(List<String> allowedTransferTypes) {
            dataplaneRegistration.setAllowedTransferTypes(allowedTransferTypes);
            return this;
        }

        public Builder url(String url) {
            dataplaneRegistration.setUrl(url);
            return this;
        }

        public DataplaneRegistration build() {
            return dataplaneRegistration;
        }

        public Builder destinationProvisionTypes(List<String> httpData) {
            dataplaneRegistration.setDestinationProvisionTypes(httpData);
            return this;
        }
    }
}
