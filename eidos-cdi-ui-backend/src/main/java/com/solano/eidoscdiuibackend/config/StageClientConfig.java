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

package com.solano.eidoscdiuibackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * HTTP-клиент к admin API eidos-stage. Базовый URL и admin-токен берутся из
 * конфигурации; токен подставляется в заголовок {@code X-Admin-Token} ко всем
 * запросам. UI-backend выступает доверенным посредником: наружу доступ
 * ограничен ролью ADMIN, внутрь — статичным admin-токеном stage.
 */
@Configuration
public class StageClientConfig {

    @Bean
    public RestClient stageRestClient(
            @Value("${eidos.stage.base-url}") String baseUrl,
            @Value("${eidos.stage.admin-token}") String adminToken) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Admin-Token", adminToken)
                .build();
    }
}
