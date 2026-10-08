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

package com.solano.stage.service.stats;

import com.solano.stage.entity.registry.IngestStat;
import com.solano.stage.repository.registry.IngestStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Учёт суточной статистики приёма. Инкременты никогда не роняют пайплайн:
 * любая ошибка статистики логируется и глотается.
 */
@Service
public class IngestStatsService {

    public record IngestStatView(String sourceCode, long accepted, long rejected) {
    }

    private static final Logger log = LoggerFactory.getLogger(IngestStatsService.class);

    private final IngestStatRepository repository;

    public IngestStatsService(IngestStatRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordAccepted(String sourceCode) {
        incrementQuietly(sourceCode, 1, 0);
    }

    @Transactional
    public void recordRejected(String sourceCode) {
        incrementQuietly(sourceCode, 0, 1);
    }

    private void incrementQuietly(String sourceCode, long accepted, long rejected) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return;
        }
        try {
            repository.increment(sourceCode, LocalDate.now(), accepted, rejected);
        } catch (Exception e) {
            log.warn("Failed to record ingest stats for source={}: {}", sourceCode, e.getMessage());
        }
    }

    /** Агрегат за последние {@code days} суток, по источникам. */
    @Transactional(readOnly = true)
    public List<IngestStatView> summary(int days) {
        LocalDate from = LocalDate.now().minusDays(Math.max(days, 1) - 1L);
        Map<String, long[]> acc = new LinkedHashMap<>();
        for (IngestStat s : repository.findByDayGreaterThanEqual(from)) {
            long[] pair = acc.computeIfAbsent(s.getSourceCode(), k -> new long[2]);
            pair[0] += s.getAccepted();
            pair[1] += s.getRejected();
        }
        return acc.entrySet().stream()
                .map(e -> new IngestStatView(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }
}
