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

package com.solano.eidoscore.crypto;

import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Страховочный проход по записям, форма хранения которых разошлась с согласием.
 *
 * <p>Основной путь — обработка события о согласии. Проход нужен для случаев,
 * когда событие не дошло: сервис был недоступен, запись появилась позже
 * согласия, обработка оборвалась на середине. Берёт обе стороны: и потерявшие
 * согласие, но ещё открытые, и обезличенные, которым согласие вернули.</p>
 *
 * <p>Выборка идёт по той же паре колонок, что и признак работы, поэтому проход
 * по уже согласованной базе ничего не находит и ничего не стоит.</p>
 */
@Component
public class DepersonalizationSweep {

    private static final Logger log = LoggerFactory.getLogger(DepersonalizationSweep.class);

    private final GoldenRecordRepository goldenRecordRepository;
    private final DepersonalizationService depersonalizationService;
    private final boolean enabled;
    private final int batchSize;

    public DepersonalizationSweep(
            GoldenRecordRepository goldenRecordRepository,
            DepersonalizationService depersonalizationService,
            @Value("${eidos.crypto.depersonalisation.sweep-enabled:true}") boolean enabled,
            @Value("${eidos.crypto.depersonalisation.batch-size:200}") int batchSize
    ) {
        this.goldenRecordRepository = goldenRecordRepository;
        this.depersonalizationService = depersonalizationService;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${eidos.crypto.depersonalisation.sweep-cron}")
    public void sweep() {
        if (!enabled) {
            return;
        }
        int hidden = process(true);
        int revealed = process(false);
        if (hidden > 0 || revealed > 0) {
            log.info("Depersonalisation sweep: {} hidden, {} restored", hidden, revealed);
        }
    }

    /** @param hide {@code true} — обезличить потерявших согласие, {@code false} — раскрыть вернувших */
    private int process(boolean hide) {
        int done = 0;
        while (true) {
            Page<GoldenRecord> page = hide
                    ? goldenRecordRepository.findByPdConsentActiveFalseAndPdTokenizedAtIsNull(
                            PageRequest.of(0, batchSize))
                    : goldenRecordRepository.findByPdConsentActiveTrueAndPdTokenizedAtIsNotNull(
                            PageRequest.of(0, batchSize));
            if (page.isEmpty()) {
                return done;
            }
            int changedInPage = 0;
            for (GoldenRecord record : page.getContent()) {
                try {
                    boolean changed = hide
                            ? depersonalizationService.depersonalize(record)
                            : depersonalizationService.restore(record);
                    if (changed) {
                        changedInPage++;
                    }
                } catch (Exception e) {
                    // Одна запись не должна останавливать проход, но и крутиться
                    // на ней бесконечно нельзя: выходим, следующий запуск попробует снова.
                    log.error("Cannot process {}", record.getGrClientId(), e);
                    return done + changedInPage;
                }
            }
            done += changedInPage;
            if (changedInPage == 0) {
                // Страница целиком не изменилась — выборка её и дальше будет
                // возвращать. Такое бывает, когда у записи нечего прятать.
                log.warn("Depersonalisation sweep stalled on {} records, leaving them for review",
                        page.getNumberOfElements());
                return done;
            }
        }
    }
}
