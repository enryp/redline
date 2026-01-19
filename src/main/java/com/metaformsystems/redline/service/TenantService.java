package com.metaformsystems.redline.service;

import com.metaformsystems.redline.client.dataplane.DataPlaneApiClient;
import com.metaformsystems.redline.client.hashicorpvault.HashicorpVaultClient;
import com.metaformsystems.redline.client.management.ManagementApiClient;
import com.metaformsystems.redline.client.management.dto.Catalog;
import com.metaformsystems.redline.client.management.dto.NewAsset;
import com.metaformsystems.redline.client.management.dto.NewCelExpression;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.TenantManagerClient;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1NewTenant;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1ParticipantProfile;
import com.metaformsystems.redline.dao.FileResource;
import com.metaformsystems.redline.dao.NewParticipantDeployment;
import com.metaformsystems.redline.dao.NewTenantRegistration;
import com.metaformsystems.redline.dao.ParticipantResource;
import com.metaformsystems.redline.dao.PartnerReferenceResource;
import com.metaformsystems.redline.dao.TenantResource;
import com.metaformsystems.redline.dao.VPAResource;
import com.metaformsystems.redline.model.ClientCredentials;
import com.metaformsystems.redline.model.DataspaceInfo;
import com.metaformsystems.redline.model.DeploymentState;
import com.metaformsystems.redline.model.Participant;
import com.metaformsystems.redline.model.Tenant;
import com.metaformsystems.redline.model.UploadedFile;
import com.metaformsystems.redline.model.VirtualParticipantAgent;
import com.metaformsystems.redline.repository.ParticipantRepository;
import com.metaformsystems.redline.repository.ServiceProviderRepository;
import com.metaformsystems.redline.repository.TenantRepository;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.metaformsystems.redline.service.Constants.ASSET_PERMISSION;
import static com.metaformsystems.redline.service.Constants.MEMBERSHIP_CONTRACT_DEFINITION;
import static com.metaformsystems.redline.service.Constants.MEMBERSHIP_EXPRESSION;
import static com.metaformsystems.redline.service.Constants.MEMBERSHIP_EXPRESSION_ID;
import static com.metaformsystems.redline.service.Constants.MEMBERSHIP_POLICY;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

/**
 *
 */
@Service
public class TenantService {
    public static final String STATE_PROPERTY_KEY = "cfm.vpa.state";

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    private final TenantRepository tenantRepository;
    private final ParticipantRepository participantRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final TenantManagerClient tenantManagerClient;
    private final HashicorpVaultClient vaultClient;
    private final DataPlaneApiClient dataPlaneApiClient;
    private final ManagementApiClient managementApiClient;
    private final ConcurrentLruCache<LookupKey, CacheableEntry<Catalog>> catalogCache;

    public TenantService(TenantRepository tenantRepository,
                         ParticipantRepository participantRepository,
                         ServiceProviderRepository serviceProviderRepository,
                         TenantManagerClient tenantManagerClient,
                         HashicorpVaultClient vaultClient, DataPlaneApiClient dataPlaneApiClient, ManagementApiClient managementApiClient) {
        this.tenantRepository = tenantRepository;
        this.participantRepository = participantRepository;
        this.serviceProviderRepository = serviceProviderRepository;
        this.tenantManagerClient = tenantManagerClient;
        this.vaultClient = vaultClient;
        this.dataPlaneApiClient = dataPlaneApiClient;
        this.managementApiClient = managementApiClient;
        this.catalogCache = new ConcurrentLruCache<>(100, key -> fetchCatalog(key.participantId(), key.did()));
    }


