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

import com.solano.eidoscore.exception.UnprocessableException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Нечёткий поиск клиентов и мерчантов.
 *
 * <p>Опечатки прощаются автоматически: {@code fuzziness} подбирается по длине
 * слова. Кириллица и латиница сведены анализатором, поэтому «Каримов» находится
 * и по «Karimov». У мерчантов анализатор дополнительно выбрасывает
 * организационно-правовые формы, так что «ООО Оазис» и «Oazis MCHJ» сходятся.</p>
 *
 * <p>Возвращаются только идентификаторы записей и оценка совпадения: карточку
 * вызывающая сторона дочитывает из базы. Так в индексе не приходится держать
 * данные ради выдачи, а права на их показ остаются там, где им положено.</p>
 *
 * <p>Обезличенные клиенты не находятся — их документов в индексе нет. Это
 * осознанно: поиск по имени для клиента без согласия был бы способом его
 * перечислить.</p>
 */
@Service
public class FuzzySearchService {

    private static final Logger log = LoggerFactory.getLogger(FuzzySearchService.class);

    /** Запрос короче отсекается: он вернул бы половину базы. */
    private static final int MIN_QUERY_LENGTH = 3;

    private final OpenSearchClient client;
    private final int maxResults;

    public FuzzySearchService(
            OpenSearchClient client,
            @Value("${eidos.search.max-results:50}") int maxResults
    ) {
        this.client = client;
        this.maxResults = maxResults;
    }

    /** @param limit сколько вернуть; обрезается до максимума из конфигурации */
    public List<Match> searchClients(String query, int limit) {
        return search(SearchIndexes.CLIENTS, query, limit,
                List.of("fullName^3", "lastName^2", "firstName", "middleName"),
                "grClientId");
    }

    public List<Match> searchMerchants(String query, int limit) {
        return search(SearchIndexes.MERCHANTS, query, limit,
                List.of("fullName^2", "shortName"),
                "grLegalEntityId");
    }

    private List<Match> search(String index, String query, int limit, List<String> fields, String idField) {
        String trimmed = query == null ? "" : query.strip();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            throw new UnprocessableException(
                    "Search query must be at least " + MIN_QUERY_LENGTH + " characters long");
        }
        int size = Math.max(1, Math.min(limit <= 0 ? maxResults : limit, maxResults));
        try {
            SearchResponse<Document> response = client.search(r -> r
                            .index(index)
                            .size(size)
                            .query(q -> q.multiMatch(m -> m
                                    .query(trimmed)
                                    .fields(fields)
                                    .fuzziness("AUTO")
                                    .operator(Operator.And))),
                    Document.class);
            return response.hits().hits().stream()
                    .map(hit -> new Match(hit.id(), hit.score() == null ? 0 : hit.score()))
                    .toList();
        } catch (Exception e) {
            log.error("Fuzzy search in {} failed", index, e);
            throw new SearchUnavailableException(e);
        }
    }

    /** Найденная запись: идентификатор и оценка совпадения. */
    public record Match(String id, double score) {
    }

    /** Поля документа нас не интересуют — идентификатор приходит в {@code _id}. */
    private record Document() {
    }

    /** Поисковая подсистема недоступна: отличать от «ничего не найдено». */
    public static class SearchUnavailableException extends RuntimeException {
        public SearchUnavailableException(Throwable cause) {
            super("Search is unavailable", cause);
        }
    }
}
