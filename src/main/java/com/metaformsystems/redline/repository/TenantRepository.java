package com.metaformsystems.redline.repository;

import com.metaformsystems.redline.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    List<Tenant> findByServiceProviderId(Long serviceProviderId);
}