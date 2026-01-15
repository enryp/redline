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

import com.metaformsystems.redline.client.dataplane.dto.UploadResponse;
import com.metaformsystems.redline.client.management.dto.QuerySpec;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface DataPlaneApiClient {
    /**
     * This is used on the provider side to upload files, such as PDFs etc.
     *
     * @param metadata optional metadata to describe the file
     * @param data     an input stream of the file data
     */
    UploadResponse uploadMultipart(String participantContextId, Map<String, String> metadata, InputStream data);

    /**
     * This is used on the provider side to list all uploaded files
     */
    List<UploadResponse> getAllUploads();

    /**
     * This method is used on the consumer side to query all files that are offered on the network
     */
    List<UploadResponse> queryProviderFiles(String participantContextId, QuerySpec querySpec);

    /**
     * Downloads a file from the provider's dataplane
     *
     * @param fileId the id of the file to download
     */
    byte[] downloadFile(String authToken, String fileId);
}
