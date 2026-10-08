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

import com.solano.eidoscore.engine.RecordMerger;
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.entity.legal.LegalEntityFieldMeta;
import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.repository.legal.LegalEntityFieldMetaRepository;
import com.solano.eidoscore.repository.legal.LegalEntityRecordRepository;
import com.solano.eidoscore.service.legal.mapper.LegalEntityMapper;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merge входящих данных юрлица в существующую запись. Тонкий фасад над
 * обобщённым {@link RecordMerger} — правила (по-полевой merge по trust,
 * grey-zone → tentative, архив после save) те же, что у физлиц.
 */
@Service
public class LegalEntityMerger {

    private final RecordMerger<LegalEntityGoldenRecordDto, LegalEntityRecord, LegalEntityFieldMeta> engine;

    public LegalEntityMerger(
            LegalEntityRecordRepository recordRepository,
            LegalEntityFieldMetaRepository fieldMetaRepository,
            LegalEntitySnapshotter snapshotter,
            LegalEntityMapper mapper
    ) {
        this.engine = new RecordMerger<>(
                mapper,
                LegalEntityRecord::getGrLegalEntityId,
                fieldMetaRepository::findByIdGrLegalEntityId,
                // saveAndFlush: @Version инкрементируется на flush — иначе архивная
                // ревизия пишется со старым номером версии.
                recordRepository::saveAndFlush,
                fieldMetaRepository::saveAll,
                snapshotter::archive,
                snapshotter::tentativeFromIncoming
        );
    }

    @Transactional
    public LegalEntityRecord execute(LegalEntityRecord existing,
                                     LegalEntityGoldenRecordDto incoming,
                                     Source incomingSource) {
        return engine.execute(existing, incoming, incomingSource);
    }
}
