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

package com.solano.eidosgateway.service.stats;

import com.solano.eidosgateway.repository.registry.IngestStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Учёт записей, отклонённых валидацией на шлюзе, в общей таблице
 * {@code ingest_stats}. Ошибка статистики никогда не влияет на ответ клиенту.
 */
@Service
public class RejectStatsService {

    private static final Logger log = LoggerFactory.getLogger(RejectStatsService.class);

    private final IngestStatRepository repository;

    public RejectStatsService(IngestStatRepository repository) {
        this.repository = repository;
    }

    @Transactional("registryTransactionManager")
    public void recordRejected(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return;
        }
        try {
            repository.incrementRejected(sourceCode, LocalDate.now());
        } catch (Exception e) {
            log.warn("Failed to record reject stats for source={}: {}", sourceCode, e.getMessage());
        }
    }
}
