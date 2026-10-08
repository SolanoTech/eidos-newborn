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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Досчитывает слепой индекс у записей, которые появились до его введения.
 *
 * <p>Миграция этого сделать не могла: HMAC требует ключа, а ключ живёт в
 * хранилище, не в базе. Поэтому дозаполнение — код, а не SQL.</p>
 *
 * <p>Значения индекса считает тот же {@link BlindIndex}, что и при обычной
 * записи, — расходиться двум путям вычисления не даёт общий код. А вот
 * записывается результат точечным UPDATE, а не через сущность: иначе
 * дозаполнение подняло бы {@code version} и {@code updated_at} у каждой
 * карточки и выглядело бы как массовая правка всех клиентов за один день.</p>
 *
 * <p>Идемпотентно и возобновляемо: выбираются строки, у которых версия ключа
 * пустая или отличается от текущей, поэтому повторный запуск после обрыва
 * продолжает с того же места. Тот же механизм обслужит ротацию ключа.</p>
 */
@Component
public class BlindIndexBackfill {

    private static final Logger log = LoggerFactory.getLogger(BlindIndexBackfill.class);

    private final GoldenRecordRepository goldenRecordRepository;
    private final BlindIndex blindIndex;
    private final boolean enabled;
    private final int batchSize;

    public BlindIndexBackfill(
            GoldenRecordRepository goldenRecordRepository,
            BlindIndex blindIndex,
            @Value("${eidos.crypto.blind-index.backfill-enabled:true}") boolean enabled,
            @Value("${eidos.crypto.blind-index.batch-size:500}") int batchSize
    ) {
        this.goldenRecordRepository = goldenRecordRepository;
        this.blindIndex = blindIndex;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        if (!enabled) {
            log.info("Blind index backfill disabled");
            return;
        }
        try {
            int version = blindIndex.currentKeyVersion();
            long pending = goldenRecordRepository.countByPdBiKeyVersionIsNullOrPdBiKeyVersionNot(version);
            if (pending == 0) {
                log.info("Blind index is up to date for key version {}", version);
                return;
            }
            log.info("Blind index backfill: {} records to process with key version {}", pending, version);
            long done = 0;
            while (true) {
                int processed = processBatch(version);
                if (processed == 0) {
                    break;
                }
                done += processed;
                log.info("Blind index backfill: {}/{}", done, pending);
            }
            log.info("Blind index backfill finished, {} records processed", done);
        } catch (Exception e) {
            // Индекс пока никем не читается, поэтому неудача дозаполнения не
            // повод не пускать сервис. Повторится при следующем старте.
            log.error("Blind index backfill failed; it will be retried on next startup", e);
        }
    }

    /** Всегда берём первую страницу: обработанные строки из выборки уходят. */
    private int processBatch(int version) {
        Page<GoldenRecord> page = goldenRecordRepository
                .findByPdBiKeyVersionIsNullOrPdBiKeyVersionNot(version, PageRequest.of(0, batchSize));
        if (page.isEmpty()) {
            return 0;
        }
        for (GoldenRecord record : page.getContent()) {
            goldenRecordRepository.updateBlindIndex(
                    record.getGrClientId(),
                    blindIndex.identity(record.getGrLastName(), record.getGrFirstName(),
                            record.getGrBirthDate(), record.getGrPinfl()),
                    blindIndex.identityByDocument(record.getGrLastName(), record.getGrFirstName(),
                            record.getGrBirthDate(), record.getGrDocPassData()),
                    blindIndex.pinfl(record.getGrPinfl()),
                    version);
        }
        return page.getNumberOfElements();
    }
}
