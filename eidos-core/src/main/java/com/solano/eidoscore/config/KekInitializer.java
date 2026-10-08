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

package com.solano.eidoscore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Готовит хранилище ключей к работе: включает движок transit и заводит KEK,
 * если их ещё нет. Обе операции идемпотентны — повторный вызов на уже
 * настроенном хранилище ничего не меняет.
 *
 * <p>Нужен потому, что на стенде OpenBao поднимается в dev-режиме и держит
 * состояние в памяти: после перезапуска контейнера настройка исчезает. В среде
 * с постоянным хранилищем это делается один раз при развёртывании, и тогда
 * инициализатор просто ничего не находит для создания.</p>
 */
@Component
public class KekInitializer {

    private static final Logger log = LoggerFactory.getLogger(KekInitializer.class);
    private static final String TOKEN_HEADER = "X-Vault-Token";

    private final RestClient openBaoRestClient;
    private final String token;
    private final String kekName;
    private final boolean enabled;

    public KekInitializer(
            RestClient openBaoRestClient,
            @Value("${eidos.crypto.openbao.token}") String token,
            @Value("${eidos.crypto.openbao.kek-name}") String kekName,
            @Value("${eidos.crypto.enabled:true}") boolean enabled
    ) {
        this.openBaoRestClient = openBaoRestClient;
        this.token = token;
        this.kekName = kekName;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureKek() {
        if (!enabled) {
            log.info("Crypto disabled, key store setup skipped");
            return;
        }
        try {
            ensure("/v1/sys/mounts/transit", Map.of("type", "transit"), "transit engine");
            ensure("/v1/transit/keys/" + kekName, Map.of("type", "aes256-gcm96"), "KEK " + kekName);
            log.info("Key store ready: transit engine and KEK {} in place", kekName);
        } catch (Exception e) {
            // Не валим старт: криптоконтур пока ни с чем не связан, а внятная
            // запись в логе полезнее падения сервиса из-за соседнего компонента.
            log.error("Key store is not ready; tokenisation will fail until it is", e);
        }
    }

    /** Повторное создание отвечает 400 «уже существует» — это штатный ответ. */
    private void ensure(String uri, Map<String, String> body, String what) {
        try {
            openBaoRestClient.post().uri(uri).header(TOKEN_HEADER, token).body(body)
                    .retrieve().toBodilessEntity();
            log.info("Created {}", what);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.debug("{} already present", what);
        }
    }
}
