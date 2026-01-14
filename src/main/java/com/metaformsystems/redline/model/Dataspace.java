package com.metaformsystems.redline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a dataspace ecosystem. A dataspace has one or more {@link DataspaceProfile}s. It may have multiple profiles
 * if more than one protocol version or policy set is supported.
 */
@Entity
@Table(name = "dataspaces")
public class Dataspace extends VersionedEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @OneToMany
    @JoinColumn(name = "dataspace_id")
    private Set<DataspaceProfile> profiles = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<DataspaceProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Set<DataspaceProfile> profiles) {
        this.profiles = profiles;
    }
}
