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

package com.solano.eidoscore.engine;

import com.solano.eidoscore.entity.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Обобщённое создание новой записи из входящего DTO.
 *
 * <p>Шаги (транзакция — на фасаде типа):</p>
 * <ol>
 *   <li>Новая entity с уже сгенерированным идентификатором
 *       ({@link RecordFactory} типа отвечает за префикс — GR_/LE_).</li>
 *   <li>Копирование всех известных полей из DTO.</li>
 *   <li>Сохранение записи.</li>
 *   <li>Активная связка с внешним идентификатором источника.</li>
 *   <li>Строка провенанса для каждого скопированного поля — база для
 *       последующих merge по trust-уровням.</li>
 * </ol>
 */
public class RecordCreator<D, E, M extends FieldProvenance> {

    private static final Logger log = LoggerFactory.getLogger(RecordCreator.class);

    private final RecordFieldAccess<D, E> fields;
    private final RecordFactory<E> recordFactory;
    private final Function<E, String> recordId;
    private final RecordStore<E> recordStore;
    private final ExternalIdStore<E> externalIdStore;
    private final ProvenanceFactory<E, M> provenanceFactory;
    private final ProvenanceSaver<M> provenanceSaver;

    public RecordCreator(
            RecordFieldAccess<D, E> fields,
            RecordFactory<E> recordFactory,
            Function<E, String> recordId,
            RecordStore<E> recordStore,
            ExternalIdStore<E> externalIdStore,
            ProvenanceFactory<E, M> provenanceFactory,
            ProvenanceSaver<M> provenanceSaver
    ) {
        this.fields = fields;
        this.recordFactory = recordFactory;
        this.recordId = recordId;
        this.recordStore = recordStore;
        this.externalIdStore = externalIdStore;
        this.provenanceFactory = provenanceFactory;
        this.provenanceSaver = provenanceSaver;
    }

    public E execute(D dto, Source source, String externalId) {
        E entity = recordFactory.newRecord();
        log.debug("Creating record id={} for source={}", recordId.apply(entity), source.getSourceName());

        List<String> copiedFields = fields.copyToEntity(dto, entity);

        recordStore.save(entity);
        externalIdStore.createActive(entity, source, externalId);
        saveProvenance(entity, source, copiedFields);

        return entity;
    }

    private void saveProvenance(E entity, Source source, List<String> fieldNames) {
        LocalDateTime now = LocalDateTime.now();
        List<M> rows = new ArrayList<>(fieldNames.size());
        for (String fieldName : fieldNames) {
            rows.add(provenanceFactory.create(entity, fieldName, source, now));
        }
        provenanceSaver.saveAll(rows);
    }
}
