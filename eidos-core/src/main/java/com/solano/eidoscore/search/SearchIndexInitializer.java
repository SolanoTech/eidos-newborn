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

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Заводит индексы нечёткого поиска, если их ещё нет.
 *
 * <p>Описания лежат в ресурсах: там и анализаторы, и соответствие полей.
 * Анализатор клиентов транслитерирует кириллицу в узбекскую латиницу, чтобы
 * «Каримов» и «Karimov» сводились к одному; анализатор мерчантов делает то же
 * и дополнительно выбрасывает организационно-правовые формы, которые при
 * поиске по бренду только мешают. Раньше эту работу делала нормализация
 * названия на запись — теперь она целиком в индексе.</p>
 *
 * <p>Идемпотентно: существующий индекс не трогается. Изменение соответствия
 * полей существующий индекс не подхватит — для этого нужен новый индекс и
 * переиндексация, обычная практика для поисковых систем.</p>
 *
 * <p>Недоступность кластера старт не роняет: поиск — не тот компонент, ради
 * которого стоит отказываться принимать данные.</p>
 */
@Component
public class SearchIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexInitializer.class);

    private final OpenSearchClient client;
    private final boolean enabled;

    public SearchIndexInitializer(OpenSearchClient client, @Value("${eidos.search.enabled:true}") boolean enabled) {
        this.client = client;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        if (!enabled) {
            log.info("Search is disabled, indices not checked");
            return;
        }
        ensure(SearchIndexes.CLIENTS, "search/clients-index.json");
        ensure(SearchIndexes.MERCHANTS, "search/merchants-index.json");
    }

    private void ensure(String index, String definition) {
        try {
            boolean exists = client.indices().exists(r -> r.index(index)).value();
            if (exists) {
                log.debug("Index {} already present", index);
                return;
            }
            // Описание отправляется как есть: файл ресурса — единственный
            // источник правды по анализаторам и соответствию полей, и
            // пересобирать его типизированным билдером значило бы держать то же
            // самое в двух местах.
            String body;
            try (InputStream in = new ClassPathResource(definition).getInputStream()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            try (Response response = client.generic().execute(Requests.builder()
                    .endpoint("/" + index)
                    .method("PUT")
                    .json(body)
                    .build())) {
                if (response.getStatus() >= 300) {
                    log.error("Cannot create index {}: HTTP {}", index, response.getStatus());
                    return;
                }
            }
            log.info("Created search index {}", index);
        } catch (Exception e) {
            log.error("Cannot prepare search index {}; fuzzy search will not work until it exists", index, e);
        }
    }
}
