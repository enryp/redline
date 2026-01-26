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

package com.metaformsystems.redline.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

import java.util.HashMap;
import java.util.Map;

/**
 * A reference to a partner organization. The identifier is the participant identifier such as a DID.
 */
@Embeddable
public record PartnerReference(
        String identifier,
        String nickname,

        @Column(name = "properties", columnDefinition = "TEXT")
        @Convert(converter = HashMapConverter.class)
        Map<String, Object> properties
) {
    public PartnerReference {
        // Canonical constructor - initialize properties if null
        if (properties == null) {
            properties = new HashMap<>();
        }
    }
    
    // Convenience constructor for backward compatibility
    public PartnerReference(String identifier, String nickname) {
        this(identifier, nickname, new HashMap<>());
    }
}
