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

import com.solano.eidoscdiuibackend.web.dto.SourceRequest;
import com.solano.eidoscdiuibackend.web.dto.SourceView;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Доступ к реестру источников через admin API eidos-stage. Не содержит
 * бизнес-логики — только проксирование вызовов; ошибки downstream (4xx/5xx)
 * RestClient бросает как {@code RestClientResponseException}, их транслирует
 * глобальный обработчик.
 */
@Component
public class StageSourceClient {

    private static final String BASE = "/internal/api/v1/sources";
    private static final ParameterizedTypeReference<List<SourceView>> LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient stageRestClient;

    public StageSourceClient(RestClient stageRestClient) {
        this.stageRestClient = stageRestClient;
    }

    public List<SourceView> list() {
        return stageRestClient.get().uri(BASE).retrieve().body(LIST_TYPE);
    }

    public SourceView create(SourceRequest request) {
        return stageRestClient.post().uri(BASE).body(request).retrieve().body(SourceView.class);
    }

    public SourceView update(Long id, SourceRequest request) {
        return stageRestClient.put().uri(BASE + "/{id}", id).body(request).retrieve().body(SourceView.class);
    }

    public void delete(Long id) {
        stageRestClient.delete().uri(BASE + "/{id}", id).retrieve().toBodilessEntity();
    }
}
