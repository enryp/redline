package com.metaformsystems.redline.service;

import com.metaformsystems.redline.client.hashicorpvault.HashicorpVaultClient;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.TenantManagerClient;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1NewTenant;
import com.metaformsystems.redline.client.tenantmanager.v1alpha1.dto.V1Alpha1ParticipantProfile;
import com.metaformsystems.redline.dao.NewParticipantDeployment;
import com.metaformsystems.redline.dao.NewTenantRegistration;
import com.metaformsystems.redline.dao.ParticipantResource;
import com.metaformsystems.redline.dao.TenantResource;
import com.metaformsystems.redline.dao.VPAResource;
import com.metaformsystems.redline.model.ClientCredentials;
import com.metaformsystems.redline.model.DataspaceInfo;
import com.metaformsystems.redline.model.DeploymentState;
import com.metaformsystems.redline.model.Participant;
import com.metaformsystems.redline.model.Tenant;
import com.metaformsystems.redline.model.VirtualParticipantAgent;
import com.metaformsystems.redline.repository.ParticipantRepository;
import com.metaformsystems.redline.repository.ServiceProviderRepository;
import com.metaformsystems.redline.repository.TenantRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 *
 */
@Service
public class TenantService {
    public static final String STATE_PROPERTY_KEY = "cfm.vpa.state";
    private final TenantRepository tenantRepository;
    private final ParticipantRepository participantRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final TenantManagerClient tenantManagerClient;
    private final HashicorpVaultClient vaultClient;

    public TenantService(TenantRepository tenantRepository,
                         ParticipantRepository participantRepository,
                         ServiceProviderRepository serviceProviderRepository,
                         TenantManagerClient tenantManagerClient,
                         HashicorpVaultClient vaultClient) {
        this.tenantRepository = tenantRepository;
        this.participantRepository = participantRepository;
        this.serviceProviderRepository = serviceProviderRepository;
        this.tenantManagerClient = tenantManagerClient;
        this.vaultClient = vaultClient;
    }

    @Transactional
    TenantResource getTenant(Long id) {
        return tenantRepository.findById(id)
                .map(t -> {
                    var participants = t.getParticipants().stream()
                            .map(this::toParticipantResource).toList();
                    return new TenantResource(t.getId(), t.getName(), participants);
                })
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + id));
    }

    @Transactional
    public Tenant registerTenant(Long serviceProviderId, NewTenantRegistration registration) {
        // Create tenant
        var tenant = new Tenant();
        tenant.setName(registration.tenantName());
        tenant.setServiceProvider(serviceProviderRepository.getReferenceById(serviceProviderId));

        // Create participant with dataspaces
        var participant = new Participant();
        participant.setIdentifier(registration.tenantName());
        participant.setTenant(tenant);

        // FIXME for now, register in all dataspaces. This should be changed to support tenant registration in specific dataspaces.
        var dataspaces = registration.dataspaces().stream().map(dataspaceId -> {
            var info = new DataspaceInfo();
            info.setDataspaceId(dataspaceId);
            return info;
        }).collect(toSet());

        participant.setDataspaceInfos(dataspaces);

        var savedTenant = tenantRepository.save(tenant);

        participantRepository.save(participant);

        savedTenant.getParticipants().add(participant);

        return savedTenant;
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
                UUID.randomUUID().toString(), 0L, deployment.webDid(), tenant.getCorrelationId(), false, null, Map.of(), Map.of(), Collections.emptyList()
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
        var props = tenantManagerClient.getParticipantProfile(participant.getTenant().getCorrelationId(), participantCorrelationId).properties();

        if (props != null && props.containsKey(STATE_PROPERTY_KEY) && props.get(STATE_PROPERTY_KEY) instanceof Map stateMap) {
            var credentialRequestUrl = stateMap.get("credentialRequestUrl");
            var holderPid = stateMap.get("holderPid");
            var participantContextId = stateMap.get("participantContextId");

            // update internal participant entity
            participant.setParticipantContextId(participantContextId.toString());

            return participantContextId.toString();
        }
        return null;
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
        return participantRepository.findById(id)
                .map(this::toParticipantResource)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with id: " + id));

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


}
