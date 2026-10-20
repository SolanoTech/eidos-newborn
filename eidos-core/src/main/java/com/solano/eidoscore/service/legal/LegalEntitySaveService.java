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

import com.solano.eidoscore.engine.RecordSaveFlow;
import com.solano.eidoscore.entity.legal.LegalEntityExternalId;
import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.repository.SourceRepository;
import com.solano.eidoscore.repository.legal.LegalEntityExternalIdRepository;
import com.solano.eidoscore.repository.legal.LegalEntityRecordRepository;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import com.solano.eidoscore.search.RecordChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Оркестратор save-операции для записи юрлица. Тонкий фасад над
 * {@link RecordSaveFlow}; специфика типа — стратегия точного матчинга:
 * <b>ИНН самодостаточен</b>, поэтому смена названия мерчанта не порождает
 * новую запись, а приводит к merge существующей.
 */
@Service
public class LegalEntitySaveService {

    private final LegalEntityRecordRepository recordRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RecordSaveFlow<LegalEntityGoldenRecordDto, LegalEntityRecord> flow;

    public LegalEntitySaveService(
            SourceRepository sourceRepository,
            LegalEntityRecordRepository recordRepository,
            LegalEntityExternalIdRepository externalIdRepository,
            LegalEntityCreator creator,
            LegalEntityMerger merger,
            LegalEntitySnapshotter snapshotter,
            ApplicationEventPublisher eventPublisher
    ) {
        this.recordRepository = recordRepository;
        this.eventPublisher = eventPublisher;
        this.flow = new RecordSaveFlow<>(
                sourceRepository::findBySourceName,
                (source, externalId) -> externalIdRepository
                        .findBySourceIdAndExternalId(source.getId(), externalId)
                        .map(LegalEntityExternalId::getLegalEntity),
                this::matchByInn,
                snapshotter::tentativeFromDto,
                creator::execute,
                merger::execute
        );
    }

    @Transactional
    public LegalEntityRecord execute(LegalEntityGoldenRecordDto dto, String sourceName, String externalId) {
        LegalEntityRecord record = flow.execute(dto, sourceName, externalId);
        eventPublisher.publishEvent(new RecordChanged.Merchant(record));
        return record;
    }

    private Optional<LegalEntityRecord> matchByInn(LegalEntityGoldenRecordDto dto) {
        return dto.getGrInn() == null
                ? Optional.empty()
                : recordRepository.findByGrInn(dto.getGrInn());
    }
}
