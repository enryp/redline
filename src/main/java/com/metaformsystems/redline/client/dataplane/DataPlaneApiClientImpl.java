/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package com.metaformsystems.redline.client.dataplane;

import com.metaformsystems.redline.client.TokenProvider;
import com.metaformsystems.redline.client.dataplane.dto.UploadResponse;
import com.metaformsystems.redline.client.management.dto.QuerySpec;
import com.metaformsystems.redline.repository.ParticipantRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class DataPlaneApiClientImpl implements DataPlaneApiClient {
    private final WebClient dataPlanePublicClient;
    private final WebClient dataPlaneInternalClient;
    private final ParticipantRepository participantRepository;
    private final TokenProvider tokenProvider;

    public DataPlaneApiClientImpl(WebClient dataPlanePublicClient, WebClient dataPlaneInternalClient, ParticipantRepository participantRepository, TokenProvider tokenProvider) {
        this.dataPlanePublicClient = dataPlanePublicClient.mutate()
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                        .build())
                .build();
        this.dataPlaneInternalClient = dataPlaneInternalClient;
        this.participantRepository = participantRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public UploadResponse uploadMultipart(String participantContextId, Map<String, Object> metadata, InputStream data) {
        var bodyBuilder = new MultipartBodyBuilder();

        // Add metadata fields
        if (metadata != null) {
            bodyBuilder.part("metadata", metadata);
        }

        // Add file data
        bodyBuilder
                .part("file", new InputStreamResource(data))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return dataPlaneInternalClient.post()
                .uri("/certs")
                .header("Authorization", "Bearer " + getToken(participantContextId))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .retrieve()
                .bodyToMono(UploadResponse.class)
                .block();
    }

    @Override
    public List<UploadResponse> getAllUploads() {
        return dataPlaneInternalClient.post()
                .uri("/certs/request")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UploadResponse>>() {
                })
                .block();
    }

    @Override
    public List<UploadResponse> listPublicFiles(String participantContextId, QuerySpec querySpec) {
        return dataPlanePublicClient.post()
                .uri("/certs/request")
                .bodyValue(querySpec)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UploadResponse>>() {
                })
                .block();
    }

    @Override
    public byte[] downloadFile(String authToken, String fileId) {

        Flux<DataBuffer> dataBufferFlux = dataPlanePublicClient.get()
                .uri("/certs/" + fileId)
                .header("Authorization", authToken)
                .retrieve()
                .bodyToFlux(DataBuffer.class);

        return DataBufferUtils.join(dataBufferFlux)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .block();
    }

    private String getToken(String participantContextId) {
        var participantProfile = participantRepository.findByParticipantContextId(participantContextId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found with context id: " + participantContextId));

        return tokenProvider.getToken(participantProfile.getClientCredentials().clientId(), participantProfile.getClientCredentials().clientSecret(), "management-api:write management-api:read");
    }
}
