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

import com.solano.shared.dto.RecordFieldCatalog;

/** Доступное целевое поле Золотой записи для UI конструктора. */
public record GoldenRecordFieldView(
        String jsonName,
        String javaName,
        String type,
        boolean required,
        String pattern
) {

    public static GoldenRecordFieldView fromDescriptor(RecordFieldCatalog.FieldDescriptor d) {
        return new GoldenRecordFieldView(
                d.jsonName(), d.javaName(), d.type().name(), d.required(), d.pattern());
    }
}
