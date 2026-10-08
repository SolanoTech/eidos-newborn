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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Разбор сообщения: до сверки доходят только согласия на обработку ПДн, а
 * мусор в топике не останавливает партицию.
 */
@ExtendWith(MockitoExtension.class)
class ConsentEventListenerTest {

    private static final UUID CLIENT = UUID.fromString("7f4ea435-3e76-4a80-8c39-d35134090b0f");

    @Mock ConsentStateService consentStateService;

    private ConsentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ConsentEventListener(new ObjectMapper(), consentStateService);
    }

    @Test
    void expiredPersonalDataEvent_triggersReconcile() {
        listener.onConsentEvent(event("expired", "pd"));
        verify(consentStateService).reconcile(CLIENT);
    }

    @Test
    void newPersonalDataEvent_alsoTriggersReconcile() {
        // Тип события не разбирается: оба означают «сверься».
        listener.onConsentEvent(event("new", "pd"));
        verify(consentStateService).reconcile(CLIENT);
    }

    @Test
    void marketingConsent_isIgnored() {
        listener.onConsentEvent(event("expired", "mk_sms"));
        verify(consentStateService, never()).reconcile(any());
    }

    @Test
    void malformedPayload_isSkippedWithoutThrowing() {
        assertThatCode(() -> listener.onConsentEvent("не json")).doesNotThrowAnyException();
        verify(consentStateService, never()).reconcile(any());
    }

    @Test
    void payloadWithoutClient_isSkipped() {
        assertThatCode(() -> listener.onConsentEvent("{\"event_type\":\"expired\",\"type\":\"pd\"}"))
                .doesNotThrowAnyException();
        verify(consentStateService, never()).reconcile(any());
    }

    private static String event(String eventType, String type) {
        return "{\"event_type\":\"" + eventType + "\",\"client_uuid\":\"" + CLIENT
                + "\",\"type\":\"" + type + "\",\"start_date\":\"2026-03-01\",\"end_date\":\"2026-09-26\"}";
    }
}
