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

import com.solano.eidoscore.engine.RecordMerger;
import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.entity.GoldenRecordFieldMeta;
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.repository.GoldenRecordFieldMetaRepository;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.service.mapper.GoldenRecordMapper;
import com.solano.shared.dto.GoldenRecordDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merge входящего {@link GoldenRecordDto} в существующую {@link GoldenRecord}.
 *
 * <p>Тонкий фасад физлиц над обобщённым {@link RecordMerger}: сами правила
 * (по-полевой merge по trust, grey-zone → tentative, архив после save) живут
 * в движке; здесь — только транзакция и адаптеры портов на репозитории
 * физлиц и {@link GoldenRecordSnapshotter}.</p>
 */
@Service
public class GoldenRecordMerger {

    private final RecordMerger<GoldenRecordDto, GoldenRecord, GoldenRecordFieldMeta> engine;

    public GoldenRecordMerger(
            GoldenRecordRepository goldenRecordRepository,
            GoldenRecordFieldMetaRepository fieldMetaRepository,
            GoldenRecordSnapshotter snapshotter,
            GoldenRecordMapper mapper
    ) {
        this.engine = new RecordMerger<>(
                mapper,
                GoldenRecord::getGrClientId,
                fieldMetaRepository::findByIdGrClientId,
                // saveAndFlush, а не save: @Version инкрементируется на flush, и без
                // него архивная ревизия получала бы ещё старый номер версии.
                goldenRecordRepository::saveAndFlush,
                fieldMetaRepository::saveAll,
                snapshotter::archive,
                snapshotter::tentativeFromIncoming
        );
    }

    @Transactional
    public GoldenRecord execute(GoldenRecord existing, GoldenRecordDto incoming, Source incomingSource) {
        return engine.execute(existing, incoming, incomingSource);
    }
}
