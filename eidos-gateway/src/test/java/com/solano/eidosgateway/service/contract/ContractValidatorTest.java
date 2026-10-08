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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContractValidatorTest {

    private final ContractValidator validator = new ContractValidator();

    @Test
    void validData_passesWithNoViolations() {
        SourceContract contract = contract(
                field("phone", FieldDataType.STRING, true, null, "^998\\d{9}$", null),
                field("bdate", FieldDataType.DATE, true, "dd.MM.yyyy", null, null),
                field("sex", FieldDataType.GENDER, true, null, null, Map.of("1", "M", "2", "F")));

        List<String> violations = validator.validate(
                Map.of("phone", "998901234567", "bdate", "15.06.1990", "sex", "1"), contract);

        assertThat(violations).isEmpty();
    }

    @Test
    void missingRequiredField_reported() {
        SourceContract contract = contract(field("phone", FieldDataType.STRING, true, null, null, null));
        List<String> violations = validator.validate(Map.of(), contract);
        assertThat(violations).anyMatch(v -> v.contains("missing required field: phone"));
    }

    @Test
    void regexMismatch_reported() {
        SourceContract contract = contract(field("phone", FieldDataType.STRING, true, null, "^998\\d{9}$", null));
        List<String> violations = validator.validate(Map.of("phone", "12345"), contract);
        assertThat(violations).anyMatch(v -> v.contains("does not match required format"));
    }

    @Test
    void badDate_reported() {
        SourceContract contract = contract(field("bdate", FieldDataType.DATE, true, "dd.MM.yyyy", null, null));
        List<String> violations = validator.validate(Map.of("bdate", "1990-06-15"), contract);
        assertThat(violations).anyMatch(v -> v.contains("not a valid date"));
    }

    @Test
    void valueNotInValueMap_reported() {
        SourceContract contract = contract(field("sex", FieldDataType.GENDER, true, null, null, Map.of("1", "M", "2", "F")));
        List<String> violations = validator.validate(Map.of("sex", "9"), contract);
        assertThat(violations).anyMatch(v -> v.contains("not allowed"));
    }

    @Test
    void nestedDotPath_isResolved() {
        SourceContract contract = contract(field("person.phone", FieldDataType.STRING, true, null, "^998\\d{9}$", null));
        List<String> violations = validator.validate(
                Map.of("person", Map.of("phone", "998901234567")), contract);
        assertThat(violations).isEmpty();
    }

    private static SourceContract contract(SourceField... fields) {
        SourceContract contract = new SourceContract();
        contract.setFields(new ArrayList<>(List.of(fields)));
        return contract;
    }

    private static SourceField field(String name, FieldDataType type, boolean required,
                                     String dateFormat, String regex, Map<String, String> valueMap) {
        SourceField field = new SourceField();
        field.setSourceFieldName(name);
        field.setTargetGrField("GR_x");
        field.setDataType(type);
        field.setRequired(required);
        field.setSourceDateFormat(dateFormat);
        field.setValidationRegex(regex);
        if (valueMap != null) {
            field.setValueMap(valueMap);
        }
        return field;
    }
}
