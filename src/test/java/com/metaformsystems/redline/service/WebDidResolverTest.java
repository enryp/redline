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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebDidResolverTest {

    @Test
    void returnsFirstProtocolEndpoint() throws Exception {
        var json = """
                {
                  "service": [
                    { "type": "ProtocolEndpoint", "serviceEndpoint": "http://example.com/api" }
                  ]
                }
                """;
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        var resolver = resolverWithMockResponse(200, json, captor);
        var endpoint = resolver.resolveProtocolEndpoints("did:web:example.com");
        assertEquals("http://example.com/api", endpoint);
    }

    @Test
    void typeComparisonIsCaseInsensitive() throws Exception {
        var json = """
                {
                  "service": [
                    { "type": "protocolendpoint", "serviceEndpoint": "http://lowercase.example/api" }
                  ]
                }
                """;
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        var resolver = resolverWithMockResponse(200, json, captor);
        assertEquals("http://lowercase.example/api", resolver.resolveProtocolEndpoints("did:web:example.com"));
    }

    @Test
    void returnsFirstWhenMultipleServices() throws Exception {
        var json = """
                {
                  "service": [
                    { "type": "ProtocolEndpoint", "serviceEndpoint": "http://first.example" },
                    { "type": "ProtocolEndpoint", "serviceEndpoint": "http://second.example" }
                  ]
                }
                """;
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        var resolver = resolverWithMockResponse(200, json, captor);
        assertEquals("http://first.example", resolver.resolveProtocolEndpoints("did:web:example.com"));
    }

    @Test
    void returnsNullWhenNoServices() throws Exception {
        var json = "{ }";
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        var resolver = resolverWithMockResponse(200, json, captor);
        assertNull(resolver.resolveProtocolEndpoints("did:web:example.com"));
    }

    @Test
    void throwsOnNon200Response() throws Exception {
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        var resolver = resolverWithMockResponse(404, "{ }", captor);
        assertThat(resolver.resolveProtocolEndpoints("did:web:example.com")).isNull();
    }

    @Test
    void throwsWhenHttpClientErrors() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("network"));
        var resolver = new WebDidResolver(httpClient, new ObjectMapper());
        assertThat(resolver.resolveProtocolEndpoints("did:web:example.com")).isNull();
    }

    @Test
    void convertDidToUrlRespectsHttpsFlag() throws Exception {
        var json = """
                {
                  "service": [
                    { "type": "ProtocolEndpoint", "serviceEndpoint": "http://example.com/api" }
                  ]
                }
                """;
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        var resolver = resolverWithMockResponse(200, json, captor);

        // set private field forceHttps = true via reflection
        var field = WebDidResolver.class.getDeclaredField("forceHttps");
        field.setAccessible(true);
        field.setBoolean(resolver, true);

        resolver.resolveProtocolEndpoints("did:web:example.com");

        var capturedUri = captor.getValue().uri().toString();
        assertTrue(capturedUri.startsWith("https://example.com/"));
    }

    @Test
    void convertDidToUrlHandlesColonInHostAndPaths() throws Exception {
        var json = """
                {
                  "service": [
                    { "type": "ProtocolEndpoint", "serviceEndpoint": "http://example.com/api" }
                  ]
                }
                """;
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        var resolver = resolverWithMockResponse(200, json, captor);

        resolver.resolveProtocolEndpoints("did:web:sub:domain:with:parts");
        var capturedUri = captor.getValue().uri().toString();
        // should have replaced ':' with '/'
        assertTrue(capturedUri.contains("sub/domain/with/parts/.well-known/did.json"));
    }

    @Test
    void invalidDidFormatThrows() {
        var httpClient = mock(HttpClient.class);
        var resolver = new WebDidResolver(httpClient, new ObjectMapper());
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveProtocolEndpoints("invalid:did"));
    }

    private WebDidResolver resolverWithMockResponse(int status, String body, ArgumentCaptor<HttpRequest> requestCaptor) throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(status);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        return new WebDidResolver(httpClient, new ObjectMapper());
    }
}