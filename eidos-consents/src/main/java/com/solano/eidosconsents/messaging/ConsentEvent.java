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

package com.solano.eidosconsents.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.solano.eidosconsents.entity.Consent;
import com.solano.eidosconsents.entity.ConsentType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Событие о согласии клиента, публикуемое в Kafka.
 *
 * <p>Имена полей snake_case — как в остальных JSON сервиса согласий;
 * {@code type} сериализуется коротким кодом ({@code pd}, {@code bio}, …) за
 * счёт {@link ConsentType#getCode()}.</p>
 *
 * <p>{@code client_uuid} — идентификатор Золотой записи клиента: по нему
 * потребитель находит карточку, которой касается событие.</p>
 */
public record ConsentEvent(
        @JsonProperty("event_type") ConsentEventType eventType,
        @JsonProperty("client_uuid") UUID clientUuid,
        @JsonProperty("type") ConsentType type,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate
) {

    public static ConsentEvent of(ConsentEventType eventType, Consent consent) {
        return new ConsentEvent(
                eventType,
                consent.getClientUuid(),
                consent.getType(),
                consent.getStartDate(),
                consent.getEndDate());
    }

    /** Ключ сообщения: события одного клиента идут в одну партицию по порядку. */
    public String partitionKey() {
        return clientUuid.toString();
    }
}
