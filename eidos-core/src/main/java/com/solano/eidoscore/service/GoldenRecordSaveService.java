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

import com.solano.eidoscore.engine.RecordSaveFlow;
import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.entity.GoldenRecordExternalId;
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.repository.GoldenRecordExternalIdRepository;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.repository.SourceRepository;
import com.solano.eidoscore.crypto.BlindIndex;
import com.solano.eidoscore.crypto.DepersonalizationService;
import com.solano.shared.dto.GoldenRecordDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Оркестратор save-операции для Golden Record физлица.
 *
 * <p>Тонкий фасад над обобщённым {@link RecordSaveFlow}: сам поток
 * (источник → external id → точный матчинг → create/merge, unknown source →
 * tentative + 404) — в движке. Специфика типа здесь — стратегия точного
 * матчинга: фамилия + имя + дата рождения + ПИНФЛ, затем паспорт.</p>
 */
@Service
public class GoldenRecordSaveService {

    private final GoldenRecordRepository goldenRecordRepository;
    private final BlindIndex blindIndex;
    private final RecordSaveFlow<GoldenRecordDto, GoldenRecord> flow;

    public GoldenRecordSaveService(
            SourceRepository sourceRepository,
            GoldenRecordRepository goldenRecordRepository,
            GoldenRecordExternalIdRepository externalIdRepository,
            GoldenRecordCreator creator,
            GoldenRecordMerger merger,
            GoldenRecordSnapshotter snapshotter,
            BlindIndex blindIndex,
            DepersonalizationService depersonalizationService
    ) {
        this.goldenRecordRepository = goldenRecordRepository;
        this.blindIndex = blindIndex;
        this.flow = new RecordSaveFlow<>(
                sourceRepository::findBySourceName,
                (source, externalId) -> externalIdRepository
                        .findBySourceIdAndExternalId(source.getId(), externalId)
                        .map(GoldenRecordExternalId::getGoldenRecord),
                this::accurateSearch,
                snapshotter::tentativeFromDto,
                creator::execute,
                (existing, dto, source) -> mergeKeepingForm(existing, dto, source, merger, depersonalizationService)
        );
    }

    @Transactional
    public GoldenRecord execute(GoldenRecordDto dto, String sourceName, String clientSourceIdentifier) {
        return flow.execute(dto, sourceName, clientSourceIdentifier);
    }

    /**
     * Сливает входящее, сохраняя форму хранения записи.
     *
     * <p>Если запись обезличена, её значения сначала раскрываются: слияние
     * сравнивает пришедшее с текущим, а токен не равен никакому присланному
     * значению — без раскрытия каждое поле выглядело бы изменившимся, и
     * происхождение переписывалось бы при каждом пуше. После слияния запись
     * обезличивается снова.</p>
     *
     * <p>Всё происходит в транзакции приёма: при сбое на любом шаге запись
     * останется обезличенной, открытых значений в базе не задержится.</p>
     *
     * <p>Слепой индекс при этом пересчитывается сам: к моменту сохранения
     * запись раскрыта, и слушатель считает его от открытых значений.</p>
     */
    private static GoldenRecord mergeKeepingForm(
            GoldenRecord existing,
            GoldenRecordDto dto,
            com.solano.eidoscore.entity.Source source,
            GoldenRecordMerger merger,
            DepersonalizationService depersonalization
    ) {
        boolean wasTokenized = existing.isTokenized();
        if (wasTokenized) {
            depersonalization.restore(existing);
        }
        GoldenRecord merged = merger.execute(existing, dto, source);
        if (wasTokenized) {
            depersonalization.depersonalize(merged);
        }
        return merged;
    }

    private Optional<GoldenRecord> accurateSearch(GoldenRecordDto dto) {
        // PINFL и birthDate обязательны на уровне DTO, паспорт тоже @NotBlank,
        // поэтому достаточно попробовать сначала по PINFL.
        //
        // Сравниваются слепые индексы, а не значения: правило то же — фамилия,
        // имя, дата рождения и ПИНФЛ целиком, — но переживёт токенизацию.
        return goldenRecordRepository
                .findByPdBiIdentity(blindIndex.identity(
                        dto.getGrLastName(), dto.getGrFirstName(),
                        dto.getGrBirthDate(), dto.getGrPinfl()))
                .or(() -> goldenRecordRepository
                        .findByPdBiIdentityDoc(blindIndex.identityByDocument(
                                dto.getGrLastName(), dto.getGrFirstName(),
                                dto.getGrBirthDate(), dto.getGrDocPassData())));
    }
}
