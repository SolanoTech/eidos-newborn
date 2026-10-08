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

package com.solano.eidosconsents.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.solano.eidosconsents.entity.Consent;
import com.solano.eidosconsents.entity.ConsentType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JSON-представление согласия в ответах API. Отдельный тип, чтобы контроллеры
 * оставались «тонкими», а сущность не засорялась аннотациями сериализации.
 *
 * <p>{@code type} сериализуется кодом {@code pd}/{@code bio} за счёт
 * {@link ConsentType#getCode()} ({@code @JsonValue}).</p>
 */
public record ConsentView(
        @JsonProperty("id") UUID id,
        @JsonProperty("client_uuid") UUID clientUuid,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("type") ConsentType type,
        @JsonProperty("source") String source
) {

    public static ConsentView fromEntity(Consent consent) {
        return new ConsentView(
                consent.getId(),
                consent.getClientUuid(),
                consent.getStartDate(),
                consent.getEndDate(),
                consent.getType(),
                consent.getSource()
        );
    }
}
