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

package com.solano.eidosgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Базовый {@link RestClient} для проксирования и форварда в downstream-сервисы.
 * Без baseUrl — вызовы используют абсолютные URI, вычисленные RouteResolver'ом.
 */
@Configuration
public class RestClientConfig {

    /**
     * Явный бин строителя: в Spring Boot 4 при стартере webmvc
     * {@code RestClient.Builder} не автоконфигурируется, поэтому предоставляем его сами.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient gatewayRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
