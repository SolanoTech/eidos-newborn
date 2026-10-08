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

package com.solano.eidosgateway.service.contract;

import com.solano.eidosgateway.entity.registry.FieldDataType;
import com.solano.eidosgateway.entity.registry.SourceContract;
import com.solano.eidosgateway.entity.registry.SourceField;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Валидирует «сырые» данные источника против его дата-контракта (конструктора).
 * Работает с уже разобранным деревом {@code Map<String,Object>}, чтобы не
 * зависеть от конкретной версии Jackson.
 *
 * <p>Проверки на каждое поле: обязательность, принадлежность value-map,
 * соответствие regex, корректность типа (дата по формату источника, целое,
 * булево). Возвращает список нарушений (пустой = всё хорошо).</p>
 */
@Service
public class ContractValidator {

    public List<String> validate(Map<String, Object> data, SourceContract contract) {
        List<String> violations = new ArrayList<>();
        for (SourceField field : contract.getFields()) {
            validateField(data, field, violations);
        }
        return violations;
    }

    private void validateField(Map<String, Object> data, SourceField field, List<String> violations) {
        String name = field.getSourceFieldName();

        if (field.getDataType() == FieldDataType.ARRAY || field.getDataType() == FieldDataType.STRUCT) {
            validateCollectionField(data, field, violations);
            return;
        }

        String raw = readByPath(data, name);
        boolean present = raw != null && !raw.isBlank();

        if (!present) {
            if (Boolean.TRUE.equals(field.getRequired()) && field.getDefaultValue() == null) {
                violations.add("missing required field: " + name);
            }
            return;
        }

        Map<String, String> valueMap = field.getValueMap();
        if (valueMap != null && !valueMap.isEmpty() && !valueMap.containsKey(raw)) {
            violations.add("value '" + raw + "' is not allowed for field: " + name);
            return;
        }

        String regex = field.getValidationRegex();
        if (regex != null && !regex.isBlank() && !raw.matches(regex)) {
            violations.add("field '" + name + "' does not match required format");
            return;
        }

        switch (field.getDataType()) {
            case DATE -> {
                if (!parsesAsDate(raw, field.getSourceDateFormat())) {
                    violations.add("field '" + name + "' is not a valid date");
                }
            }
            case INTEGER -> {
                if (!parsesAsInteger(raw)) {
                    violations.add("field '" + name + "' is not a valid integer");
                }
            }
            case BOOLEAN -> {
                if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
                    violations.add("field '" + name + "' is not a valid boolean");
                }
            }
            case STRING, GENDER -> {
                // STRING — без доп. проверок; GENDER уже проверен через value-map.
            }
            case ARRAY, STRUCT -> {
                // Коллекции проверяются отдельной веткой выше.
            }
        }
    }

    /**
     * Массивы и структуры: скалярные проверки (regex, формат даты, value-map) к
     * ним неприменимы — проверяем только обязательность и что в данных
     * действительно коллекция, а не одиночное значение.
     */
    private void validateCollectionField(Map<String, Object> data, SourceField field, List<String> violations) {
        String name = field.getSourceFieldName();
        Object value = readObjectByPath(data, name);

        if (value == null) {
            if (Boolean.TRUE.equals(field.getRequired()) && field.getDefaultValue() == null) {
                violations.add("missing required field: " + name);
            }
            return;
        }
        if (!(value instanceof Collection<?>)) {
            violations.add("field '" + name + "' must be an array");
        }
    }

    /** Читает произвольное значение по dot-path ({@code a.b.c}) из дерева. */
    @SuppressWarnings("unchecked")
    private Object readObjectByPath(Map<String, Object> data, String path) {
        if (data == null || path == null || path.isBlank()) {
            return null;
        }
        Object node = data;
        for (String segment : path.split("\\.")) {
            if (!(node instanceof Map)) {
                return null;
            }
            node = ((Map<String, Object>) node).get(segment);
        }
        return node;
    }

    private boolean parsesAsDate(String value, String sourceFormat) {
        DateTimeFormatter formatter = (sourceFormat == null || sourceFormat.isBlank())
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd")
                : DateTimeFormatter.ofPattern(sourceFormat);
        try {
            LocalDate.parse(value, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean parsesAsInteger(String value) {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Читает значение по dot-path ({@code a.b.c}) из дерева вложенных Map. */
    @SuppressWarnings("unchecked")
    private String readByPath(Map<String, Object> data, String path) {
        if (data == null || path == null || path.isBlank()) {
            return null;
        }
        Object node = data;
        for (String segment : path.split("\\.")) {
            if (!(node instanceof Map)) {
                return null;
            }
            node = ((Map<String, Object>) node).get(segment);
        }
        return node == null ? null : node.toString();
    }
}
