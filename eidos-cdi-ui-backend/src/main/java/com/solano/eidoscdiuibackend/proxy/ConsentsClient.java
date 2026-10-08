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

import com.solano.eidoscdiuibackend.web.dto.ClientConsentStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Согласия клиента через internal API eidos-consents: сводка по типам,
 * выдача нового согласия и отзыв.
 */
@Component
public class ConsentsClient {

    private static final String BASE = "/internal/api/v1/consents";
    private static final ParameterizedTypeReference<List<ClientConsentStatus>> SUMMARY =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient consentsRestClient;

    public ConsentsClient(RestClient consentsRestClient) {
        this.consentsRestClient = consentsRestClient;
    }

    public List<ClientConsentStatus> summary(UUID clientUuid) {
        return consentsRestClient.get().uri(BASE + "/client/{uuid}", clientUuid)
                .retrieve().body(SUMMARY);
    }

    /** Выдать согласие: consents ждёт короткий код типа ({@code pd}, {@code mk_sms}, …). */
    public void grant(UUID clientUuid, String typeCode) {
        consentsRestClient.post().uri(BASE)
                .body(Map.of(
                        "type", typeCode,
                        "client_uuid", clientUuid.toString(),
                        "initial_date", LocalDate.now().toString(),
                        "source_name", "cdp-console"))
                .retrieve().toBodilessEntity();
    }

    public void revoke(UUID consentId) {
        consentsRestClient.post().uri(BASE + "/{id}/revoke", consentId)
                .retrieve().toBodilessEntity();
    }
}
