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

import com.solano.eidoscore.crypto.DepersonalizationService;
import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Приводит состояние согласия на Золотой записи в соответствие с тем, что на
 * этот момент говорит сервис согласий.
 *
 * <p>Событие служит только поводом свериться: его содержимое не используется.
 * Благодаря этому повторная доставка безвредна, порядок сообщений не важен, а
 * результат один и тот же, сколько бы раз обработка ни повторилась.</p>
 *
 * <p>Пока это состояние никем не читается — на нём будет стоять деперсонализация.</p>
 */
@Service
public class ConsentStateService {

    private static final Logger log = LoggerFactory.getLogger(ConsentStateService.class);

    private final GoldenRecordRepository goldenRecordRepository;
    private final ConsentsClient consentsClient;
    private final DepersonalizationService depersonalizationService;
    private final Clock clock;

    public ConsentStateService(
            GoldenRecordRepository goldenRecordRepository,
            ConsentsClient consentsClient,
            DepersonalizationService depersonalizationService,
            Clock clock
    ) {
        this.goldenRecordRepository = goldenRecordRepository;
        this.consentsClient = consentsClient;
        this.depersonalizationService = depersonalizationService;
        this.clock = clock;
    }

    /**
     * Сверяет и записывает состояние согласия клиента.
     *
     * @return {@code false}, если Золотой записи с таким идентификатором нет —
     *         согласие может быть выдано раньше, чем приедут данные клиента
     */
    @Transactional
    public boolean reconcile(UUID clientUuid) {
        Optional<String> clientId = resolveClientId(clientUuid);
        if (clientId.isEmpty()) {
            log.info("No golden record for client {} yet, consent state not stored", clientUuid);
            return false;
        }

        Optional<ActiveConsent> active = consentsClient.activePersonalDataConsent(clientUuid);
        LocalDate validTo = active.map(ActiveConsent::endDate).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);

        goldenRecordRepository.updateConsentState(clientId.get(), active.isPresent(), validTo, now);
        log.info("Consent state for {}: active={} validTo={}", clientId.get(), active.isPresent(), validTo);

        applyToStoredData(clientId.get(), active.isPresent());
        return true;
    }

    /**
     * Приводит форму хранения в соответствие с согласием: без согласия значения
     * заменяются токенами, с возвращённым согласием — раскрываются обратно.
     *
     * <p>Запись перечитывается после смены состояния, чтобы работать с текущей
     * отметкой обезличивания. Обе операции идемпотентны, поэтому повторная
     * обработка того же события ничего не меняет.</p>
     */
    private void applyToStoredData(String clientId, boolean consentActive) {
        GoldenRecord record = goldenRecordRepository.findById(clientId).orElse(null);
        if (record == null) {
            return;
        }
        if (consentActive) {
            depersonalizationService.restore(record);
        } else {
            depersonalizationService.depersonalize(record);
        }
    }

    /** Идентификатор из события приходит без префикса — проверяем обе формы. */
    private Optional<String> resolveClientId(UUID clientUuid) {
        return GoldenRecordIds.candidates(clientUuid).stream()
                .filter(goldenRecordRepository::existsById)
                .findFirst();
    }
}
