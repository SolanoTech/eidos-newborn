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

package com.solano.eidoscore.service.legal;

import com.solano.eidoscore.engine.RecordCreator;
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.entity.legal.LegalEntityExternalId;
import com.solano.eidoscore.entity.legal.LegalEntityFieldMeta;
import com.solano.eidoscore.entity.legal.LegalEntityFieldMetaId;
import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.repository.legal.LegalEntityExternalIdRepository;
import com.solano.eidoscore.repository.legal.LegalEntityFieldMetaRepository;
import com.solano.eidoscore.repository.legal.LegalEntityRecordRepository;
import com.solano.eidoscore.service.legal.mapper.LegalEntityMapper;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Создание новой записи юрлица. Тонкий фасад над обобщённым
 * {@link RecordCreator}: порядок шагов — в движке, здесь специфика типа —
 * префикс идентификатора {@code LE_} и сущности external id / field_meta.
 */
@Service
public class LegalEntityCreator {

    private final RecordCreator<LegalEntityGoldenRecordDto, LegalEntityRecord, LegalEntityFieldMeta> engine;

    public LegalEntityCreator(
            LegalEntityRecordRepository recordRepository,
            LegalEntityExternalIdRepository externalIdRepository,
            LegalEntityFieldMetaRepository fieldMetaRepository,
            LegalEntityMapper mapper
    ) {
        this.engine = new RecordCreator<>(
                mapper,
                LegalEntityCreator::newRecordWithId,
                LegalEntityRecord::getGrLegalEntityId,
                recordRepository::save,
                (entity, source, externalId) -> externalIdRepository.save(
                        LegalEntityExternalId.builder()
                                .legalEntity(entity)
                                .source(source)
                                .externalId(externalId)
                                .isActive(Boolean.TRUE)
                                .build()),
                (entity, fieldName, source, now) -> LegalEntityFieldMeta.builder()
                        .id(new LegalEntityFieldMetaId(entity.getGrLegalEntityId(), fieldName))
                        .legalEntity(entity)
                        .source(source)
                        .updatedAt(now)
                        .build(),
                fieldMetaRepository::saveAll
        );
    }

    @Transactional
    public LegalEntityRecord execute(LegalEntityGoldenRecordDto dto, Source source, String externalId) {
        return engine.execute(dto, source, externalId);
    }

    private static LegalEntityRecord newRecordWithId() {
        LegalEntityRecord entity = new LegalEntityRecord();
        entity.setGrLegalEntityId("LE_" + UUID.randomUUID());
        return entity;
    }
}
