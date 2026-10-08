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

package com.solano.stage.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сообщение из топика {@code client-data}:
 *
 * <pre>{@code
 * {
 *   "data":   { ... данные в формате источника ... },
 *   "source": "source-code"
 * }
 * }</pre>
 *
 * <p>{@code data} держим как «сырой» {@link JsonNode} — он сохраняется в MongoDB
 * как есть и только потом десериализуется в конкретный контракт источника.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestMessage {

    @JsonProperty("data")
    private JsonNode data;

    @JsonProperty("source")
    private String source;
}
