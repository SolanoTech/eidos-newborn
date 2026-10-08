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

package com.solano.eidosgateway.kafka;

import com.solano.eidosgateway.auth.TokenAuthService;
import com.solano.eidosgateway.entity.registry.EntityType;
import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.entity.registry.SourceContract;
import com.solano.eidosgateway.exception.ContractValidationException;
import com.solano.eidosgateway.forward.StageClient;
import com.solano.eidosgateway.repository.registry.SourceContractRepository;
import com.solano.eidosgateway.service.contract.ContractValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Обработка одного Kafka-сообщения Gateway: авторизация по токену, валидация
 * данных против дата-контракта источника и форвард карточки клиента в
 * eidos-stage. Вынесено отдельно от инфраструктуры контейнера ради
 * юнит-тестируемости.
 */
@Component
public class GatewayMessageHandler {

    private final TokenAuthService tokenAuthService;
    private final SourceContractRepository contractRepository;
    private final ContractValidator contractValidator;
    private final ObjectMapper objectMapper;
    private final StageClient stageClient;
    private final com.solano.eidosgateway.service.stats.RejectStatsService rejectStats;

    public GatewayMessageHandler(
            TokenAuthService tokenAuthService,
            SourceContractRepository contractRepository,
            ContractValidator contractValidator,
            ObjectMapper objectMapper,
            StageClient stageClient,
            com.solano.eidosgateway.service.stats.RejectStatsService rejectStats
    ) {
        this.tokenAuthService = tokenAuthService;
        this.contractRepository = contractRepository;
        this.contractValidator = contractValidator;
        this.objectMapper = objectMapper;
        this.stageClient = stageClient;
        this.rejectStats = rejectStats;
    }

    /**
     * Авторизует токен (из заголовка {@code X-Access-Token} сообщения), валидирует
     * данные против контракта источника и, если всё в порядке, пересылает тело в
     * stage. Любая ошибка → исключение (консьюмер залогирует и пропустит сообщение).
     */
    public void handle(String accessToken, byte[] payload) {
        handle(accessToken, payload, EntityType.PERSON);
    }

    /**
     * @param entityType тип сущности, заданный <b>топиком</b>, из которого
     *                   пришло сообщение: источник его не присылает
     */
    public void handle(String accessToken, byte[] payload, EntityType entityType) {
        Source source = tokenAuthService.authorize(accessToken);
        SourceContract contract = contractRepository
                .findBySourceCodeAndEntityType(source.getCode(), entityType)
                .orElseThrow(() -> {
                    rejectStats.recordRejected(source.getCode());
                    return new ContractValidationException(
                            "No " + entityType + " data contract configured for source "
                                    + source.getCode(), List.of());
                });

        Map<String, Object> envelope = parseEnvelope(source.getCode(), payload);
        Map<String, Object> dataNode = extractData(envelope);

        List<String> violations = contractValidator.validate(dataNode, contract);
        if (!violations.isEmpty()) {
            rejectStats.recordRejected(source.getCode());
            throw new ContractValidationException("Client data failed contract validation: " + violations, violations);
        }

        // Источник — авторитетно из токена, а не из тела продюсера. Проставляем на
        // верхнем уровне конверта {data, source}, который читает eidos-stage, и
        // пересылаем ИЗМЕНЁННЫЕ байты (не исходный payload).
        envelope.put("source", source.getCode());
        byte[] forwarded = objectMapper.writeValueAsBytes(envelope);
        switch (entityType) {
            case PERSON -> stageClient.forwardClientData(forwarded);
            case LEGAL_ENTITY -> stageClient.forwardLegalData(forwarded);
        }
    }

    /** Разбирает конверт в изменяемый map; невалидный JSON → отклонение. */
    private Map<String, Object> parseEnvelope(String sourceCode, byte[] payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> envelope = objectMapper.readValue(payload, Map.class);
            return envelope;
        } catch (Exception e) {
            rejectStats.recordRejected(sourceCode);
            throw new ContractValidationException("Malformed client-data body: " + e.getMessage(), List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> envelope) {
        Object data = envelope.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }
}
