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

package com.solano.stage.web.admin.dto;

import com.solano.stage.entity.registry.FieldDataType;
import com.solano.stage.entity.registry.SourceField;

import java.util.Map;

public record FieldView(
        Long id,
        String sourceFieldName,
        String targetGrField,
        FieldDataType dataType,
        Boolean required,
        String sourceDateFormat,
        String validationRegex,
        String defaultValue,
        Integer ordering,
        Map<String, String> valueMap
) {

    public static FieldView fromEntity(SourceField field) {
        return new FieldView(
                field.getId(),
                field.getSourceFieldName(),
                field.getTargetGrField(),
                field.getDataType(),
                field.getRequired(),
                field.getSourceDateFormat(),
                field.getValidationRegex(),
                field.getDefaultValue(),
                field.getOrdering(),
                field.getValueMap()
        );
    }
}
