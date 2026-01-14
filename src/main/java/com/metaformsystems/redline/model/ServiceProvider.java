package com.metaformsystems.redline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

/**
 * An organization that provides dataspace services (e.g., a connector and credential service) to tenants.
 */
@Entity
@Table(name = "providers")
public class ServiceProvider extends VersionedEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "serviceProvider")
    private Set<Tenant> tenants = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Tenant> getTenants() {
        return tenants;
    }

    public void setTenants(Set<Tenant> tenants) {
        this.tenants = tenants;
    }
}

