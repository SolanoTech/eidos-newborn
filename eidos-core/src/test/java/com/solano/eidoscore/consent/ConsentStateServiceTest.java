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

import com.solano.eidoscore.repository.GoldenRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Сверка состояния согласия: событие — только повод, истина берётся у сервиса
 * согласий, а идентификатор Золотой записи ищется в обеих формах.
 */
@ExtendWith(MockitoExtension.class)
class ConsentStateServiceTest {

    private static final UUID CLIENT = UUID.fromString("7f4ea435-3e76-4a80-8c39-d35134090b0f");
    private static final String PREFIXED = "GR_7f4ea435-3e76-4a80-8c39-d35134090b0f";
    private static final String BARE = "7f4ea435-3e76-4a80-8c39-d35134090b0f";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 27, 12, 0);

    @Mock GoldenRecordRepository goldenRecordRepository;
    @Mock ConsentsClient consentsClient;
    @Mock com.solano.eidoscore.crypto.DepersonalizationService depersonalizationService;

    private ConsentStateService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new ConsentStateService(
                goldenRecordRepository, consentsClient, depersonalizationService, fixed);
    }

    @Test
    void activeConsent_isStoredWithItsEndDate() {
        when(goldenRecordRepository.existsById(PREFIXED)).thenReturn(true);
        when(consentsClient.activePersonalDataConsent(CLIENT))
                .thenReturn(Optional.of(new ActiveConsent(LocalDate.of(2026, 3, 1), LocalDate.of(2027, 2, 16))));

        assertThat(service.reconcile(CLIENT)).isTrue();

        verify(goldenRecordRepository).updateConsentState(
                PREFIXED, true, LocalDate.of(2027, 2, 16), NOW);
    }

    @Test
    void noActiveConsent_isStoredAsInactive() {
        when(goldenRecordRepository.existsById(PREFIXED)).thenReturn(true);
        when(consentsClient.activePersonalDataConsent(CLIENT)).thenReturn(Optional.empty());

        service.reconcile(CLIENT);

        verify(goldenRecordRepository).updateConsentState(PREFIXED, false, null, NOW);
    }

    @Test
    void recordStoredWithoutPrefix_isAlsoFound() {
        when(goldenRecordRepository.existsById(PREFIXED)).thenReturn(false);
        when(goldenRecordRepository.existsById(BARE)).thenReturn(true);
        when(consentsClient.activePersonalDataConsent(CLIENT)).thenReturn(Optional.empty());

        service.reconcile(CLIENT);

        verify(goldenRecordRepository).updateConsentState(eq(BARE), eq(false), any(), eq(NOW));
    }

    @Test
    void unknownClient_isSkippedWithoutAskingConsents() {
        when(goldenRecordRepository.existsById(PREFIXED)).thenReturn(false);
        when(goldenRecordRepository.existsById(BARE)).thenReturn(false);

        assertThat(service.reconcile(CLIENT)).isFalse();

        verify(consentsClient, never()).activePersonalDataConsent(any());
        verify(goldenRecordRepository, never()).updateConsentState(any(), any(), any(), any());
    }
}
