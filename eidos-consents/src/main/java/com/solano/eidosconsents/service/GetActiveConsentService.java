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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Поиск последнего активного согласия клиента выбранного типа.
 *
 * <p>«Активное» — согласие, действующее на текущую дату
 * ({@code start_date <= today <= end_date}); «последнее» — с самым поздним
 * {@code start_date}. Если подходящего согласия нет — {@link NotFoundException}.</p>
 *
 * <p>{@link Clock} инжектируется, чтобы «сегодня» было детерминированным в тестах.</p>
 */
@Service
public class GetActiveConsentService {

    private final ConsentRepository consentRepository;
    private final Clock clock;

    public GetActiveConsentService(ConsentRepository consentRepository, Clock clock) {
        this.consentRepository = consentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Consent execute(UUID clientUuid, ConsentType type) {
        LocalDate today = LocalDate.now(clock);
        return consentRepository
                .findFirstByClientUuidAndTypeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        clientUuid, type, today, today)
                .orElseThrow(() -> new NotFoundException(
                        "No active consent of type " + type + " for client " + clientUuid));
    }
}
