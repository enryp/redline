package com.metaformsystems.redline.controller;

import com.metaformsystems.redline.client.management.dto.Catalog;
import com.metaformsystems.redline.dao.DataspaceResource;
import com.metaformsystems.redline.dao.FileResource;
import com.metaformsystems.redline.dao.NewParticipantDeployment;
import com.metaformsystems.redline.dao.NewServiceProvider;
import com.metaformsystems.redline.dao.NewTenantRegistration;
import com.metaformsystems.redline.dao.ParticipantResource;
import com.metaformsystems.redline.dao.PartnerReferenceResource;
import com.metaformsystems.redline.dao.ServiceProviderResource;
import com.metaformsystems.redline.dao.TenantResource;
import com.metaformsystems.redline.service.ServiceProviderService;
import com.metaformsystems.redline.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Main API controller for the Redline UI
 */
@RestController
@RequestMapping("/api/ui")
@Tag(name = "Redline UI", description = "UI API for managing dataspaces, service providers, tenants, and participants")
public class RedlineController {
    private final ServiceProviderService serviceProviderService;
    private final TenantService tenantService;

    public RedlineController(ServiceProviderService serviceProviderService, TenantService tenantService) {
        this.tenantService = tenantService;
        this.serviceProviderService = serviceProviderService;
    }

