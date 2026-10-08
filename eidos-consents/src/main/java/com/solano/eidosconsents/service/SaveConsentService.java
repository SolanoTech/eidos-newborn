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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Создание согласия.
 *
 * <p>Если {@code endDate} не передан — он вычисляется от начальной даты в
 * зависимости от типа согласия (см. {@link #calculateEndDate}). Идентификатор
 * генерируется здесь же через {@code UUID.randomUUID()}.</p>
 *
 * <p>После сохранения публикуется доменное событие {@code new}; в Kafka оно
 * уходит только после коммита транзакции — см.
 * {@code ConsentEventPublisher}.</p>
 */
@Service
public class SaveConsentService {

    /** Срок действия согласия по умолчанию, в днях (для PERSONAL_DATA и BIO). */
    private static final long DEFAULT_VALIDITY_DAYS = 180;

    private final ConsentRepository consentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SaveConsentService(
            ConsentRepository consentRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.consentRepository = consentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Consent execute(
            ConsentType type,
            UUID clientUuid,
            LocalDate initialDate,
            LocalDate endDate,
            String sourceName
    ) {
        LocalDate effectiveEndDate = (endDate != null)
                ? endDate
                : calculateEndDate(initialDate, type);

        Consent consent = Consent.builder()
                .id(UUID.randomUUID())
                .clientUuid(clientUuid)
                .startDate(initialDate)
                .endDate(effectiveEndDate)
                .type(type)
                .source(sourceName)
                .build();

        Consent saved = consentRepository.save(consent);
        eventPublisher.publishEvent(ConsentEvent.of(ConsentEventType.NEW, saved));
        return saved;
    }

    /**
     * Вычисляет дату окончания согласия. Для PERSONAL_DATA и BIO — +180 дней
     * для маркетинговых и прочих типов (расширение CDP) — +365 дней.
     */
    private LocalDate calculateEndDate(LocalDate initialDate, ConsentType type) {
        return switch (type) {
            case PERSONAL_DATA, BIO -> initialDate.plusDays(DEFAULT_VALIDITY_DAYS);
            case MARKETING_SMS, MARKETING_PUSH, MARKETING_EMAIL, PROFILING, THIRD_PARTY ->
                    initialDate.plusDays(365);
        };
    }
}
