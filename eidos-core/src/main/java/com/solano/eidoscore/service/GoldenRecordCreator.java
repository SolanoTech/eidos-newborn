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

package com.solano.eidoscore.service;

import com.solano.eidoscore.engine.RecordCreator;
import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.entity.GoldenRecordExternalId;
import com.solano.eidoscore.entity.GoldenRecordFieldMeta;
import com.solano.eidoscore.entity.GoldenRecordFieldMetaId;
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.repository.GoldenRecordExternalIdRepository;
import com.solano.eidoscore.repository.GoldenRecordFieldMetaRepository;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.service.mapper.GoldenRecordMapper;
import com.solano.shared.dto.GoldenRecordDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Создаёт новую {@link GoldenRecord} из входящего {@link GoldenRecordDto}.
 *
 * <p>Тонкий фасад физлиц над обобщённым {@link RecordCreator}: порядок шагов
 * (id → копирование → save → external id → field_meta) — в движке; здесь —
 * специфика типа: префикс {@code GR_}, сущности external id и field_meta.</p>
 */
@Service
public class GoldenRecordCreator {

    private final RecordCreator<GoldenRecordDto, GoldenRecord, GoldenRecordFieldMeta> engine;

    public GoldenRecordCreator(
            GoldenRecordRepository goldenRecordRepository,
            GoldenRecordExternalIdRepository externalIdRepository,
            GoldenRecordFieldMetaRepository fieldMetaRepository,
            GoldenRecordMapper mapper
    ) {
        this.engine = new RecordCreator<>(
                mapper,
                GoldenRecordCreator::newRecordWithId,
                GoldenRecord::getGrClientId,
                goldenRecordRepository::save,
                (entity, source, externalId) -> externalIdRepository.save(
                        GoldenRecordExternalId.builder()
                                .goldenRecord(entity)
                                .source(source)
                                .externalId(externalId)
                                .isActive(Boolean.TRUE)
                                .build()),
                (entity, fieldName, source, now) -> GoldenRecordFieldMeta.builder()
                        .id(new GoldenRecordFieldMetaId(entity.getGrClientId(), fieldName))
                        .goldenRecord(entity)
                        .source(source)
                        .updatedAt(now)
                        .build(),
                fieldMetaRepository::saveAll
        );
    }

    @Transactional
    public GoldenRecord execute(GoldenRecordDto dto, Source source, String externalId) {
        return engine.execute(dto, source, externalId);
    }

    private static GoldenRecord newRecordWithId() {
        GoldenRecord entity = new GoldenRecord();
        entity.setGrClientId("GR_" + UUID.randomUUID());
        return entity;
    }
}
