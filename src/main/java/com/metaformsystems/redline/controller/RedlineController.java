package com.metaformsystems.redline.controller;

import com.metaformsystems.redline.dao.DataspaceResource;
import com.metaformsystems.redline.dao.NewParticipantDeployment;
import com.metaformsystems.redline.dao.NewServiceProvider;
import com.metaformsystems.redline.dao.NewTenantRegistration;
import com.metaformsystems.redline.dao.ParticipantProfileResource;
import com.metaformsystems.redline.dao.ServiceProviderResource;
import com.metaformsystems.redline.model.Tenant;
import com.metaformsystems.redline.repository.ServiceProviderRepository;
import com.metaformsystems.redline.service.ServiceProviderService;
import com.metaformsystems.redline.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *
 */
@RestController
@RequestMapping("/api/ui")
public class RedlineController {
    private final ServiceProviderService serviceProviderService;
    private final TenantService tenantService;

    public RedlineController(ServiceProviderService serviceProviderService,
                             TenantService tenantService,
                             ServiceProviderRepository serviceProviderRepository) {
        this.tenantService = tenantService;
        this.serviceProviderService = serviceProviderService;
    }

    @GetMapping("dataspaces")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<DataspaceResource>> getDataspaces() {
        // TODO make a service
        return ResponseEntity.ok(serviceProviderService.getDataspaces());
    }

    @GetMapping("service-providers")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ServiceProviderResource>> getServiceProviders() {
        // TODO make a service
        return ResponseEntity.ok(serviceProviderService.getServiceProviders());
    }

    @PostMapping("service-providers")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ServiceProviderResource> createServiceProvider(@RequestBody NewServiceProvider newServiceProvider) {
        var saved = serviceProviderService.createServiceProvider(newServiceProvider);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("service-providers/{id}/registrations")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Tenant> createRegistration(@PathVariable Long id, @RequestBody NewTenantRegistration registration) {
        var tenant = tenantService.registerTenant(id, registration);
        return ResponseEntity.ok(tenant);
    }

    @PostMapping("service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/deployments")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ParticipantProfileResource> deployParticipant(@RequestBody NewParticipantDeployment deployment) {
        var participant = tenantService.deployParticipant(deployment);
        return ResponseEntity.ok(participant);
    }

}
