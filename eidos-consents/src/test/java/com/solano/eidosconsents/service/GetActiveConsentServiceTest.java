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
import com.solano.eidosconsents.repository.ConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActiveConsentServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);

    @Mock
    ConsentRepository consentRepository;

    private GetActiveConsentService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        service = new GetActiveConsentService(consentRepository, fixedClock);
    }

    @Test
    void returnsActiveConsent_queryingWithTodayBoundaries() {
        UUID client = UUID.randomUUID();
        Consent active = Consent.builder()
                .id(UUID.randomUUID())
                .clientUuid(client)
                .type(ConsentType.PERSONAL_DATA)
                .startDate(TODAY.minusDays(10))
                .endDate(TODAY.plusDays(170))
                .source("mobile-app")
                .build();
        when(consentRepository
                .findFirstByClientUuidAndTypeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        eq(client), eq(ConsentType.PERSONAL_DATA), eq(TODAY), eq(TODAY)))
                .thenReturn(Optional.of(active));

        Consent result = service.execute(client, ConsentType.PERSONAL_DATA);

        assertThat(result).isSameAs(active);
        verify(consentRepository)
                .findFirstByClientUuidAndTypeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        client, ConsentType.PERSONAL_DATA, TODAY, TODAY);
    }

    @Test
    void throwsNotFound_whenNoActiveConsent() {
        UUID client = UUID.randomUUID();
        when(consentRepository
                .findFirstByClientUuidAndTypeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        eq(client), eq(ConsentType.BIO), eq(TODAY), eq(TODAY)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(client, ConsentType.BIO))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("BIO");
    }
}
