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
import java.util.Objects;
import java.util.function.Function;

/**
 * Обобщённый merge входящего DTO в существующую запись — ядро MDM-логики,
 * общее для всех типов записей (физлица, юрлица).
 *
 * <p>Правила по каждому полю (через {@link FieldProvenance}):</p>
 * <ol>
 *   <li>Источник новый, его {@code trustLevel} выше, значение пришло непустым
 *       и отличается от текущего — перезаписываем поле и переключаем
 *       провенанс на новый источник. Если значение совпало — переключаем
 *       только провенанс: подтверждённое значение закрепляется за более
 *       надёжным источником. Непришедшие поля остаются за прежним
 *       источником (по-полевой merge).</li>
 *   <li>Источник тот же, что поставил текущее значение, значение пришло
 *       непустым и отличается — обновляем поле: раз поле за ним, более
 *       надёжного источника для этого поля нет, и источник вправе передать
 *       изменение своих же данных. Провенанс остаётся за ним, время
 *       обновления сдвигается.</li>
 *   <li>Источник новый, trust равен, значения отличаются — серая зона:
 *       пишем tentative-снимок входящих значений (не более одного на вызов),
 *       текущее значение не меняем.</li>
 *   <li>В остальных случаях ничего не делаем.</li>
 * </ol>
 *
 * <p>Если хотя бы одно поле перезаписано — запись сохраняется (JPA
 * инкрементирует {@code @Version}) и ПОСЛЕ сохранения архивируется, чтобы
 * снимок отражал новую версию.</p>
 *
 * <p>Чистый POJO: транзакция и Spring-проводка — на фасаде типа.</p>
 */
public class RecordMerger<D, E, M extends FieldProvenance> {

    private static final Logger log = LoggerFactory.getLogger(RecordMerger.class);

    private final RecordFieldAccess<D, E> fields;
    private final Function<E, String> recordId;
    private final ProvenanceLoader<M> provenanceLoader;
    private final RecordStore<E> recordStore;
    private final ProvenanceSaver<M> provenanceSaver;
    private final Archiver<E> archiver;
    private final IncomingTentativeWriter<D> tentativeWriter;

    public RecordMerger(
            RecordFieldAccess<D, E> fields,
            Function<E, String> recordId,
            ProvenanceLoader<M> provenanceLoader,
            RecordStore<E> recordStore,
            ProvenanceSaver<M> provenanceSaver,
            Archiver<E> archiver,
            IncomingTentativeWriter<D> tentativeWriter
    ) {
        this.fields = fields;
        this.recordId = recordId;
        this.provenanceLoader = provenanceLoader;
        this.recordStore = recordStore;
        this.provenanceSaver = provenanceSaver;
        this.archiver = archiver;
        this.tentativeWriter = tentativeWriter;
    }

    public E execute(E existing, D incoming, Source incomingSource) {
        String id = recordId.apply(existing);
        List<M> metadata = provenanceLoader.byRecordId(id);

        boolean wasUpdated = false;
        boolean greyZoneSnapshotWritten = false;
        List<M> updatedMetadata = new ArrayList<>();
        List<M> confirmedMetadata = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (M meta : metadata) {
            String fieldName = meta.getFieldName();
            if (!fields.knowsField(fieldName)) {
                continue;
            }
            Source metaSource = meta.getSource();

            boolean differentSource = !Objects.equals(incomingSource.getId(), metaSource.getId());
            boolean higherTrust = incomingSource.getTrustLevel() > metaSource.getTrustLevel();
            boolean equalTrust = Objects.equals(incomingSource.getTrustLevel(), metaSource.getTrustLevel());

            // Владелец поля обновляет своё значение: поле за ним, значит более
            // надёжного источника для него нет. Правила те же, что у более
            // надёжного источника: пустое значение ничего не затирает.
            if (!differentSource || higherTrust) {
                // По-полевой merge: перезаписываем значение и переключаем provenance
                // ТОЛЬКО если источник реально прислал поле (не null) и его значение
                // отличается от текущего. Непришедшие поля остаются за прежним
                // источником — частичное обновление не затирает их пустыми значениями.
                Object incomingVal = fields.readFromDto(incoming, fieldName);
                Object currentVal = fields.readFromEntity(existing, fieldName);
                if (incomingVal != null && !Objects.equals(incomingVal, currentVal)) {
                    fields.copyField(incoming, existing, fieldName);
                    meta.setSource(incomingSource);
                    meta.setUpdatedAt(now);
                    updatedMetadata.add(meta);
                    wasUpdated = true;
                } else if (differentSource && incomingVal != null) {
                    // Более надёжный источник подтвердил то же значение — поле
                    // закрепляется за ним, иначе прежний, менее надёжный владелец
                    // мог бы потом изменить подтверждённое значение. Данные записи
                    // не меняются: версия не растёт, архив не пишется.
                    meta.setSource(incomingSource);
                    meta.setUpdatedAt(now);
                    confirmedMetadata.add(meta);
                }
            } else if (differentSource && equalTrust) {
                Object currentVal = fields.readFromEntity(existing, fieldName);
                Object incomingVal = fields.readFromDto(incoming, fieldName);
                if (!Objects.equals(currentVal, incomingVal)) {
                    log.warn(
                            "Grey-zone conflict on field {} for recordId={}: " +
                                    "incoming source '{}' (trust {}) ties with current source '{}' (trust {}); " +
                                    "writing tentative snapshot, record value unchanged",
                            fieldName, id,
                            incomingSource.getSourceName(), incomingSource.getTrustLevel(),
                            metaSource.getSourceName(), metaSource.getTrustLevel());
                    greyZoneSnapshotWritten = greyZoneSnapshotWritten
                            || writeGreyZoneSnapshotOnce(id, incoming, incomingSource.getSourceName(), greyZoneSnapshotWritten);
                }
            }
        }

        if (wasUpdated) {
            log.debug("Record {} updated by source={}", id, incomingSource.getSourceName());
            E saved = recordStore.save(existing);
            provenanceSaver.saveAll(updatedMetadata);
            // Архивный снимок пишется ПОСЛЕ save'а, чтобы archivedVersion отражал
            // уже инкрементированный @Version.
            archiver.archive(saved);
        }
        if (!confirmedMetadata.isEmpty()) {
            provenanceSaver.saveAll(confirmedMetadata);
        }
        return existing;
    }

    /**
     * Гарантирует, что для одного merge-вызова мы пишем максимум ОДНУ tentative
     * запись по причине grey-zone, даже если конфликтуют несколько полей —
     * иначе оператор будет тонуть в дубликатах.
     */
    private boolean writeGreyZoneSnapshotOnce(String recordId, D incoming,
                                              String sourceName, boolean alreadyWritten) {
        if (alreadyWritten) {
            return true;
        }
        tentativeWriter.write(incoming, recordId, sourceName);
        return true;
    }
}
