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

package com.solano.eidoscore.consent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

/**
 * Чтение действующего согласия из eidos-consents.
 *
 * <p>Internal API сервиса согласий открыт внутри кластера, отдельного токена не
 * требует. «Нет действующего согласия» сервис выражает как 404 — это штатный
 * ответ, а не ошибка.</p>
 */
@Component
public class ConsentsClient {

    private static final Logger log = LoggerFactory.getLogger(ConsentsClient.class);

    /** Имя константы: сервис согласий биндит тип в query именно так. */
    private static final String PERSONAL_DATA = "PERSONAL_DATA";

    private final RestClient consentsRestClient;

    public ConsentsClient(RestClient consentsRestClient) {
        this.consentsRestClient = consentsRestClient;
    }

    /**
     * Действующее на сегодня согласие клиента на обработку ПДн.
     *
     * @return пустое значение, если действующего согласия нет
     * @throws ConsentsUnavailableException если сервис согласий недоступен —
     *         отсутствие ответа нельзя трактовать как отсутствие согласия
     */
    public Optional<ActiveConsent> activePersonalDataConsent(UUID clientUuid) {
        try {
            ActiveConsent consent = consentsRestClient.get()
                    .uri("/internal/api/v1/consents/active/{uuid}?type={type}", clientUuid, PERSONAL_DATA)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        // 404 — «нет активного согласия»; тело не читаем, ошибку не бросаем
                    })
                    .body(ActiveConsent.class);
            return Optional.ofNullable(consent).filter(c -> c.endDate() != null);
        } catch (ConsentsUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Consents service unreachable while checking client {}", clientUuid, e);
            throw new ConsentsUnavailableException(clientUuid, e);
        }
    }

    /** Сервис согласий не ответил: состояние клиента осталось неизвестным. */
    public static class ConsentsUnavailableException extends RuntimeException {
        public ConsentsUnavailableException(UUID clientUuid, Throwable cause) {
            super("Cannot read consent state for client " + clientUuid, cause);
        }
    }
}
