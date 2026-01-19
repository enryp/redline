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

package com.metaformsystems.redline;

public interface TestData {
    String PARTICIPANT_PROFILE_RESPONSE = """
            {
              "id": "0d93930b-2c92-4421-93f2-533d392ca517",
              "version": 0,
              "identifier": "did:web:identityhub.edc-v.svc.cluster.local%3A7083:consumer",
              "tenantId": "e2387a34-581b-48d0-864c-66b5c859af29",
              "participantRoles": {},
              "vpas": [
                {
                  "id": "69b9dba2-35fc-47ea-b35a-93edb3804ab9",
                  "version": 0,
                  "state": "active",
                  "stateTimestamp": "2026-01-16T08:01:03.809045341Z",
                  "type": "cfm.connector",
                  "cellId": "1db5a032-0515-4e95-bd01-a1069d808bb9"
                },
                {
                  "id": "95f509ba-2374-4fe7-8275-e1265a6fcd95",
                  "version": 0,
                  "state": "active",
                  "stateTimestamp": "2026-01-16T08:01:03.809047551Z",
                  "type": "cfm.credentialservice",
                  "cellId": "1db5a032-0515-4e95-bd01-a1069d808bb9"
                },
                {
                  "id": "e7650ca5-8e20-4e98-9883-7f72d6362d0c",
                  "version": 0,
                  "state": "active",
                  "stateTimestamp": "2026-01-16T08:01:03.809049133Z",
                  "type": "cfm.dataplane",
                  "cellId": "1db5a032-0515-4e95-bd01-a1069d808bb9"
                }
              ],
              "properties": {
                "cfm.vpa.state": {
                  "credentialRequest": "http://identityhub.edc-v.svc.cluster.local:7081/v1alpha/participants/ODM0YzkzMDhmMjllNDgwMGI0ZmY3MTRkZTkwNzQ0MzM/credentials/request/c248a998-b73b-4288-8f44-e83812d4448f",
                  "holderPid": "c248a998-b73b-4288-8f44-e83812d4448f",
                  "participantContextId": "834c9308f29e4800b4ff714de9074433"
                }
              },
              "error": false
            }
            """;

    String VAULT_CREDENTIAL_RESPONSE = """
            {
                "data": {
                    "data": {
                        "content": "secret-value-here"
                    }
                 }
            }
            """;
}
