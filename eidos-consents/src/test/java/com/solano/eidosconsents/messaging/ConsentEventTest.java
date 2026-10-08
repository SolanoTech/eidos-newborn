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

import com.solano.eidosconsents.entity.Consent;
import com.solano.eidosconsents.entity.ConsentType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Формат сообщения на проводе: его читают другие сервисы, поэтому имена полей
 * и коды зафиксированы тестом.
 */
class ConsentEventTest {

    // Jackson 3 умеет java.time из коробки — отдельный модуль не нужен.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesWithSnakeCaseNamesAndShortCodes() {
        UUID client = UUID.fromString("7f4ea435-3e76-4a80-8c39-d35134090b0f");
        Consent consent = Consent.builder()
                .id(UUID.randomUUID())
                .clientUuid(client)
                .startDate(LocalDate.of(2026, 2, 21))
                .endDate(LocalDate.of(2026, 8, 20))
                .type(ConsentType.PERSONAL_DATA)
                .source("payme")
                .build();

        String json = objectMapper.writeValueAsString(ConsentEvent.of(ConsentEventType.NEW, consent));

        assertThat(json)
                .contains("\"event_type\":\"new\"")
                .contains("\"client_uuid\":\"7f4ea435-3e76-4a80-8c39-d35134090b0f\"")
                .contains("\"type\":\"pd\"")
                .contains("\"start_date\":\"2026-02-21\"")
                .contains("\"end_date\":\"2026-08-20\"");
    }

    @Test
    void expiredEventUsesItsOwnCode() {
        Consent consent = Consent.builder()
                .id(UUID.randomUUID())
                .clientUuid(UUID.randomUUID())
                .startDate(LocalDate.of(2026, 2, 21))
                .endDate(LocalDate.of(2026, 8, 20))
                .type(ConsentType.BIO)
                .source("payme")
                .build();

        ConsentEvent event = ConsentEvent.of(ConsentEventType.EXPIRED, consent);

        assertThat(objectMapper.writeValueAsString(event))
                .contains("\"event_type\":\"expired\"")
                .contains("\"type\":\"bio\"");
        assertThat(event.partitionKey()).isEqualTo(consent.getClientUuid().toString());
    }
}
