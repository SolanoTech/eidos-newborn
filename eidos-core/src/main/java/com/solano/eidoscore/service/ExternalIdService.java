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
import com.solano.eidoscore.entity.GoldenRecordExternalId;
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.exception.BadRequestException;
import com.solano.eidoscore.exception.NotFoundException;
import com.solano.eidoscore.repository.GoldenRecordExternalIdRepository;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.repository.SourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD внешних идентификаторов Golden Record. Маршруты внешнего API.
 */
@Service
public class ExternalIdService {

    private final GoldenRecordExternalIdRepository externalIdRepository;
    private final GoldenRecordRepository goldenRecordRepository;
    private final SourceRepository sourceRepository;

    public ExternalIdService(
            GoldenRecordExternalIdRepository externalIdRepository,
            GoldenRecordRepository goldenRecordRepository,
            SourceRepository sourceRepository
    ) {
        this.externalIdRepository = externalIdRepository;
        this.goldenRecordRepository = goldenRecordRepository;
        this.sourceRepository = sourceRepository;
    }

    @Transactional
    public GoldenRecordExternalId create(String grClientId, String sourceName, String externalId) {
        Source source = resolveSource(sourceName);
        GoldenRecord goldenRecord = resolveGoldenRecord(grClientId);
        externalIdRepository.findByGoldenRecordGrClientIdAndSourceId(grClientId, source.getId())
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "External ID for source " + sourceName
                                    + " and client " + grClientId + " already exists");
                });
        return externalIdRepository.save(
                GoldenRecordExternalId.builder()
                        .goldenRecord(goldenRecord)
                        .source(source)
                        .externalId(externalId)
                        .isActive(Boolean.TRUE)
                        .build()
        );
    }

    @Transactional
    public GoldenRecordExternalId updateByGoldenRecordUuid(String grClientId, String sourceName, String externalId) {
        Source source = resolveSource(sourceName);
        GoldenRecordExternalId entity = externalIdRepository
                .findByGoldenRecordGrClientIdAndSourceId(grClientId, source.getId())
                .orElseThrow(() -> new NotFoundException(
                        "External ID for source " + sourceName + " and client " + grClientId + " does not exist"));
        entity.setExternalId(externalId);
        return externalIdRepository.save(entity);
    }

    @Transactional
    public GoldenRecordExternalId updateByOldExternalId(String sourceName, String oldExternalId, String newExternalId) {
        Source source = resolveSource(sourceName);
        GoldenRecordExternalId entity = externalIdRepository
                .findBySourceIdAndExternalId(source.getId(), oldExternalId)
                .orElseThrow(() -> new NotFoundException(
                        "External ID " + oldExternalId + " for source " + sourceName + " does not exist"));
        entity.setExternalId(newExternalId);
        return externalIdRepository.save(entity);
    }

    private Source resolveSource(String sourceName) {
        return sourceRepository.findBySourceName(sourceName)
                .orElseThrow(() -> new NotFoundException("Source Not Found"));
    }

    private GoldenRecord resolveGoldenRecord(String grClientId) {
        return goldenRecordRepository.findById(grClientId)
                .orElseThrow(() -> new NotFoundException("Golden Record " + grClientId + " not found"));
    }
}
