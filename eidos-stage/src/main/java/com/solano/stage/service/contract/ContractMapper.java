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

package com.solano.stage.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solano.shared.dto.GoldenRecordDto;
import com.solano.stage.entity.registry.FieldDataType;
import com.solano.stage.entity.registry.SourceContract;
import com.solano.stage.entity.registry.SourceField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Преобразует «сырые» данные источника в {@link GoldenRecordDto} по дата-контракту
 * (конструктору). На каждое поле контракта: читает значение по пути в исходных
 * данных, применяет value-map, приводит к формату Золотой записи (даты — к
 * каноническому {@code yyyy-MM-dd}, пол — к {@code M}/{@code F}), подставляет
 * значение по умолчанию. Результат собирается в карту по JSON-именам полей и
 * один раз конвертируется Jackson'ом в {@link GoldenRecordDto}.
 *
 * <p>Это динамическая замена прежним кодовым контрактам
 * ({@code StandardSourceContract} и наследникам).</p>
 */
@Component
public class ContractMapper {

    private static final DateTimeFormatter CANONICAL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper;

    public ContractMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Результат маппинга: Золотая запись и идентификатор клиента в источнике. */
    public record MappedRecord<T>(T record, String clientSourceIdentificator) {
    }

    /**
     * Преобразует данные источника в целевую Золотую запись указанного типа.
     *
     * @param targetType контракт целевой записи: {@code GoldenRecordDto} для
     *                   физлиц, {@code LegalEntityGoldenRecordDto} для юрлиц —
     *                   выбирается каналом приёма, а не самим источником
     */
    public <T> MappedRecord<T> map(JsonNode data, SourceContract contract, Class<T> targetType) {
        Map<String, Object> output = new LinkedHashMap<>();

        for (SourceField field : contract.getFields()) {
            Object value = resolveFieldValue(data, field);
            if (value != null) {
                output.put(field.getTargetGrField(), value);
            }
        }

        String clientId = contract.getClientIdentifierField() == null
                ? null
                : readText(data, contract.getClientIdentifierField());

        T record = objectMapper.convertValue(output, targetType);
        return new MappedRecord<>(record, clientId);
    }

    /** Обратная совместимость: маппинг в контракт физлица. */
    public MappedRecord<GoldenRecordDto> map(JsonNode data, SourceContract contract) {
        return map(data, contract, GoldenRecordDto.class);
    }

    /**
     * Значение поля: массивы и структуры переносятся поддеревом JSON как есть,
     * скаляры проходят value-map и приведение типа.
     */
    private Object resolveFieldValue(JsonNode data, SourceField field) {
        if (field.getDataType() == FieldDataType.ARRAY || field.getDataType() == FieldDataType.STRUCT) {
            JsonNode node = readNode(data, field.getSourceFieldName());
            return node == null || node.isNull() || node.isMissingNode() ? null : node;
        }
        return resolveValue(field, readText(data, field.getSourceFieldName()));
    }

    /**
     * Вычисляет целевое значение поля: value-map → приведение типа; при
     * отсутствии исходного значения — default.
     */
    private Object resolveValue(SourceField field, String raw) {
        if (raw == null || raw.isBlank()) {
            return field.getDefaultValue(); // null или константа по умолчанию
        }
        String mapped = applyValueMap(field, raw);
        return coerce(field, mapped);
    }

    private String applyValueMap(SourceField field, String raw) {
        Map<String, String> valueMap = field.getValueMap();
        if (valueMap != null && valueMap.containsKey(raw)) {
            return valueMap.get(raw);
        }
        return raw;
    }

    private Object coerce(SourceField field, String value) {
        return switch (field.getDataType()) {
            case DATE -> toCanonicalDate(value, field.getSourceDateFormat());
            case STRING, INTEGER, BOOLEAN, GENDER -> value;
            // ARRAY/STRUCT сюда не доходят — они переносятся поддеревом JSON.
            case ARRAY, STRUCT -> value;
        };
    }

    /** Приводит дату из формата источника к каноническому {@code yyyy-MM-dd}. */
    private String toCanonicalDate(String value, String sourceFormat) {
        DateTimeFormatter parser = (sourceFormat == null || sourceFormat.isBlank())
                ? CANONICAL_DATE
                : DateTimeFormatter.ofPattern(sourceFormat);
        LocalDate date = LocalDate.parse(value, parser);
        return date.format(CANONICAL_DATE);
    }

    /** Читает узел по dot-path ({@code a.b.c}); {@code null}, если пути нет. */
    private JsonNode readNode(JsonNode data, String path) {
        if (data == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode node = data;
        for (String segment : path.split("\\.")) {
            node = node.path(segment);
        }
        return node;
    }

    /** Читает текстовое значение по dot-path ({@code a.b.c}) из дерева JSON. */
    private String readText(JsonNode data, String path) {
        if (data == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode node = data;
        for (String segment : path.split("\\.")) {
            node = node.path(segment);
        }
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
