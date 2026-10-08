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
import com.solano.eidosconsents.entity.ConsentScanState;
import com.solano.eidosconsents.entity.ConsentType;
import com.solano.eidosconsents.repository.ConsentRepository;
import com.solano.eidosconsents.repository.ConsentScanStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Планировщик излучает {@code expired}, когда клиент потерял согласие типа:
 * срок закончился вчера и действующей замены нет. Сам отбор «замены» делает
 * запрос репозитория, здесь проверяется поведение вокруг него.
 */
@ExtendWith(MockitoExtension.class)
class ConsentExpiryScannerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 21);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    private static final int MAX_CATCH_UP_DAYS = 30;

    @Mock ConsentRepository consentRepository;
    @Mock ConsentScanStateRepository scanStateRepository;
    @Mock ConsentEventPublisher publisher;

    private ConsentExpiryScanner scanner;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        scanner = new ConsentExpiryScanner(
                consentRepository, scanStateRepository, publisher, fixedClock, MAX_CATCH_UP_DAYS);
    }

    @Test
    void scheduledRun_asksForYesterdaysExpiries_onTodaysActiveness() {
        when(consentRepository.findExpiredWithoutActiveConsent(eq(YESTERDAY), eq(TODAY)))
                .thenReturn(List.of());

        scanner.scanToday();

        verify(consentRepository).findExpiredWithoutActiveConsent(YESTERDAY, TODAY);
        verify(publisher, never()).send(any());
    }

    @Test
    void publishesExpiredEvent_carryingTheConsentDates() {
        Consent consent = consent(UUID.randomUUID(), ConsentType.PERSONAL_DATA,
                LocalDate.of(2026, 2, 21), YESTERDAY);
        when(consentRepository.findExpiredWithoutActiveConsent(eq(YESTERDAY), eq(TODAY)))
                .thenReturn(List.of(consent));

        assertThat(scanner.publishExpiredOn(TODAY)).isEqualTo(1);

        ArgumentCaptor<ConsentEvent> captor = ArgumentCaptor.forClass(ConsentEvent.class);
        verify(publisher).send(captor.capture());

        ConsentEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(ConsentEventType.EXPIRED);
        assertThat(event.clientUuid()).isEqualTo(consent.getClientUuid());
        assertThat(event.type()).isEqualTo(ConsentType.PERSONAL_DATA);
        assertThat(event.startDate()).isEqualTo(LocalDate.of(2026, 2, 21));
        assertThat(event.endDate()).isEqualTo(YESTERDAY);
    }

    @Test
    void severalConsentsOfOneTypeEndingTogether_giveOneEvent() {
        UUID client = UUID.randomUUID();
        // Запрос отдаёт самое позднее согласие пары первым — его даты и ждём в событии.
        when(consentRepository.findExpiredWithoutActiveConsent(eq(YESTERDAY), eq(TODAY)))
                .thenReturn(List.of(
                        consent(client, ConsentType.PERSONAL_DATA, LocalDate.of(2026, 5, 1), YESTERDAY),
                        consent(client, ConsentType.PERSONAL_DATA, LocalDate.of(2026, 2, 21), YESTERDAY)));

        assertThat(scanner.publishExpiredOn(TODAY)).isEqualTo(1);

        ArgumentCaptor<ConsentEvent> captor = ArgumentCaptor.forClass(ConsentEvent.class);
        verify(publisher).send(captor.capture());
        assertThat(captor.getValue().startDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void differentTypesOfOneClient_areSeparateEvents() {
        UUID client = UUID.randomUUID();
        when(consentRepository.findExpiredWithoutActiveConsent(eq(YESTERDAY), eq(TODAY)))
                .thenReturn(List.of(
                        consent(client, ConsentType.PERSONAL_DATA, LocalDate.of(2026, 2, 21), YESTERDAY),
                        consent(client, ConsentType.BIO, LocalDate.of(2026, 2, 21), YESTERDAY)));

        assertThat(scanner.publishExpiredOn(TODAY)).isEqualTo(2);
        verify(publisher, times(2)).send(any());
    }

    @Test
    void differentClients_eachGetTheirOwnEvent() {
        when(consentRepository.findExpiredWithoutActiveConsent(eq(YESTERDAY), eq(TODAY)))
                .thenReturn(List.of(
                        consent(UUID.randomUUID(), ConsentType.PERSONAL_DATA,
                                LocalDate.of(2026, 2, 21), YESTERDAY),
                        consent(UUID.randomUUID(), ConsentType.PERSONAL_DATA,
                                LocalDate.of(2026, 2, 21), YESTERDAY)));

        assertThat(scanner.publishExpiredOn(TODAY)).isEqualTo(2);
        verify(publisher, times(2)).send(any());
    }

    @Test
    void firstRunEver_scansOnlyToday_andRemembersIt() {
        when(scanStateRepository.findById(ConsentScanState.EXPIRY)).thenReturn(Optional.empty());

        scanner.scanToday();

        verify(consentRepository).findExpiredWithoutActiveConsent(YESTERDAY, TODAY);
        verify(consentRepository, times(1)).findExpiredWithoutActiveConsent(any(), any());
        verifySavedLastScanned(TODAY);
    }

    @Test
    void alreadyScannedToday_doesNothing() {
        when(scanStateRepository.findById(ConsentScanState.EXPIRY))
                .thenReturn(Optional.of(new ConsentScanState(ConsentScanState.EXPIRY, TODAY)));

        scanner.scanToday();

        verify(consentRepository, never()).findExpiredWithoutActiveConsent(any(), any());
        verify(scanStateRepository, never()).save(any());
    }

    @Test
    void missedDays_areCaughtUpInOrder_eachRememberedAfterItsEvents() {
        when(scanStateRepository.findById(ConsentScanState.EXPIRY))
                .thenReturn(Optional.of(new ConsentScanState(ConsentScanState.EXPIRY, TODAY.minusDays(3))));
        Consent missed = consent(UUID.randomUUID(), ConsentType.PERSONAL_DATA,
                LocalDate.of(2026, 2, 1), TODAY.minusDays(3));
        when(consentRepository.findExpiredWithoutActiveConsent(any(), any())).thenReturn(List.of());
        when(consentRepository.findExpiredWithoutActiveConsent(TODAY.minusDays(3), TODAY.minusDays(2)))
                .thenReturn(List.of(missed));

        scanner.scanToday();

        InOrder order = inOrder(consentRepository, publisher, scanStateRepository);
        order.verify(consentRepository).findExpiredWithoutActiveConsent(TODAY.minusDays(3), TODAY.minusDays(2));
        order.verify(publisher).send(any());
        order.verify(scanStateRepository).save(argThatLastScanned(TODAY.minusDays(2)));
        order.verify(consentRepository).findExpiredWithoutActiveConsent(TODAY.minusDays(2), TODAY.minusDays(1));
        order.verify(scanStateRepository).save(argThatLastScanned(TODAY.minusDays(1)));
        order.verify(consentRepository).findExpiredWithoutActiveConsent(YESTERDAY, TODAY);
        order.verify(scanStateRepository).save(argThatLastScanned(TODAY));
    }

    @Test
    void longOutage_isCaughtUpOnlyToTheConfiguredDepth() {
        when(scanStateRepository.findById(ConsentScanState.EXPIRY))
                .thenReturn(Optional.of(new ConsentScanState(ConsentScanState.EXPIRY, TODAY.minusDays(100))));
        when(consentRepository.findExpiredWithoutActiveConsent(any(), any())).thenReturn(List.of());

        scanner.scanToday();

        verify(consentRepository, times(MAX_CATCH_UP_DAYS + 1)).findExpiredWithoutActiveConsent(any(), any());
        verify(consentRepository).findExpiredWithoutActiveConsent(
                TODAY.minusDays(MAX_CATCH_UP_DAYS + 1), TODAY.minusDays(MAX_CATCH_UP_DAYS));
    }

    @Test
    void sendFailure_stopsTheRun_andLeavesTheDayForTheNextOne() {
        LocalDate lastScanned = TODAY.minusDays(2);
        when(scanStateRepository.findById(ConsentScanState.EXPIRY))
                .thenReturn(Optional.of(new ConsentScanState(ConsentScanState.EXPIRY, lastScanned)));
        Consent consent = consent(UUID.randomUUID(), ConsentType.PERSONAL_DATA,
                LocalDate.of(2026, 2, 1), lastScanned);
        when(consentRepository.findExpiredWithoutActiveConsent(lastScanned, TODAY.minusDays(1)))
                .thenReturn(List.of(consent));
        doThrow(new ConsentEventSendException(ConsentEvent.of(ConsentEventType.EXPIRED, consent),
                new RuntimeException("broker down")))
                .when(publisher).send(any());

        scanner.scanToday();

        verify(scanStateRepository, never()).save(any());
        verify(consentRepository, never()).findExpiredWithoutActiveConsent(YESTERDAY, TODAY);
    }

    @Test
    void startupRun_doesNotFailTheApplication() {
        when(scanStateRepository.findById(ConsentScanState.EXPIRY))
                .thenThrow(new IllegalStateException("database is down"));

        scanner.scanOnStartup();

        verify(consentRepository, never()).findExpiredWithoutActiveConsent(any(), any());
    }

    private void verifySavedLastScanned(LocalDate date) {
        verify(scanStateRepository).save(argThatLastScanned(date));
    }

    private static ConsentScanState argThatLastScanned(LocalDate date) {
        return argThat(state -> ConsentScanState.EXPIRY.equals(state.getName())
                && date.equals(state.getLastScannedDate()));
    }

    private static Consent consent(UUID client, ConsentType type, LocalDate start, LocalDate end) {
        return Consent.builder()
                .id(UUID.randomUUID())
                .clientUuid(client)
                .startDate(start)
                .endDate(end)
                .type(type)
                .source("payme")
                .build();
    }
}
