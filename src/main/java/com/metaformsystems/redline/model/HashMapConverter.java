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

package com.metaformsystems.redline.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Converter
public class HashMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> map) {

        String json = null;
        try {
            json = objectMapper.writeValueAsString(map);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return json;
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String json) {

        Map<String, Object> map = null;
        try {
            map = objectMapper.readValue(json,
                    new TypeReference<HashMap<String, Object>>() {
                    });
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return map;
    }
}
