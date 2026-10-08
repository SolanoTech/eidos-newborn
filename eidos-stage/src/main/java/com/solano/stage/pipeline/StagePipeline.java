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

package com.solano.stage.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.solano.shared.dto.GoldenRecordDto;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import com.solano.stage.core.EidosCoreClient;
import com.solano.stage.core.EidosCoreException;
import com.solano.stage.entity.registry.EntityType;
import com.solano.stage.entity.registry.SourceContract;
import com.solano.stage.messaging.IngestMessage;
import com.solano.stage.mongo.RawClientDataRepository;
import com.solano.stage.repository.registry.SourceContractRepository;
import com.solano.stage.service.contract.ContractMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ядро обработки одного сообщения с данными источника:
 *
 * <ol>
 *   <li>Сохранить сырые данные в MongoDB «как есть» (всегда, первым шагом).</li>
 *   <li>Загрузить дата-контракт (конструктор) источника для нужного типа
 *       сущности из реестра.</li>
 *   <li>Преобразовать {@code data} в целевую Золотую запись через
 *       {@link ContractMapper}.</li>
 *   <li>Провалидировать обязательные поля / форматы (bean validation — финальная
 *       страховка; основная схемная валидация выполняется на gateway).</li>
 *   <li>Отправить результат в eidos-core.</li>
 * </ol>
 *
 * <p><b>Тип сущности приходит не из данных, а из канала:</b> у физлиц и юрлиц
 * разные эндпоинты и разные топики Kafka. Источник в своём payload тип не
 * указывает и указать не может.</p>
 *
 * <p>Любая ошибка оборачивается в {@link StageProcessingException}; REST-слой
 * транслирует её в ответ 422 вызывающей стороне. Сырьё к этому моменту уже
 * сохранено в Mongo (шаг 1 — первый).</p>
 */
@Service
public class StagePipeline {

    private static final Logger log = LoggerFactory.getLogger(StagePipeline.class);

    private final RawClientDataRepository rawRepository;
    private final SourceContractRepository sourceContractRepository;
    private final ContractMapper contractMapper;
    private final Validator validator;
    private final EidosCoreClient coreClient;
    private final com.solano.stage.service.stats.IngestStatsService ingestStats;

    public StagePipeline(
            RawClientDataRepository rawRepository,
            SourceContractRepository sourceContractRepository,
            ContractMapper contractMapper,
            Validator validator,
            EidosCoreClient coreClient,
            com.solano.stage.service.stats.IngestStatsService ingestStats
    ) {
        this.rawRepository = rawRepository;
        this.sourceContractRepository = sourceContractRepository;
        this.contractMapper = contractMapper;
        this.validator = validator;
        this.coreClient = coreClient;
        this.ingestStats = ingestStats;
    }

    /** Обработка карточки физлица (канал {@code /api/v1/client-data}). */
    public void process(IngestMessage message) {
        process(message, EntityType.PERSON);
    }

    public void process(IngestMessage message, EntityType entityType) {
        String source = message.getSource();
        JsonNode data = message.getData();

        if (source == null || source.isBlank()) {
            throw new StageProcessingException("Message has no 'source'");
        }
        if (data == null || data.isNull() || data.isMissingNode()) {
            throw new StageProcessingException("Message has no 'data' for source=" + source);
        }

        // Шаг 1: сырьё в Mongo (как есть).
        persistRaw(source, data, entityType);

        try {
            // Шаг 2: дата-контракт источника для этого типа сущности.
            SourceContract contract = sourceContractRepository
                    .findBySourceCodeAndEntityType(source, entityType)
                    .orElseThrow(() -> new StageProcessingException(
                            "No data contract configured for source=" + source + " type=" + entityType));

            // Шаги 3–5: маппинг, валидация, отправка — по типу сущности.
            String clientId = switch (entityType) {
                case PERSON -> processPerson(source, data, contract);
                case LEGAL_ENTITY -> processLegalEntity(source, data, contract);
            };

            ingestStats.recordAccepted(source);
            log.info("Processed {} message source={} clientSourceIdentificator={}",
                    entityType, source, clientId);
        } catch (StageProcessingException e) {
            // Отклонено на этапе контракта/маппинга/валидации/отправки — в статистику.
            ingestStats.recordRejected(source);
            throw e;
        }
    }

    private String processPerson(String source, JsonNode data, SourceContract contract) {
        ContractMapper.MappedRecord<GoldenRecordDto> mapped =
                mapToRecord(source, data, contract, GoldenRecordDto.class);
        validate(source, mapped.record());
        try {
            coreClient.saveGoldenRecord(mapped.record(), source, mapped.clientSourceIdentificator());
        } catch (EidosCoreException e) {
            throw new StageProcessingException("eidos-core rejected record for source=" + source, e);
        }
        return mapped.clientSourceIdentificator();
    }

    private String processLegalEntity(String source, JsonNode data, SourceContract contract) {
        ContractMapper.MappedRecord<LegalEntityGoldenRecordDto> mapped =
                mapToRecord(source, data, contract, LegalEntityGoldenRecordDto.class);
        validate(source, mapped.record());
        try {
            coreClient.saveLegalEntity(mapped.record(), source, mapped.clientSourceIdentificator());
        } catch (EidosCoreException e) {
            throw new StageProcessingException("eidos-core rejected legal entity for source=" + source, e);
        }
        return mapped.clientSourceIdentificator();
    }

    private void persistRaw(String source, JsonNode data, EntityType entityType) {
        try {
            rawRepository.save(source, data, entityType.name());
        } catch (Exception e) {
            throw new StageProcessingException("Failed to persist raw data for source=" + source, e);
        }
    }

    private <T> ContractMapper.MappedRecord<T> mapToRecord(
            String source, JsonNode data, SourceContract contract, Class<T> targetType) {
        try {
            return contractMapper.map(data, contract, targetType);
        } catch (Exception e) {
            throw new StageProcessingException("Mapping to GoldenRecord failed for source=" + source, e);
        }
    }

    private <T> void validate(String source, T record) {
        Set<ConstraintViolation<T>> violations = validator.validate(record);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new StageProcessingException(
                    "GoldenRecord validation failed for source=" + source + ": " + details);
        }
    }
}
