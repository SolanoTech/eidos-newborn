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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Потребитель событий о согласиях.
 *
 * <p>Сейчас обрабатываются только согласия на обработку персональных данных:
 * остальные типы (маркетинг, профилирование, передача третьим лицам) на
 * Золотую запись пока не влияют и молча пропускаются.</p>
 *
 * <p>Тип события ({@code new} / {@code expired}) не разбирается намеренно —
 * оба означают «состояние клиента изменилось, сверься». Что именно теперь
 * верно, решает {@link ConsentStateService} по ответу сервиса согласий.</p>
 *
 * <p>Сообщение, которое не удалось разобрать, логируется и пропускается: одно
 * битое сообщение не должно останавливать партицию. А вот недоступность
 * сервиса согласий — повод не подтверждать смещение и повторить позже, поэтому
 * такое исключение пробрасывается наружу.</p>
 */
@Component
public class ConsentEventListener {

    private static final Logger log = LoggerFactory.getLogger(ConsentEventListener.class);

    private final ObjectMapper objectMapper;
    private final ConsentStateService consentStateService;

    public ConsentEventListener(ObjectMapper objectMapper, ConsentStateService consentStateService) {
        this.objectMapper = objectMapper;
        this.consentStateService = consentStateService;
    }

    @KafkaListener(
            topics = "${eidos.consents.events.topic}",
            groupId = "${eidos.consents.events.group-id}",
            autoStartup = "${eidos.consents.events.enabled:true}")
    public void onConsentEvent(String payload) {
        ConsentEvent event;
        try {
            event = objectMapper.readValue(payload, ConsentEvent.class);
        } catch (Exception e) {
            log.error("Skipping malformed consent event: {}", payload, e);
            return;
        }
        if (event.clientUuid() == null) {
            log.error("Skipping consent event without client_uuid: {}", payload);
            return;
        }
        if (!event.isPersonalData()) {
            log.debug("Ignoring {} consent event of type {}", event.eventType(), event.type());
            return;
        }
        consentStateService.reconcile(event.clientUuid());
    }
}
