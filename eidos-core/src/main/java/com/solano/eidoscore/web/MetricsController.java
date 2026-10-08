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

package com.solano.eidoscore.web;

import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.repository.TentativeGoldenRecordRepository;
import com.solano.eidoscore.entity.TentativeReason;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Метрики платформы для дашборда консоли: объём Golden Records, очередь
 * конфликтов и показатели качества (валидность/полнота/свежесть), считаемые
 * прямо по данным {@code golden_record}.
 */
@RestController
@RequestMapping("/api/v1/internal/metrics")
public class MetricsController {

    private final GoldenRecordRepository goldenRecordRepository;
    private final TentativeGoldenRecordRepository tentativeRepository;

    public MetricsController(
            GoldenRecordRepository goldenRecordRepository,
            TentativeGoldenRecordRepository tentativeRepository
    ) {
        this.goldenRecordRepository = goldenRecordRepository;
        this.tentativeRepository = tentativeRepository;
    }

    @GetMapping
    public Map<String, Object> metrics() {
        long total = goldenRecordRepository.count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("grTotal", total);
        m.put("tentativeTotal", tentativeRepository.count());
        m.put("tentativeGreyZone", tentativeRepository.countByReason(TentativeReason.GREY_ZONE_CONFLICT));
        m.put("tentativeUnknownSource", tentativeRepository.countByReason(TentativeReason.UNKNOWN_SOURCE));
        m.put("phoneValidPct", pct(goldenRecordRepository.countValidPhones(), total));
        m.put("pinflValidPct", pct(goldenRecordRepository.countValidPinfl(), total));
        m.put("middleNamePct", pct(goldenRecordRepository.countWithMiddleName(), total));
        m.put("fresh30dPct", pct(goldenRecordRepository.countFresh30d(), total));
        return m;
    }

    private static double pct(long part, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(part * 1000.0 / total) / 10.0;
    }
}
