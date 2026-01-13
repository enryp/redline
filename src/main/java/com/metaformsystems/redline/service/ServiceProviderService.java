package com.metaformsystems.redline.service;

import com.metaformsystems.redline.dao.DataspaceResource;
import com.metaformsystems.redline.dao.NewServiceProvider;
import com.metaformsystems.redline.dao.ServiceProviderResource;
import com.metaformsystems.redline.model.ServiceProvider;
import com.metaformsystems.redline.repository.DataspaceRepository;
import com.metaformsystems.redline.repository.ServiceProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceProviderService {
    private final ServiceProviderRepository serviceProviderRepository;
    private final DataspaceRepository dataspaceRepository;

    public ServiceProviderService(ServiceProviderRepository serviceProviderRepository, DataspaceRepository dataspaceRepository) {
        this.serviceProviderRepository = serviceProviderRepository;
        this.dataspaceRepository = dataspaceRepository;
    }

    @Transactional
    public ServiceProviderResource createServiceProvider(NewServiceProvider provider) {
        var serviceProvider = new ServiceProvider();
        serviceProvider.setName(provider.name());
        var saved = serviceProviderRepository.save(serviceProvider);
        return new ServiceProviderResource(saved.getId(), saved.getName());
    }

    @Transactional
    public List<DataspaceResource> getDataspaces() {
        return dataspaceRepository.findAll().stream()
                .map(dataspace -> new DataspaceResource(dataspace.getId(), dataspace.getName()))
                .toList();
    }

    @Transactional
    public List<ServiceProviderResource> getServiceProviders() {
        return serviceProviderRepository.findAll().stream()
                .map(provider -> new ServiceProviderResource(provider.getId(), provider.getName()))
                .toList();
    }
}

