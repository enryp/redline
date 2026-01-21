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

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Map;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile extends VersionedEntity {
    private String fileId;
    private String originalFilename;
    private String contentType;
    @Convert(converter = HashMapConverter.class)
    private Map<String, Object> metadata;

    public UploadedFile(String fileId, String originalFilename, String contentType, Map<String, Object> metadata) {
        this.fileId = fileId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.metadata = metadata;
    }

    public UploadedFile() {

    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getFileId() {
        return fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }
}
