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

package com.solano.eidosgateway.auth;

import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.exception.UnauthorizedException;
import com.solano.eidosgateway.repository.registry.SourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Авторизация источников по токену {@code X-Access-Token}. Единая точка для
 * REST-фильтра и Kafka-консьюмера. Источники читаются из единого реестра.
 */
@Service
public class TokenAuthService {

    /** Имя заголовка токена доступа — общее для HTTP и Kafka. */
    public static final String ACCESS_TOKEN_HEADER = "X-Access-Token";

    private final SourceRepository sourceRepository;

    public TokenAuthService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    /**
     * Проверяет токен и возвращает соответствующий источник.
     *
     * @throws UnauthorizedException если токен пуст или не найден
     */
    @Transactional(transactionManager = "registryTransactionManager", readOnly = true)
    public Source authorize(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Missing " + ACCESS_TOKEN_HEADER);
        }
        return sourceRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid access token"));
    }
}
