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

package com.solano.stage.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Тело запроса приёма Золотой записи в eidos-core ({@code golden-records}
 * для физлиц, {@code legal-records} для юрлиц). Имена полей совпадают с
 * {@code PostGoldenRecordRequest}/{@code PostLegalEntityRequest} на стороне core;
 * тип записи задаётся эндпоинтом, а не полем.
 */
@Data
@AllArgsConstructor
public class CoreSaveRequest {

    @JsonProperty("record")
    private Object record;

    @JsonProperty("source")
    private String source;

    @JsonProperty("client_source_identificator")
    private String clientSourceIdentificator;
}
