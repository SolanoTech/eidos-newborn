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
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Запись и удаление документов поискового индекса.
 *
 * <p>Идентификатор документа — идентификатор самой записи, поэтому повторная
 * индексация обновляет документ, а не плодит дубликаты.</p>
 *
 * <p><b>Обезличенная запись в индексе не хранится.</b> Индекс — вторая копия
 * персональных данных, и обезличивание обязано доставать и её: иначе клиент,
 * отозвавший согласие, остался бы прекрасно ищущимся по имени. Поэтому
 * {@link #index(GoldenRecord)} для обезличенной записи документ <i>удаляет</i>.
 * Следствие осознанное: нечёткий поиск не находит клиентов без согласия, тогда
 * как точный поиск по ПИНФЛ через слепой индекс продолжает работать — чтобы
 * найти человека, надо уже знать его номер.</p>
 *
 * <p>Ошибка индексации не поднимается наверх: запись уже сохранена, и ронять
 * приём из-за поисковой подсистемы неправильно. Расхождения чинит сверка.</p>
 */
@Component
public class SearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);

    private final OpenSearchClient client;
    private final boolean enabled;

    public SearchIndexer(OpenSearchClient client, @Value("${eidos.search.enabled:true}") boolean enabled) {
        this.client = client;
        this.enabled = enabled;
    }

    /** @return удалось ли привести индекс в соответствие с записью */
    public boolean index(GoldenRecord record) {
        if (record.isTokenized()) {
            return remove(SearchIndexes.CLIENTS, record.getGrClientId());
        }
        return put(SearchIndexes.CLIENTS, record.getGrClientId(), ClientDocument.of(record));
    }

    public boolean index(LegalEntityRecord record) {
        return put(SearchIndexes.MERCHANTS, record.getGrLegalEntityId(), MerchantDocument.of(record));
    }

    public boolean removeClient(String clientId) {
        return remove(SearchIndexes.CLIENTS, clientId);
    }

    private boolean put(String index, String id, Object document) {
        if (!enabled) {
            return false;
        }
        try {
            Result result = client.index(r -> r.index(index).id(id).document(document)).result();
            log.debug("Indexed {} in {}: {}", id, index, result);
            return true;
        } catch (Exception e) {
            log.error("Cannot index {} in {}; the sweep will retry", id, index, e);
            return false;
        }
    }

    private boolean remove(String index, String id) {
        if (!enabled) {
            return false;
        }
        try {
            client.delete(r -> r.index(index).id(id));
            log.debug("Removed {} from {}", id, index);
            return true;
        } catch (Exception e) {
            log.error("Cannot remove {} from {}; the sweep will retry", id, index, e);
            return false;
        }
    }
}
