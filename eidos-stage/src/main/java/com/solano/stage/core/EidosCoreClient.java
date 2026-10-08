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

package com.solano.stage.core;

import com.solano.shared.dto.GoldenRecordDto;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP-клиент к eidos-core на Spring {@link RestClient} (единый фреймворк со
 * всеми сервисами). Отправляет преобразованную Golden Record во внутренний API
 * core; любой не-2xx ответ или сетевая ошибка превращаются в
 * {@link EidosCoreException}.
 */
@Component
public class EidosCoreClient {

    private static final Logger log = LoggerFactory.getLogger(EidosCoreClient.class);
    private static final String SAVE_PATH = "/api/v1/internal/golden-records";
    private static final String SAVE_LEGAL_PATH = "/api/v1/internal/legal-records";

    private final RestClient restClient;

    public EidosCoreClient(
            RestClient.Builder restClientBuilder,
            @Value("${eidos.core.base-url}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public void saveGoldenRecord(GoldenRecordDto record, String source, String clientSourceIdentificator) {
        CoreSaveRequest payload = new CoreSaveRequest(record, source, clientSourceIdentificator);
        try {
            restClient.post()
                    .uri(SAVE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("eidos-core accepted Golden Record (source={})", source);
        } catch (RestClientResponseException e) {
            throw new EidosCoreException(
                    "eidos-core returned HTTP " + e.getStatusCode().value() + " for source=" + source
                            + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new EidosCoreException("I/O error while calling eidos-core for source=" + source, e);
        }
    }

    /** Отправляет Золотую запись юрлица (мерчанта) в core. */
    public void saveLegalEntity(LegalEntityGoldenRecordDto record, String source, String clientSourceIdentificator) {
        CoreSaveRequest payload = new CoreSaveRequest(record, source, clientSourceIdentificator);
        try {
            restClient.post()
                    .uri(SAVE_LEGAL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("eidos-core accepted legal entity (source={})", source);
        } catch (RestClientResponseException e) {
            throw new EidosCoreException(
                    "eidos-core returned HTTP " + e.getStatusCode().value() + " for source=" + source
                            + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new EidosCoreException("I/O error while calling eidos-core for source=" + source, e);
        }
    }
}
