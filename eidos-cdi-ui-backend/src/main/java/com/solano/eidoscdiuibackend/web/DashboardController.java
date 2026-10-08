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

package com.solano.eidoscdiuibackend.web;

import com.solano.eidoscdiuibackend.proxy.CoreGoldenRecordClient;
import com.solano.eidoscdiuibackend.proxy.StageSourceClient;
import com.solano.eidoscdiuibackend.proxy.StageStatsClient;
import com.solano.eidoscdiuibackend.web.dto.IngestStatView;
import com.solano.eidoscdiuibackend.web.dto.SourceView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Агрегированный дашборд консоли: метрики core (объём, качество, очередь
 * конфликтов), реестр источников stage и суточная активность приёма.
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final CoreGoldenRecordClient coreClient;
    private final StageSourceClient sourceClient;
    private final StageStatsClient statsClient;

    public DashboardController(CoreGoldenRecordClient coreClient,
                               StageSourceClient sourceClient,
                               StageStatsClient statsClient) {
        this.coreClient = coreClient;
        this.sourceClient = sourceClient;
        this.statsClient = statsClient;
    }

    @GetMapping
    public Map<String, Object> dashboard() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("metrics", coreClient.metrics());
        List<SourceView> sources = sourceClient.list();
        out.put("sources", sources);
        List<IngestStatView> activity = statsClient.summary(1);
        out.put("activity", activity);
        return out;
    }
}
