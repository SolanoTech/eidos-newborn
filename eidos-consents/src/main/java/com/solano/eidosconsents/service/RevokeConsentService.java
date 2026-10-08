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
import com.solano.eidosconsents.exception.NotFoundException;
import com.solano.eidosconsents.messaging.ConsentEvent;
import com.solano.eidosconsents.messaging.ConsentEventType;
import com.solano.eidosconsents.messaging.ConsentRevoked;
import com.solano.eidosconsents.repository.ConsentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Отзыв согласия: срок действия обрезается вчерашним днём, после чего согласие
 * перестаёт быть активным. Запись не удаляется — история сохраняется.
 *
 * <ul>
 *   <li>Действующее сегодня согласие обрезается и помечается днём отзыва
 *       ({@code revoked_on}); публикуется доменное событие {@link ConsentRevoked}.
 *       Нужно ли из-за него событие {@code expired} в Kafka, решается после
 *       коммита — см. {@code ConsentRevocationListener}.</li>
 *   <li>Ещё не начавшееся согласие тоже обрезается — так оно не вступит в
 *       силу. Событий нет: клиент по нему согласен не был.</li>
 *   <li>Уже закончившееся согласие не меняется: перенос даты окончания на
 *       вчера продлил бы его в истории. Повторный отзыв поэтому безопасен.</li>
 * </ul>
 */
@Service
public class RevokeConsentService {

    private final ConsentRepository consentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public RevokeConsentService(
            ConsentRepository consentRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.consentRepository = consentRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Consent execute(UUID consentId) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new NotFoundException("Consent " + consentId + " not found"));
        LocalDate today = LocalDate.now(clock);
        if (consent.getEndDate().isBefore(today)) {
            return consent;
        }
        boolean wasActive = consent.isActiveOn(today);
        consent.setEndDate(today.minusDays(1));
        consent.setRevokedOn(today);
        Consent saved = consentRepository.save(consent);
        if (wasActive) {
            eventPublisher.publishEvent(
                    new ConsentRevoked(ConsentEvent.of(ConsentEventType.EXPIRED, saved), today));
        }
        return saved;
    }
}
