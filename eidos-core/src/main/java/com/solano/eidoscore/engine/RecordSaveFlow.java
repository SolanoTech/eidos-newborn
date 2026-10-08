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

package com.solano.eidoscore.engine;

import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

/**
 * Обобщённый оркестратор save-операции записи.
 *
 * <p>Поток (одинаков для всех типов записей):</p>
 * <ol>
 *   <li>Найти {@link Source} по имени — иначе tentative (UNKNOWN_SOURCE) и 404.</li>
 *   <li>Найти запись по {@code (source, externalId)}.</li>
 *   <li>Если не нашли — точный матчинг по идентифицирующим полям DTO
 *       (стратегия типа: у физлиц — ФИО+дата+ПИНФЛ/паспорт, у юрлиц — ИНН).</li>
 *   <li>Ничего не нашли — создание; нашли — merge.</li>
 * </ol>
 *
 * <p>Чистый POJO: транзакция — на фасаде типа.</p>
 */
public class RecordSaveFlow<D, E> {

    private static final Logger log = LoggerFactory.getLogger(RecordSaveFlow.class);

    private final Function<String, Optional<Source>> sourceResolver;
    private final ExternalIdMatcher<E> externalIdMatcher;
    private final ExactMatcher<D, E> exactMatcher;
    private final OrphanTentativeWriter<D> orphanTentativeWriter;
    private final RecordCreation<D, E> creation;
    private final RecordMerge<D, E> merge;

    public RecordSaveFlow(
            Function<String, Optional<Source>> sourceResolver,
            ExternalIdMatcher<E> externalIdMatcher,
            ExactMatcher<D, E> exactMatcher,
            OrphanTentativeWriter<D> orphanTentativeWriter,
            RecordCreation<D, E> creation,
            RecordMerge<D, E> merge
    ) {
        this.sourceResolver = sourceResolver;
        this.externalIdMatcher = externalIdMatcher;
        this.exactMatcher = exactMatcher;
        this.orphanTentativeWriter = orphanTentativeWriter;
        this.creation = creation;
        this.merge = merge;
    }

    public E execute(D dto, String sourceName, String clientSourceIdentifier) {
        Source source = sourceResolver.apply(sourceName).orElse(null);
        if (source == null) {
            log.info("Unknown source '{}' — writing tentative and rejecting request", sourceName);
            orphanTentativeWriter.write(dto, sourceName);
            throw new NotFoundException("Source " + sourceName + " not found");
        }

        log.debug("Searching record by source={} and externalId={}", sourceName, clientSourceIdentifier);
        E existing = externalIdMatcher.find(source, clientSourceIdentifier)
                .or(() -> {
                    log.debug("No match by external id, falling back to exact match");
                    return exactMatcher.find(dto);
                })
                .orElse(null);

        if (existing == null) {
            log.debug("No existing record — creating new");
            return creation.create(dto, source, clientSourceIdentifier);
        }

        log.debug("Existing record found — merging");
        return merge.merge(existing, dto, source);
    }
}
