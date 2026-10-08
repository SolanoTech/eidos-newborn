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

package com.solano.eidosconsents.service;

import com.solano.eidosconsents.entity.Consent;
import com.solano.eidosconsents.entity.ConsentType;
import com.solano.eidosconsents.exception.NotFoundException;
import com.solano.eidosconsents.messaging.ConsentEventType;
import com.solano.eidosconsents.messaging.ConsentRevoked;
import com.solano.eidosconsents.repository.ConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Отзыв обрезает срок вчерашним днём и сообщает о себе доменным событием —
 * только если отозванное согласие действовало сегодня.
 */
@ExtendWith(MockitoExtension.class)
class RevokeConsentServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 21);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Mock ConsentRepository consentRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    private RevokeConsentService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        service = new RevokeConsentService(consentRepository, eventPublisher, fixedClock);
    }

    @Test
    void activeConsent_endsYesterday_andAnnouncesTheRevocation() {
        Consent consent = consent(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 12, 31));
        stubFindAndSave(consent);

        Consent revoked = service.execute(consent.getId());

        assertThat(revoked.getEndDate()).isEqualTo(YESTERDAY);
        assertThat(revoked.getRevokedOn()).isEqualTo(TODAY);
        verify(consentRepository).save(consent);

        ArgumentCaptor<ConsentRevoked> captor = ArgumentCaptor.forClass(ConsentRevoked.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ConsentRevoked event = captor.getValue();
        assertThat(event.revokedOn()).isEqualTo(TODAY);
        assertThat(event.expired().eventType()).isEqualTo(ConsentEventType.EXPIRED);
        assertThat(event.expired().clientUuid()).isEqualTo(consent.getClientUuid());
        assertThat(event.expired().type()).isEqualTo(ConsentType.PERSONAL_DATA);
        assertThat(event.expired().startDate()).isEqualTo(LocalDate.of(2026, 2, 21));
        // В событии уже обрезанный срок.
        assertThat(event.expired().endDate()).isEqualTo(YESTERDAY);
    }

    @Test
    void consentStartingToday_isActive_andAnnounced() {
        Consent consent = consent(TODAY, TODAY.plusDays(180));
        stubFindAndSave(consent);

        service.execute(consent.getId());

        verify(eventPublisher).publishEvent(any(ConsentRevoked.class));
    }

    @Test
    void consentEndingToday_isActive_andAnnounced() {
        Consent consent = consent(LocalDate.of(2026, 2, 21), TODAY);
        stubFindAndSave(consent);

        Consent revoked = service.execute(consent.getId());

        assertThat(revoked.getEndDate()).isEqualTo(YESTERDAY);
        verify(eventPublisher).publishEvent(any(ConsentRevoked.class));
    }

    @Test
    void futureConsent_isCutSoItNeverStarts_withoutAnEvent() {
        Consent consent = consent(TODAY.plusDays(10), TODAY.plusDays(190));
        stubFindAndSave(consent);

        Consent revoked = service.execute(consent.getId());

        assertThat(revoked.getEndDate()).isEqualTo(YESTERDAY);
        assertThat(revoked.getRevokedOn()).isEqualTo(TODAY);
        assertThat(revoked.isActiveOn(TODAY.plusDays(10))).isFalse();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void alreadyEndedConsent_isLeftAsIs() {
        // Перенос окончания на «вчера» продлил бы согласие в истории.
        LocalDate end = TODAY.minusDays(30);
        Consent consent = consent(LocalDate.of(2026, 1, 1), end);
        when(consentRepository.findById(consent.getId())).thenReturn(Optional.of(consent));

        Consent result = service.execute(consent.getId());

        assertThat(result.getEndDate()).isEqualTo(end);
        assertThat(result.getRevokedOn()).isNull();
        verify(consentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void secondRevocation_isANoOp() {
        Consent consent = consent(LocalDate.of(2026, 2, 21), LocalDate.of(2026, 12, 31));
        stubFindAndSave(consent);

        service.execute(consent.getId());
        service.execute(consent.getId());

        verify(consentRepository).save(consent);
        verify(eventPublisher).publishEvent(any(ConsentRevoked.class));
    }

    @Test
    void unknownConsent_isNotFound() {
        UUID id = UUID.randomUUID();
        when(consentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(id)).isInstanceOf(NotFoundException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    private void stubFindAndSave(Consent consent) {
        when(consentRepository.findById(consent.getId())).thenReturn(Optional.of(consent));
        when(consentRepository.save(any(Consent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Consent consent(LocalDate start, LocalDate end) {
        return Consent.builder()
                .id(UUID.randomUUID())
                .clientUuid(UUID.randomUUID())
                .startDate(start)
                .endDate(end)
                .type(ConsentType.PERSONAL_DATA)
                .source("payme")
                .build();
    }
}
