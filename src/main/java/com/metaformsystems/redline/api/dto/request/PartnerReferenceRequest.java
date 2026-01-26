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

package com.metaformsystems.redline.api.dto.request;

import java.util.Map;

/**
 * Request DTO for creating a partner reference.
 */
public record PartnerReferenceRequest(
        String identifier,
        String nickname,
        Map<String, Object> properties
) {
    public PartnerReferenceRequest(String identifier, String nickname) {
        this(identifier, nickname, Map.of());
    }
}
