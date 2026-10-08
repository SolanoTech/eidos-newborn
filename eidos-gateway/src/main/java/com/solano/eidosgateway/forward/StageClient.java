/*
 * Copyright 2026 LLC SOLANOTECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solano.eidosgateway.forward;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Форвард карточек клиентов в eidos-stage ({@code POST /api/v1/client-data}).
 * Используется Kafka-консьюмером Gateway (REST-поток для stage идёт через общий
 * прокси).
 */
@Component
public class StageClient {

    private static final Logger log = LoggerFactory.getLogger(StageClient.class);
    private static final String CLIENT_DATA_PATH = "/api/v1/client-data";
    private static final String LEGAL_DATA_PATH = "/api/v1/legal-data";

    private final RestClient restClient;

    public StageClient(RestClient gatewayRestClient, @Value("${eidos.stage.base-url}") String stageBaseUrl) {
        this.restClient = gatewayRestClient.mutate().baseUrl(stripTrailingSlash(stageBaseUrl)).build();
    }

    /** Отправляет «сырое» тело карточки клиента в stage. */
    public void forwardClientData(byte[] payload) {
        restClient.post()
                .uri(CLIENT_DATA_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        log.debug("Forwarded client-data payload to stage ({} bytes)", payload == null ? 0 : payload.length);
    }

    /** Отправляет «сырое» тело карточки юрлица в stage (отдельный канал). */
    public void forwardLegalData(byte[] payload) {
        restClient.post()
                .uri(LEGAL_DATA_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        log.debug("Forwarded legal-data payload to stage ({} bytes)", payload == null ? 0 : payload.length);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
