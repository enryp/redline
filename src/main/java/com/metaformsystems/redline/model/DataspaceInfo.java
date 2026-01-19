package com.metaformsystems.redline.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata associated with a {@link Participant} for a specific {@link Dataspace}.
 */
@Entity
@Table(name = "dataspace_info")
public class DataspaceInfo extends VersionedEntity {
    @NotNull
    @Column(nullable = false)
    private Long dataspaceId;

    @ElementCollection
    @CollectionTable(
            name = "dataspace_info_agreement_types",
            joinColumns = @JoinColumn(name = "dataspace_info_id")
    )
    @Column(name = "agreement_type")
    private List<String> agreementTypes = new ArrayList<>();
    @ElementCollection
    @CollectionTable(
            name = "dataspace_info_roles",
            joinColumns = @JoinColumn(name = "dataspace_info_id")
    )
    @Column(name = "role")
    private List<String> roles = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "dataspace_info_partners",
            joinColumns = @JoinColumn(name = "dataspace_info_id")
    )
    private List<PartnerReference> partners = new ArrayList<>();

    public Long getDataspaceId() {
        return dataspaceId;
    }

    public void setDataspaceId(Long dataspaceId) {
        this.dataspaceId = dataspaceId;
    }

    public List<String> getAgreementTypes() {
        return agreementTypes;
    }

    public void setAgreementTypes(List<String> agreementTypes) {
        this.agreementTypes = agreementTypes;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<PartnerReference> getPartners() {
        return partners;
    }

    public void setPartners(List<PartnerReference> partners) {
        this.partners = partners;
    }
}
