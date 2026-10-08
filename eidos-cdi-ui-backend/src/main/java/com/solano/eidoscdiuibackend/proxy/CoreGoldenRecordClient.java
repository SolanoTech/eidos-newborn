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

package com.solano.eidoscdiuibackend.proxy;

import com.solano.eidoscdiuibackend.web.dto.ExternalIdView;
import com.solano.eidoscdiuibackend.web.dto.FieldMetaView;
import com.solano.eidoscdiuibackend.web.dto.GoldenRecordDetail;
import com.solano.eidoscdiuibackend.web.dto.SearchPage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Доступ к поиску Golden Record через internal API eidos-core. Чистое
 * проксирование; ошибки downstream транслирует глобальный обработчик.
 */
@Component
public class CoreGoldenRecordClient {

    private static final String BASE = "/api/v1/internal/golden-records";
    private static final String SEARCH = BASE + "/search";

    private final RestClient coreRestClient;

    public CoreGoldenRecordClient(RestClient coreRestClient) {
        this.coreRestClient = coreRestClient;
    }

    public SearchPage search(String query, int page) {
        return coreRestClient.get()
                .uri(uriBuilder -> uriBuilder.path(SEARCH)
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(SearchPage.class);
    }

    private static final ParameterizedTypeReference<java.util.List<FieldMetaView>> META_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<java.util.Map<String, Object>> METRICS =
            new ParameterizedTypeReference<>() {
            };

    /** Структурный поиск по реквизитам (пустые критерии игнорируются). */
    public SearchPage structuredSearch(String pinfl, String passport, String lastName,
                                       String firstName, String middleName, String phone, int page) {
        return coreRestClient.get()
                .uri(b -> b.path(BASE + "/search/structured")
                        .queryParam("pinfl", nz(pinfl))
                        .queryParam("passport", nz(passport))
                        .queryParam("lastName", nz(lastName))
                        .queryParam("firstName", nz(firstName))
                        .queryParam("middleName", nz(middleName))
                        .queryParam("phone", nz(phone))
                        .queryParam("page", page)
                        .build())
                .retrieve().body(SearchPage.class);
    }

    public java.util.List<FieldMetaView> fieldMeta(String clientId) {
        return coreRestClient.get().uri(BASE + "/{clientId}/field-meta", clientId)
                .retrieve().body(META_LIST);
    }

    private static final ParameterizedTypeReference<java.util.List<ExternalIdView>> EXT_LIST =
            new ParameterizedTypeReference<>() {
            };

    public java.util.List<ExternalIdView> externalIds(String clientId) {
        return coreRestClient.get().uri(BASE + "/{clientId}/external-ids", clientId)
                .retrieve().body(EXT_LIST);
    }

    public java.util.Map<String, Object> metrics() {
        return coreRestClient.get().uri("/api/v1/internal/metrics").retrieve().body(METRICS);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    public GoldenRecordDetail getById(String clientId) {
        return coreRestClient.get().uri(BASE + "/{clientId}", clientId)
                .retrieve().body(GoldenRecordDetail.class);
    }
}
