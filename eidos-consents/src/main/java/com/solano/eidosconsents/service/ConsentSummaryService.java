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
import com.solano.eidosconsents.repository.ConsentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сводка согласий клиента для карточки Customer 360: по каждому типу —
 * последнее согласие и признак активности на текущую дату.
 */
@Service
public class ConsentSummaryService {

    public record ConsentStatus(
            String type,
            String code,
            boolean active,
            UUID consentId,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    private final ConsentRepository consentRepository;
    private final Clock clock;

    public ConsentSummaryService(ConsentRepository consentRepository, Clock clock) {
        this.consentRepository = consentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ConsentStatus> summary(UUID clientUuid) {
        LocalDate today = LocalDate.now(clock);
        List<ConsentStatus> out = new ArrayList<>();
        for (ConsentType type : ConsentType.values()) {
            Optional<Consent> latest =
                    consentRepository.findFirstByClientUuidAndTypeOrderByStartDateDesc(clientUuid, type);
            boolean active = latest
                    .map(c -> !today.isBefore(c.getStartDate()) && !today.isAfter(c.getEndDate()))
                    .orElse(false);
            out.add(new ConsentStatus(
                    type.name(),
                    type.getCode(),
                    active,
                    latest.map(Consent::getId).orElse(null),
                    latest.map(Consent::getStartDate).orElse(null),
                    latest.map(Consent::getEndDate).orElse(null)));
        }
        return out;
    }
}