    @GetMapping("dataspaces")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all dataspaces", description = "Retrieves a list of all available dataspaces")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved dataspaces")
    })
    public ResponseEntity<List<DataspaceResource>> getDataspaces() {
        return ResponseEntity.ok(serviceProviderService.getDataspaces());
    }

    @GetMapping("service-providers")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all service providers", description = "Retrieves a list of all registered service providers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved service providers")
    })
    public ResponseEntity<List<ServiceProviderResource>> getServiceProviders() {
        return ResponseEntity.ok(serviceProviderService.getServiceProviders());
    }

    @PostMapping("service-providers")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a new service provider", description = "Registers a new service provider in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service provider successfully created",
                    content = @Content(schema = @Schema(implementation = ServiceProviderResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid service provider data")
    })
    public ResponseEntity<ServiceProviderResource> createServiceProvider(@RequestBody NewServiceProvider newServiceProvider) {
        var saved = serviceProviderService.createServiceProvider(newServiceProvider);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("service-providers/{serviceProviderId}/tenants")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Register a new tenant", description = "Registers a new tenant under a specific service provider. A participant profile is also created.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant successfully registered",
                    content = @Content(schema = @Schema(implementation = TenantResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid tenant registration data"),
            @ApiResponse(responseCode = "404", description = "Service provider not found")
    })
    @Parameter(name = "serviceProviderId", description = "Database ID of the service provider", required = true)
    @Parameter(name = "registration", description = "Tenant registration details", required = true)
    public ResponseEntity<TenantResource> registerTenant(
            @PathVariable Long serviceProviderId,
            @RequestBody NewTenantRegistration registration) {
        var tenant = tenantService.registerTenant(serviceProviderId, registration);
        return ResponseEntity.ok(tenant);
    }

    @PostMapping("service-providers/{serviceProviderId}/tenants/{tenantId}/participants/{participantId}/deployments")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Deploy a participant", description = "Deploys a participant for a tenant. This will trigger the creation of resources in the dataspace.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Participant successfully deployed",
                    content = @Content(schema = @Schema(implementation = ParticipantResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid deployment data"),
            @ApiResponse(responseCode = "404", description = "Service provider, tenant, or participant not found")
    })
    @Parameter(name = "serviceProviderId", description = "Database ID of the service provider", required = true)
    @Parameter(name = "tenantId", description = "Database ID of the tenant", required = true)
    @Parameter(name = "participantId", description = "Database ID of the participant", required = true)
    public ResponseEntity<ParticipantResource> deployParticipant(@PathVariable Long serviceProviderId,
                                                                 @PathVariable Long tenantId,
                                                                 @PathVariable Long participantId,
                                                                 @RequestBody NewParticipantDeployment deployment) {
        var participant = tenantService.deployParticipant(deployment);
        return ResponseEntity.ok(participant);
    }

    @GetMapping("service-providers/{serviceProviderId}/tenants/{tenantId}")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get tenant details", description = "Retrieves detailed information about a specific tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tenant details",
                    content = @Content(schema = @Schema(implementation = TenantResource.class))),
            @ApiResponse(responseCode = "404", description = "Service provider or tenant not found")
    })
    @Parameter(name = "serviceProviderId", description = "Database ID of the service provider", required = true)
    @Parameter(name = "tenantId", description = "Database ID of the tenant", required = true)
    public ResponseEntity<TenantResource> getTenant(@PathVariable Long serviceProviderId,
                                                    @PathVariable Long tenantId) {
        var tenantResource = tenantService.getTenant(tenantId);
        // TODO auth check for provider access
        return ResponseEntity.ok(tenantResource);
    }

    @GetMapping("service-providers/{serviceProviderId}/tenants/{tenantId}/participants/{participantId}")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get participant details", description = "Retrieves detailed information about a specific participant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved participant details",
                    content = @Content(schema = @Schema(implementation = ParticipantResource.class))),
            @ApiResponse(responseCode = "404", description = "Service provider, tenant, or participant not found")
    })
    @Parameter(name = "serviceProviderId", description = "Database ID of the service provider", required = true)
    @Parameter(name = "tenantId", description = "Database ID of the tenant", required = true)
    @Parameter(name = "participantId", description = "Database ID of the participant", required = true)
    public ResponseEntity<ParticipantResource> getParticipant(@PathVariable Long serviceProviderId,
                                                              @PathVariable Long tenantId,
                                                              @PathVariable Long participantId) {
        var participantResource = tenantService.getParticipant(participantId);
        // TODO auth check for provider access
        return ResponseEntity.ok(participantResource);
    }

    @GetMapping("service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/partners/{dataspaceId}")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get partner references", description = "Retrieves a list of partner references for a participant in a specific dataspace")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved partner references"),
            @ApiResponse(responseCode = "404", description = "Service provider, tenant, participant, or dataspace not found")
    })
    @Parameter(name = "providerId", description = "Database ID of the service provider", required = true)
    @Parameter(name = "tenantId", description = "Database ID of the tenant", required = true)
    @Parameter(name = "participantId", description = "Database ID of the participant", required = true)
    @Parameter(name = "dataspaceId", description = "Database ID of the dataspace", required = true)
    public ResponseEntity<List<PartnerReferenceResource>> getPartners(@PathVariable Long providerId,
                                                                      @PathVariable Long tenantId,
                                                                      @PathVariable Long participantId,
                                                                      @PathVariable Long dataspaceId) {
        var references = tenantService.getPartnerReferences(participantId, dataspaceId);
        // TODO auth check for provider access
        return ResponseEntity.ok(references);
    }

    @PostMapping(path = "service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/files", consumes = "multipart/form-data")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Upload a file", description = "Uploads a file for a specific participant with associated metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File successfully uploaded"),
            @ApiResponse(responseCode = "400", description = "Invalid file or metadata"),
            @ApiResponse(responseCode = "404", description = "Service provider, tenant, or participant not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error during file upload")
    })
    @Parameter(name = "participantId", description = "Database ID of the participant", required = true)
    @Parameter(name = "tenantId", description = "Database ID of the tenant", required = true)
    @Parameter(name = "providerId", description = "Database ID of the service provider", required = true)
    public ResponseEntity<Void> uploadFile(@PathVariable Long participantId,
                                           @PathVariable Long tenantId,
                                           @PathVariable Long providerId,
                                           @RequestPart("metadata") Map<String, Object> metadata,
                                           @RequestPart("file") MultipartFile file) {

        try {
            tenantService.uploadFileForParticipant(participantId, metadata, file.getInputStream(), file.getContentType(), file.getOriginalFilename());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(null);
    }

    @GetMapping("service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/files")
//    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "List files", description = "Retrieves a list of all files associated with a specific participant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved file list"),
            @ApiResponse(responseCode = "404", description = "Service provider, tenant, or participant not found")
    })
    @Parameter(name = "participantId", description = "Database ID of the participant", required = true)
    @Parameter(name = "tenantId", description = "Database ID of the tenant", required = true)
    @Parameter(name = "providerId", description = "Database ID of the service provider", required = true)
    public ResponseEntity<List<FileResource>> listFiles(@PathVariable Long participantId,
                                                        @PathVariable Long tenantId,
                                                        @PathVariable Long providerId) {
        var files = tenantService.listFilesForParticipant(participantId);
        return ResponseEntity.ok(files);
    }

    @PostMapping("service-providers/{providerId}/tenants/{tenantId}/participants/{participantId}/catalog")
    @Operation(summary = "Request catalog", description = "Requests a catalog from a counter-party participant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved catalog",
                    content = @Content(schema = @Schema(implementation = Catalog.class))),
            @ApiResponse(responseCode = "400", description = "Invalid counter-party identifier"),
            @ApiResponse(responseCode = "404", description = "Service provider, tenant, or participant not found")
    })
    @Parameter(name = "cacheControl", description = "Cache control directive", required = false)
    @Parameter(name = "providerId", description = "Database ID of the service provider", required = true)
    @Parameter(name = "tenantId", description = "Database ID of the tenant", required = true)
    @Parameter(name = "participantId", description = "Database ID of the participant", required = true)
    @Parameter(name = "counterPartyIdentifier", description = "Identifier of the counter-party to request catalog from", required = true)
    public ResponseEntity<Catalog> requestCatalog(@RequestHeader(name = "Cache-Control", required = false, defaultValue = "no-cache") String cacheControl,
                                                  @PathVariable Long providerId,
                                                  @PathVariable Long tenantId,
                                                  @PathVariable Long participantId,
                                                  @RequestParam String counterPartyIdentifier) {

        var catalog = tenantService.requestCatalog(participantId, counterPartyIdentifier, cacheControl);
        return ResponseEntity.ok(catalog);
    }


}
