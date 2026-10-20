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

package com.solano.eidoscore.search;

import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.repository.legal.LegalEntityRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Догоняет индекс, когда он отстал от базы.
 *
 * <p>Индексация идёт после коммита и ошибку наверх не поднимает, поэтому
 * недоступность кластера оставляет расхождение. Проход переиндексирует записи,
 * изменённые за последнее окно: окно взято с запасом относительно периода
 * прохода, так что однократный сбой наверстывается следующим же запуском.</p>
 *
 * <p>Записи вне окна проход не трогает — за этим есть полная переиндексация,
 * которую включают отдельно. Она же нужна при первом наполнении индекса и
 * после изменения соответствия полей.</p>
 *
 * <p>Отдельной колонки «когда проиндексировано» намеренно нет: она означала бы
 * ещё одну запись в базу на каждое сохранение карточки, то есть лишнюю нагрузку
 * на приёмный тракт ради сведений, которые и так выводятся из
 * {@code updated_at}.</p>
 */
@Component
public class SearchIndexSweep {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexSweep.class);

    private final GoldenRecordRepository goldenRecordRepository;
    private final LegalEntityRecordRepository legalEntityRepository;
    private final SearchIndexer indexer;
    private final Clock clock;
    private final boolean enabled;
    private final int lookBackMinutes;
    private final int batchSize;
    private final boolean reindexOnStartup;

    public SearchIndexSweep(
            GoldenRecordRepository goldenRecordRepository,
            LegalEntityRecordRepository legalEntityRepository,
            SearchIndexer indexer,
            Clock clock,
            @Value("${eidos.search.sweep-enabled:true}") boolean enabled,
            @Value("${eidos.search.sweep-look-back-minutes:120}") int lookBackMinutes,
            @Value("${eidos.search.batch-size:500}") int batchSize,
            @Value("${eidos.search.reindex-on-startup:false}") boolean reindexOnStartup
    ) {
        this.goldenRecordRepository = goldenRecordRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.indexer = indexer;
        this.clock = clock;
        this.enabled = enabled;
        this.lookBackMinutes = lookBackMinutes;
        this.batchSize = batchSize;
        this.reindexOnStartup = reindexOnStartup;
    }

    /** Полная переиндексация: первое наполнение и смена соответствия полей. */
    @EventListener(ApplicationReadyEvent.class)
    public void reindexAll() {
        if (!reindexOnStartup) {
            return;
        }
        log.info("Full reindex requested");
        int clients = walk(page -> goldenRecordRepository.findAll(page), indexer::index);
        int merchants = walk(page -> legalEntityRepository.findAll(page), indexer::index);
        log.info("Full reindex finished: {} clients, {} merchants", clients, merchants);
    }

    @Scheduled(cron = "${eidos.search.sweep-cron}")
    public void sweep() {
        if (!enabled) {
            return;
        }
        LocalDateTime since = LocalDateTime.now(clock).minusMinutes(lookBackMinutes);
        int clients = walk(page -> goldenRecordRepository.findByUpdatedAtAfter(since, page), indexer::index);
        int merchants = walk(page -> legalEntityRepository.findByUpdatedAtAfter(since, page), indexer::index);
        if (clients > 0 || merchants > 0) {
            log.info("Search sweep: {} clients, {} merchants re-indexed", clients, merchants);
        }
    }

    private <T> int walk(java.util.function.Function<PageRequest, Page<T>> source,
                         java.util.function.Predicate<T> action) {
        int done = 0;
        for (int pageNumber = 0; ; pageNumber++) {
            Page<T> page = source.apply(
                    PageRequest.of(pageNumber, batchSize, Sort.by(Sort.Direction.ASC, "createdAt")));
            if (page.isEmpty()) {
                return done;
            }
            for (T item : page.getContent()) {
                if (action.test(item)) {
                    done++;
                }
            }
            if (!page.hasNext()) {
                return done;
            }
        }
    }
}