    @Transactional
    public TenantResource getTenant(Long id) {
        return tenantRepository.findById(id)
                .map(this::toTenantResource)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + id));
    }

    @Transactional
    public TenantResource registerTenant(Long serviceProviderId, NewTenantRegistration registration) {
        // Create tenant
        var tenant = new Tenant();
        tenant.setName(registration.tenantName());
        tenant.setServiceProvider(serviceProviderRepository.getReferenceById(serviceProviderId));

        // Create participant with dataspaces
        var participant = new Participant();
        participant.setIdentifier(registration.tenantName());
        participant.setTenant(tenant);

        // FIXME for now, register in all dataspaces. This should be changed to support tenant registration in specific dataspaces.
        var dataspaces = registration.dataspaceInfos().stream().map(i -> {
            var info = new DataspaceInfo();
            info.setRoles(i.getRoles());
            info.setAgreementTypes(i.getAgreementTypes());
            info.setDataspaceId(i.getDataspaceId());
            return info;
        }).collect(toSet());

        participant.setDataspaceInfos(dataspaces);

        var savedTenant = tenantRepository.save(tenant);

        participantRepository.save(participant);

        savedTenant.getParticipants().add(participant);

        return toTenantResource(savedTenant);
    }

    @Transactional
    public ParticipantResource deployParticipant(NewParticipantDeployment deployment) {
        var participant = participantRepository.findById(deployment.participantId())
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + deployment.participantId()));

        var tenant = participant.getTenant();
        if (tenant.getCorrelationId() == null) {
            // Create Tenant in CFM and update tenant with correlation id
            var tmTenant = tenantManagerClient.createTenant(new V1Alpha1NewTenant(Map.of("name", tenant.getName())));
            tenant.setCorrelationId(tmTenant.id());
        }

        // invoke CFM to deploy the ParticipantProfile and update the internal Participant entity with correlation id, identifier, and VPAs
        var tmProfile = tenantManagerClient.createParticipantProfile(tenant.getCorrelationId(), new V1Alpha1ParticipantProfile(
                UUID.randomUUID().toString(), 0L, deployment.identifier(), tenant.getCorrelationId(), false, null, Map.of(), Map.of(), Collections.emptyList()
        ));
        participant.setCorrelationId(tmProfile.id());
        participant.setIdentifier(tmProfile.identifier());


        participant.getAgents().clear();
        participant.getAgents().addAll(tmProfile.vpas().stream().map(apiVpa -> new VirtualParticipantAgent(VirtualParticipantAgent.VpaType.fromCfmName(apiVpa.type()), DeploymentState.valueOf(apiVpa.state().toUpperCase()))).collect(Collectors.toSet()));

        // wait for participants to be ready
        var saved = participantRepository.save(participant);
        return toParticipantResource(saved);
    }

    @Transactional
    public String getParticipantContextId(Long participantId) {
        var participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + participantId));
        var participantCorrelationId = participant.getCorrelationId();
        var cfmProfile = tenantManagerClient.getParticipantProfile(participant.getTenant().getCorrelationId(), participantCorrelationId);

        var pcId = extractParticipantContextId(cfmProfile);
        participant.setParticipantContextId(pcId);
        return pcId;
    }

    /**
     * Get the client credentials for a participant, which is necessary to access the participant's APIs in the
     * control plane and identity hub later
     *
     * @param participantContextId the Participant Context ID that was created by the tenant manager. Use {@link #getParticipantContextId(Long)} to retrieve it.
     */
    @Transactional
    public ClientCredentials getClientCredentials(String participantContextId) {
        var secret = vaultClient.readSecret("/v1/secret/data/%s".formatted(participantContextId));
        if (!StringUtils.hasText(secret)) {
            return null;
        }
        //todo: store credentials somewhere safer!
        var participantProfile = participantRepository.findByParticipantContextId(participantContextId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with participantContextId id: " + participantContextId));

        var clientCredentials = new ClientCredentials(participantContextId, secret);
        participantProfile.setClientCredentials(clientCredentials);

        return clientCredentials;
    }

    @Transactional
    public ParticipantResource getParticipant(Long id) {

        var profile = participantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + id));

        // fixme: figure out a better way to synchronize redline with CFM (periodically, NATS, etc.)
        // update VPA state
        var cfmProfile = tenantManagerClient.getParticipantProfile(profile.getTenant().getCorrelationId(), profile.getCorrelationId());
        profile.setParticipantContextId(extractParticipantContextId(cfmProfile));

        // update credentials
        ofNullable(profile.getClientCredentials()).orElseGet(() -> getClientCredentials(profile.getParticipantContextId()));

        // update VPA deployment state
        cfmProfile.vpas().forEach(cfmVpa -> {
            var type = VirtualParticipantAgent.VpaType.fromCfmName(cfmVpa.type());
            ofNullable(profile.getAgentForType(type)).ifPresentOrElse(agent -> agent.setState(DeploymentState.valueOf(cfmVpa.state().toUpperCase())),
                    () -> log.warn("VPA received {} from CFM, but not found in participant {}", cfmVpa.type(), profile.getIdentifier()));
        });

        // No need to save - changes will be automatically persisted at transaction end
        return toParticipantResource(profile);
    }

    @Transactional
    public List<PartnerReferenceResource> getPartnerReferences(Long participantId, Long dataspacesId) {
        return participantRepository.findById(participantId).stream()
                .flatMap(p -> p.getDataspaceInfos().stream())
                .filter(i -> i.getDataspaceId().equals(dataspacesId))
                .flatMap(i -> i.getPartners().stream())
                .map(r -> new PartnerReferenceResource(r.identifier(), r.nickname()))
                .toList();
    }

    @Transactional
    public void uploadFileForParticipant(Long participantId, Map<String, Object> metadata, InputStream fileStream, String contentType, String originalFilename) {

        var participant = participantRepository.findById(participantId).orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + participantId));
        //1. create asset
        var participantContextId = participant.getParticipantContextId();
        var asset = createAsset(metadata, contentType, originalFilename);
        managementApiClient.createAsset(participantContextId, asset);

        // create CEL expression
        try {
            managementApiClient.createCelExpression(NewCelExpression.Builder.aNewCelExpression()
                    .id(MEMBERSHIP_EXPRESSION_ID)
                    .leftOperand("MembershipCredential")
                    .description("Expression for evaluating membership credential")
                    .scopes(Set.of("catalog", "contract.negotiation", "transfer.process"))
                    .expression(MEMBERSHIP_EXPRESSION)
                    .build());
        } catch (WebClientResponseException.Conflict e) {
            //do nothing, CEL expression already exists
        }

        //2. create policy
        var policy = MEMBERSHIP_POLICY;
        try {
            managementApiClient.createPolicy(participantContextId, policy);
        } catch (WebClientResponseException.Conflict e) {
            // do nothing, policy already exists
            log.info("Policy already exists: {}", policy.getId());
        }

        //3. create contract definition if none exists
        try {
            managementApiClient.createContractDefinition(participantContextId, MEMBERSHIP_CONTRACT_DEFINITION);
        } catch (WebClientResponseException.Conflict e) {
            // do nothing, contract definition already exists
            log.info("Contract Definition already exists: {}", policy.getId());
        }

        //4. upload file to data plane
        // todo: do we need this?
        metadata.put("originalFilename", originalFilename);
        metadata.put("contentType", contentType);

        var response = dataPlaneApiClient.uploadMultipart(participantContextId, metadata, fileStream);
        var fileId = response.id();

        //2. track uploaded file in DB
        participant.getUploadedFiles().add(new UploadedFile(fileId, originalFilename, contentType));
    }

    @Transactional
    public List<FileResource> listFilesForParticipant(Long participantId) {
        var participant = participantRepository.findById(participantId).orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + participantId));
        return participant.getUploadedFiles().stream()
                .map(f -> new FileResource(f.getFileId(), f.getOriginalFilename(), f.getContentType(), f.getCreatedAt().toString()))
                .toList();
    }

    @Transactional
    public Catalog requestCatalog(Long participantId, String counterPartyIdentifier, String cacheControl) {

        var participant = participantRepository.findById(participantId).orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + participantId));

        var key = new LookupKey(participant.getParticipantContextId(), counterPartyIdentifier);
        var catalogEntry = catalogCache.get(key);
        //todo: check if expired or must be reloaded
        if (isExpired(catalogEntry, cacheControl)) {
            log.info("Catalog cache expired or no-cache requested for participant {} and counterparty {}", participantId, counterPartyIdentifier);

            // removing and re-getting forces a cache update, i.e., reading the remote catalog again
            catalogCache.remove(key);
            return catalogCache.get(key).value();
        }

        return catalogEntry.value();
    }

    /**
     * Determines if cache entry requires refresh according to the cacheControl value
     */
    private boolean isExpired(CacheableEntry<Catalog> entry, String cacheControl) {
        if (entry == null) return true;
        if (!StringUtils.hasText(cacheControl)) return false;

        if (cacheControl.contains("no-cache") || cacheControl.contains("no-store")) {
            return true;
        }

        // Parse max-age
        var maxAgeMatch = Pattern.compile("max-age=(\\d+)").matcher(cacheControl);
        if (maxAgeMatch.find()) {
            long maxAgeSeconds = Long.parseLong(maxAgeMatch.group(1));
            return entry.timestamp().plus(Duration.ofSeconds(maxAgeSeconds)).isBefore(Instant.now());
        }

        return false;
    }

    private @Nullable String extractParticipantContextId(V1Alpha1ParticipantProfile participant) {

        var props = participant.properties();
        if (props != null && props.containsKey(STATE_PROPERTY_KEY) && props.get(STATE_PROPERTY_KEY) instanceof Map stateMap) {
            var credentialRequestUrl = stateMap.get("credentialRequestUrl");
            var holderPid = stateMap.get("holderPid");
            var participantContextId = stateMap.get("participantContextId");

            return participantContextId.toString();
        }
        return null;
    }

    private NewAsset createAsset(Map<String, Object> metadata, String contentType, String originalFilename) {

        var privateProperties = new HashMap<String, Object>(Map.of("permission", ASSET_PERMISSION));
        privateProperties.putAll(metadata);

        return NewAsset.Builder.aNewAsset()
                .id(UUID.randomUUID().toString())
                .dataAddress(Map.of(
                        "type", "HttpCertData",
                        "@type", "DataAddress"
                ))
                .privateProperties(privateProperties)
                .properties(Map.of(
                        "description", "A file uploaded by Redline on " + Instant.now().toString(),
                        "contentType", contentType,
                        "originalFilename", originalFilename
                ))
                .build();
    }

    @NonNull
    private ParticipantResource toParticipantResource(Participant saved) {
        var vpas = saved.getAgents().stream().map(vpa -> new VPAResource(vpa.getId(),
                VPAResource.Type.valueOf(vpa.getType().name()),
                com.metaformsystems.redline.dao.DeploymentState.valueOf(vpa.getState().name()))).toList();
        var infos = saved.getDataspaceInfos().stream()
                .map(i -> new com.metaformsystems.redline.dao.DataspaceInfo(
                        i.getId(),
                        i.getDataspaceId(),
                        i.getAgreementTypes(),
                        i.getRoles()))
                .toList();
        return new ParticipantResource(saved.getId(), saved.getIdentifier(), vpas, infos);
    }

    @NonNull
    private TenantResource toTenantResource(Tenant t) {
        var participants = t.getParticipants().stream()
                .map(this::toParticipantResource).toList();
        return new TenantResource(t.getId(), t.getServiceProvider().getId(), t.getName(), participants);
    }

    private CacheableEntry<Catalog> fetchCatalog(String participantId, String did) {
        return new CacheableEntry<>(managementApiClient.getCatalog(participantId, did), Instant.now());
    }

    private record LookupKey(String participantId, String did) {

    }

    private record CacheableEntry<T>(T value, Instant timestamp) {

    }
}
