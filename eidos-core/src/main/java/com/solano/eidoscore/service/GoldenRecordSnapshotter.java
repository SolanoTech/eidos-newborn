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

import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.entity.GoldenRecordArchive;
import com.solano.eidoscore.entity.TentativeGoldenRecord;
import com.solano.eidoscore.entity.TentativeReason;
import com.solano.eidoscore.repository.GoldenRecordArchiveRepository;
import com.solano.eidoscore.repository.TentativeGoldenRecordRepository;
import com.solano.eidoscore.service.mapper.GoldenRecordMapper;
import com.solano.shared.dto.GoldenRecordDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Создаёт «снимки» Golden Record — архивные ревизии и tentative-записи.
 *
 * <p>Используется:</p>
 * <ul>
 *   <li>{@link GoldenRecordMerger} — после каждого изменения вызывает
 *       {@link #archive(GoldenRecord)}; при grey-zone конфликте вызывает
 *       {@link #tentativeFromGoldenRecord}.</li>
 *   <li>{@link GoldenRecordSaveService} — при unknown source вызывает
 *       {@link #tentativeFromDto}.</li>
 * </ul>
 *
 * <p>Копирование поле-в-поле сделано через {@link BeanUtils#copyProperties}:
 * имена бизнес-свойств у {@link GoldenRecord} и
 * {@code GoldenRecordSnapshotFields}-наследников полностью совпадают.
 * Технические поля (id, version, timestamps) исключены из копирования.</p>
 */
@Service
public class GoldenRecordSnapshotter {

    /** Технические поля, которые не должны попадать в снимок при копировании из GR. */
    private static final String[] IGNORED_ON_COPY = {
            "version", "createdAt", "updatedAt"
    };

    private final GoldenRecordArchiveRepository archiveRepository;
    private final TentativeGoldenRecordRepository tentativeRepository;
    private final GoldenRecordMapper mapper;

    public GoldenRecordSnapshotter(
            GoldenRecordArchiveRepository archiveRepository,
            TentativeGoldenRecordRepository tentativeRepository,
            GoldenRecordMapper mapper
    ) {
        this.archiveRepository = archiveRepository;
        this.tentativeRepository = tentativeRepository;
        this.mapper = mapper;
    }

    /**
     * Записать снимок Golden Record после её изменения.
     * {@code archivedVersion} = текущее {@link GoldenRecord#getVersion()}.
     */
    @Transactional
    public GoldenRecordArchive archive(GoldenRecord gr) {
        GoldenRecordArchive snapshot = new GoldenRecordArchive();
        BeanUtils.copyProperties(gr, snapshot, IGNORED_ON_COPY);
        // grClientId намеренно не в IGNORED_ON_COPY — он копируется как обычное
        // имя свойства, потому что в Archive это просто строка-FK, а не PK.
        snapshot.setGrClientId(gr.getGrClientId());
        snapshot.setArchivedVersion(gr.getVersion());
        return archiveRepository.save(snapshot);
    }

    /**
     * Tentative для grey-zone: сохраняем ВХОДЯЩИЙ снимок — значения
     * конфликтующего источника. Текущее состояние и так живёт в
     * {@code golden_record}; для разрешения конфликта оператору нужны именно
     * предложенные значения (сравнение «текущее vs входящее»).
     */
    @Transactional
    public TentativeGoldenRecord tentativeFromIncoming(GoldenRecordDto incoming, String grClientId, String sourceName) {
        TentativeGoldenRecord tentative = new TentativeGoldenRecord();
        GoldenRecord scratch = new GoldenRecord();
        mapper.copyToEntity(incoming, scratch);
        BeanUtils.copyProperties(scratch, tentative, IGNORED_ON_COPY);
        tentative.setGrClientId(grClientId);
        tentative.setReason(TentativeReason.GREY_ZONE_CONFLICT);
        tentative.setSourceName(sourceName);
        return tentativeRepository.save(tentative);
    }

    /**
     * Tentative для unknown source: заполняем из DTO. Сам GR ещё не создан,
     * поэтому {@code grClientId} остаётся null.
     *
     * <p>{@code REQUIRES_NEW} принципиально: вызывающий save-поток сразу после
     * этой записи бросает 404, и в общей транзакции снимок откатился бы вместе
     * с исключением — очередь «неизвестный источник» всегда оставалась пустой.
     * Собственная транзакция коммитится независимо от исхода вызова.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TentativeGoldenRecord tentativeFromDto(GoldenRecordDto dto, String sourceName) {
        TentativeGoldenRecord tentative = new TentativeGoldenRecord();
        // Промежуточный GR используется только как удобный конвертер DTO → 43 поля.
        // Он никуда не сохраняется.
        GoldenRecord scratch = new GoldenRecord();
        mapper.copyToEntity(dto, scratch);
        BeanUtils.copyProperties(scratch, tentative, IGNORED_ON_COPY);
        tentative.setReason(TentativeReason.UNKNOWN_SOURCE);
        tentative.setSourceName(sourceName);
        return tentativeRepository.save(tentative);
    }
}
