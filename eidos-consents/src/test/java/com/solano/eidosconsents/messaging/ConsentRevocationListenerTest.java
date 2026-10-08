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
import com.solano.eidosconsents.repository.ConsentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * После коммита отзыва {@code expired} уходит, только если клиент остался без
 * действующего согласия этого типа.
 */
@ExtendWith(MockitoExtension.class)
class ConsentRevocationListenerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 21);
    private static final UUID CLIENT = UUID.randomUUID();

    @Mock ConsentRepository consentRepository;
    @Mock ConsentEventPublisher publisher;
    @InjectMocks ConsentRevocationListener listener;

    private final ConsentEvent expired = new ConsentEvent(ConsentEventType.EXPIRED, CLIENT,
            ConsentType.PERSONAL_DATA, LocalDate.of(2026, 2, 21), TODAY.minusDays(1));

    @Test
    void noOtherActiveConsent_publishesExpired() {
        when(consentRepository
                .findFirstByClientUuidAndTypeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        CLIENT, ConsentType.PERSONAL_DATA, TODAY, TODAY))
                .thenReturn(Optional.empty());

        listener.onRevoked(new ConsentRevoked(expired, TODAY));

        verify(publisher).publish(expired);
    }

    @Test
    void anotherActiveConsentOfTheType_staysSilent() {
        Consent other = Consent.builder()
                .id(UUID.randomUUID())
                .clientUuid(CLIENT)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 10, 28))
                .type(ConsentType.PERSONAL_DATA)
                .source("payme")
                .build();
        when(consentRepository
                .findFirstByClientUuidAndTypeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        CLIENT, ConsentType.PERSONAL_DATA, TODAY, TODAY))
                .thenReturn(Optional.of(other));

        listener.onRevoked(new ConsentRevoked(expired, TODAY));

        verify(publisher, never()).publish(any());
    }
}
