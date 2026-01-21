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

package com.metaformsystems.redline.controller;

public class ApiError {

    private final int code;
    private final String message;

    private ApiError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiError of(int code, String message) {
        return new ApiError(code, message);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
