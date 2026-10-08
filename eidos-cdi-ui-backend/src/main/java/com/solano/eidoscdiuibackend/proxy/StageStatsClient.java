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

package com.solano.eidoscdiuibackend.proxy;

import com.solano.eidoscdiuibackend.web.dto.IngestStatView;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Статистика приёма (принято/отклонено по источникам) через admin API eidos-stage.
 */
@Component
public class StageStatsClient {

    private static final ParameterizedTypeReference<List<IngestStatView>> LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient stageRestClient;

    public StageStatsClient(RestClient stageRestClient) {
        this.stageRestClient = stageRestClient;
    }

    public List<IngestStatView> summary(int days) {
        return stageRestClient.get()
                .uri(b -> b.path("/internal/api/v1/ingest-stats").queryParam("days", days).build())
                .retrieve().body(LIST);
    }
}
