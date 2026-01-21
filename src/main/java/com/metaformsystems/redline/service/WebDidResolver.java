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

package com.metaformsystems.redline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebDidResolver {

    private static final Logger log = LoggerFactory.getLogger(WebDidResolver.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    @Value("${web.did.forceHttps:false}")
    private boolean forceHttps;

    public WebDidResolver() {
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper();
    }

    public WebDidResolver(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Resolves a web DID and returns protocol service endpoints.
     *
     * @param did the web DID to resolve (e.g., "did:web:example.com")
     * @return Result containing list of protocol service endpoints or error
     */
    public String resolveProtocolEndpoints(String did) {
        var url = convertDidToUrl(did, forceHttps);
        var didDocument = fetchDidDocument(url);
        if (didDocument == null) {
            return null;
        }
        var endpoints = extractProtocolEndpoints(didDocument);
        return endpoints.isEmpty() ? null : endpoints.getFirst();
    }

    private String convertDidToUrl(String did, boolean forceHttps) {
        if (!did.startsWith("did:web:")) {
            throw new IllegalArgumentException("Invalid web DID format");
        }

        var identifier = did.substring("did:web:".length());
        var path = identifier.replace(":", "/");
        return (forceHttps ? "https://" : "http://") + path + "/.well-known/did.json";
    }

    private JsonNode fetchDidDocument(String url) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Failed to fetch DID document, HTTP request {}: {}", url, response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (ConnectException e) {
            log.error("Failed to resolve DID Web URL '{}' (ConnectException): {}", url, e.getMessage());
            return null;
        } catch (IOException | InterruptedException e) {
            log.error("Failed to fetch DID document for url '{}': {}", url, e.getMessage());
            return null;
        }


    }

    private List<String> extractProtocolEndpoints(JsonNode didDocument) {
        var endpoints = new ArrayList<String>();
        var services = didDocument.get("service");

        if (services != null && services.isArray()) {
            for (var service : services) {
                var type = service.get("type");
                if (type != null && "ProtocolEndpoint".equalsIgnoreCase(type.asText())) {
                    var endpoint = service.get("serviceEndpoint");
                    if (endpoint != null && endpoint.isTextual()) {
                        endpoints.add(endpoint.asText());
                    }
                }
            }
        }

        return endpoints;
    }

}
