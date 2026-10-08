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

package com.solano.eidoscore.consent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Событие о согласии из топика сервиса согласий.
 *
 * <p>Событие используется как <b>повод проверить</b>, а не как источник истины:
 * из него берутся только идентификатор клиента и тип согласия, а текущее
 * состояние core спрашивает у сервиса согласий. Так повторная доставка и
 * переупорядочивание перестают иметь значение.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsentEvent(
        @JsonProperty("event_type") String eventType,
        @JsonProperty("client_uuid") UUID clientUuid,
        @JsonProperty("type") String type
) {

    /** Короткий код согласия на обработку персональных данных. */
    public static final String PERSONAL_DATA = "pd";

    public boolean isPersonalData() {
        return PERSONAL_DATA.equals(type);
    }
}
