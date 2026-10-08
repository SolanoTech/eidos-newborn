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

package com.solano.stage.web.admin;

import com.solano.stage.service.stats.IngestStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Внутренний admin API статистики приёма (UI-backend, дашборд «Активность
 * источников»). Защищён {@code X-Admin-Token}.
 */
@RestController
@RequestMapping("/internal/api/v1/ingest-stats")
public class IngestStatsController {

    private final IngestStatsService ingestStatsService;

    public IngestStatsController(IngestStatsService ingestStatsService) {
        this.ingestStatsService = ingestStatsService;
    }

    @GetMapping
    public List<IngestStatsService.IngestStatView> summary(@RequestParam(defaultValue = "1") int days) {
        return ingestStatsService.summary(days);
    }
}
