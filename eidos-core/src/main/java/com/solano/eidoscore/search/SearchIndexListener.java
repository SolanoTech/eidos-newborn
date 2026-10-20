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

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Проводит изменения записей в поисковый индекс — <b>после коммита</b>.
 *
 * <p>Почему после, а не внутри транзакции: OpenSearch в транзакции базы не
 * участвует. Проиндексируй мы внутри — при откате в индексе остался бы фантом
 * записи, которой нет. А блокировать приём на ответе поисковой системы нельзя:
 * это чужая подсистема на горячем пути.</p>
 *
 * <p>Цена решения — индекс догоняет базу, а не совпадает с ней секунда в
 * секунду, и при недоступности кластера отстаёт. Расхождения чинит
 * {@link SearchIndexSweep}.</p>
 */
@Component
public class SearchIndexListener {

    private final SearchIndexer indexer;

    public SearchIndexListener(SearchIndexer indexer) {
        this.indexer = indexer;
    }

    @TransactionalEventListener
    public void onRecordChanged(RecordChanged event) {
        switch (event) {
            case RecordChanged.Client c -> indexer.index(c.record());
            case RecordChanged.Merchant m -> indexer.index(m.record());
        }
    }
}
