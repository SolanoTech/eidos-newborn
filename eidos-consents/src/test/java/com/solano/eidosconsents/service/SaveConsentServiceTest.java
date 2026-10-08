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
import com.solano.eidosconsents.messaging.ConsentEvent;
import com.solano.eidosconsents.messaging.ConsentEventType;
import com.solano.eidosconsents.repository.ConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveConsentServiceTest {

    @Mock
    ConsentRepository consentRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    private SaveConsentService service;

    @BeforeEach
    void setUp() {
        service = new SaveConsentService(consentRepository, eventPublisher);
        when(consentRepository.save(org.mockito.ArgumentMatchers.any(Consent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void personalData_withoutEndDate_setsEndDate180DaysLater() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        UUID client = UUID.randomUUID();

        Consent consent = service.execute(ConsentType.PERSONAL_DATA, client, start, null, "mobile-app");

        assertThat(consent.getEndDate()).isEqualTo(start.plusDays(180));
        assertThat(consent.getType()).isEqualTo(ConsentType.PERSONAL_DATA);
        assertThat(consent.getClientUuid()).isEqualTo(client);
        assertThat(consent.getStartDate()).isEqualTo(start);
        assertThat(consent.getSource()).isEqualTo("mobile-app");
        assertThat(consent.getId()).isNotNull();
        verify(consentRepository).save(consent);
    }

    @Test
    void bio_withoutEndDate_setsEndDate180DaysLater() {
        LocalDate start = LocalDate.of(2026, 3, 10);

        Consent consent = service.execute(ConsentType.BIO, UUID.randomUUID(), start, null, "branch");

        assertThat(consent.getEndDate()).isEqualTo(start.plusDays(180));
    }

    @Test
    void providedEndDate_isRespected() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate explicitEnd = LocalDate.of(2026, 2, 1);

        Consent consent = service.execute(ConsentType.PERSONAL_DATA, UUID.randomUUID(), start, explicitEnd, "src");

        assertThat(consent.getEndDate()).isEqualTo(explicitEnd);
    }

    @Test
    void eachConsentGetsUniqueId() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        UUID client = UUID.randomUUID();

        Consent first = service.execute(ConsentType.BIO, client, start, null, "src");
        Consent second = service.execute(ConsentType.BIO, client, start, null, "src");

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void persistsExactlyWhatIsReturned() {
        ArgumentCaptor<Consent> captor = ArgumentCaptor.forClass(Consent.class);

        Consent returned = service.execute(
                ConsentType.PERSONAL_DATA, UUID.randomUUID(), LocalDate.of(2026, 1, 1), null, "src");

        verify(consentRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(returned);
    }

    @Test
    void publishesNewConsentEvent_withTheStoredDates() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        UUID client = UUID.randomUUID();

        service.execute(ConsentType.PERSONAL_DATA, client, start, null, "mobile-app");

        ArgumentCaptor<ConsentEvent> captor = ArgumentCaptor.forClass(ConsentEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        ConsentEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(ConsentEventType.NEW);
        assertThat(event.clientUuid()).isEqualTo(client);
        assertThat(event.type()).isEqualTo(ConsentType.PERSONAL_DATA);
        assertThat(event.startDate()).isEqualTo(start);
        // Событие несёт вычисленный срок, а не то, что прислал источник.
        assertThat(event.endDate()).isEqualTo(start.plusDays(180));
    }
}
