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

import com.solano.eidoscore.entity.TentativeReason;
import com.solano.eidoscore.entity.legal.LegalEntityArchive;
import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.entity.legal.TentativeLegalEntity;
import com.solano.eidoscore.repository.legal.LegalEntityArchiveRepository;
import com.solano.eidoscore.repository.legal.TentativeLegalEntityRepository;
import com.solano.eidoscore.service.legal.mapper.LegalEntityMapper;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Снимки записи юрлица — архивные ревизии и tentative. Зеркало
 * {@code GoldenRecordSnapshotter}: копирование поле-в-поле через
 * {@link BeanUtils#copyProperties}, имена бизнес-свойств у
 * {@link LegalEntityRecord} и наследников {@code LegalEntitySnapshotFields}
 * совпадают.
 */
@Service
public class LegalEntitySnapshotter {

    /** Технические поля, которые не должны попадать в снимок. */
    private static final String[] IGNORED_ON_COPY = {
            "version", "createdAt", "updatedAt", "nameNormalized"
    };

    private final LegalEntityArchiveRepository archiveRepository;
    private final TentativeLegalEntityRepository tentativeRepository;
    private final LegalEntityMapper mapper;

    public LegalEntitySnapshotter(
            LegalEntityArchiveRepository archiveRepository,
            TentativeLegalEntityRepository tentativeRepository,
            LegalEntityMapper mapper
    ) {
        this.archiveRepository = archiveRepository;
        this.tentativeRepository = tentativeRepository;
        this.mapper = mapper;
    }

    /** Снимок записи после изменения; {@code archivedVersion} = текущая версия. */
    @Transactional
    public LegalEntityArchive archive(LegalEntityRecord record) {
        LegalEntityArchive snapshot = new LegalEntityArchive();
        BeanUtils.copyProperties(record, snapshot, IGNORED_ON_COPY);
        snapshot.setGrLegalEntityId(record.getGrLegalEntityId());
        snapshot.setArchivedVersion(record.getVersion());
        return archiveRepository.save(snapshot);
    }

    /**
     * Tentative для grey-zone: сохраняем ВХОДЯЩИЙ снимок — значения
     * конфликтующего источника (текущее состояние и так лежит в основной
     * таблице, оператору нужны именно предложенные значения).
     */
    @Transactional
    public TentativeLegalEntity tentativeFromIncoming(LegalEntityGoldenRecordDto incoming,
                                                      String grLegalEntityId, String sourceName) {
        TentativeLegalEntity tentative = new TentativeLegalEntity();
        LegalEntityRecord scratch = new LegalEntityRecord();
        mapper.copyToEntity(incoming, scratch);
        BeanUtils.copyProperties(scratch, tentative, IGNORED_ON_COPY);
        tentative.setGrLegalEntityId(grLegalEntityId);
        tentative.setReason(TentativeReason.GREY_ZONE_CONFLICT);
        tentative.setSourceName(sourceName);
        return tentativeRepository.save(tentative);
    }

    /**
     * Tentative для незарегистрированного источника: записи ещё нет, id = null.
     *
     * <p>{@code REQUIRES_NEW} принципиально: save-поток сразу после этой записи
     * бросает 404, и в общей транзакции снимок откатился бы вместе с
     * исключением.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TentativeLegalEntity tentativeFromDto(LegalEntityGoldenRecordDto dto, String sourceName) {
        TentativeLegalEntity tentative = new TentativeLegalEntity();
        LegalEntityRecord scratch = new LegalEntityRecord();
        mapper.copyToEntity(dto, scratch);
        BeanUtils.copyProperties(scratch, tentative, IGNORED_ON_COPY);
        tentative.setReason(TentativeReason.UNKNOWN_SOURCE);
        tentative.setSourceName(sourceName);
        return tentativeRepository.save(tentative);
    }
}
