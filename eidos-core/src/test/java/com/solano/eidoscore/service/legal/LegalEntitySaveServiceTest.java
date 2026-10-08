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

import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.entity.legal.LegalEntityExternalId;
import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.exception.NotFoundException;
import com.solano.eidoscore.repository.SourceRepository;
import com.solano.eidoscore.repository.legal.LegalEntityExternalIdRepository;
import com.solano.eidoscore.repository.legal.LegalEntityRecordRepository;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Специфика вертикали юрлиц в save-потоке: матчинг идёт по ИНН, а не по
 * составной identity. Правила merge покрыты тестами движка и здесь не
 * дублируются.
 */
@ExtendWith(MockitoExtension.class)
class LegalEntitySaveServiceTest {

    @Mock SourceRepository sourceRepository;
    @Mock LegalEntityRecordRepository recordRepository;
    @Mock LegalEntityExternalIdRepository externalIdRepository;
    @Mock LegalEntityCreator creator;
    @Mock LegalEntityMerger merger;
    @Mock LegalEntitySnapshotter snapshotter;

    private LegalEntitySaveService service;

    @BeforeEach
    void setUp() {
        service = new LegalEntitySaveService(
                sourceRepository, recordRepository, externalIdRepository,
                creator, merger, snapshotter);
    }

    @Test
    void unknownSource_writesTentative_andThrowsNotFound() {
        when(sourceRepository.findBySourceName("ghost")).thenReturn(Optional.empty());
        LegalEntityGoldenRecordDto dto = dto("123456789");

        assertThatThrownBy(() -> service.execute(dto, "ghost", "EXT-1"))
                .isInstanceOf(NotFoundException.class);

        verify(snapshotter).tentativeFromDto(dto, "ghost");
        verify(creator, never()).execute(any(), any(), anyString());
        verify(merger, never()).execute(any(), any(), any());
    }

    @Test
    void noMatchByExternalIdOrInn_createsNewRecord() {
        Source source = source();
        when(sourceRepository.findBySourceName("payme")).thenReturn(Optional.of(source));
        when(externalIdRepository.findBySourceIdAndExternalId(1, "EXT-1")).thenReturn(Optional.empty());
        when(recordRepository.findByGrInn("123456789")).thenReturn(Optional.empty());
        LegalEntityGoldenRecordDto dto = dto("123456789");

        service.execute(dto, "payme", "EXT-1");

        verify(creator).execute(dto, source, "EXT-1");
        verify(merger, never()).execute(any(), any(), any());
    }

    @Test
    void sameInnDifferentName_mergesInsteadOfCreating() {
        Source source = source();
        LegalEntityRecord existing = new LegalEntityRecord();
        existing.setGrLegalEntityId("LE_existing");
        existing.setGrInn("123456789");
        existing.setGrFullName("ООО \"Старое название\"");

        when(sourceRepository.findBySourceName("payme")).thenReturn(Optional.of(source));
        when(externalIdRepository.findBySourceIdAndExternalId(1, "EXT-9")).thenReturn(Optional.empty());
        when(recordRepository.findByGrInn("123456789")).thenReturn(Optional.of(existing));

        LegalEntityGoldenRecordDto dto = dto("123456789");
        dto.setGrFullName("ООО \"Новое название\"");

        service.execute(dto, "payme", "EXT-9");

        verify(merger).execute(existing, dto, source);
        verify(creator, never()).execute(any(), any(), anyString());
    }

    @Test
    void matchByExternalId_takesPrecedenceOverInnLookup() {
        Source source = source();
        LegalEntityRecord existing = new LegalEntityRecord();
        existing.setGrLegalEntityId("LE_by_ext");

        LegalEntityExternalId link = LegalEntityExternalId.builder()
                .legalEntity(existing).source(source).externalId("EXT-1").build();

        when(sourceRepository.findBySourceName("payme")).thenReturn(Optional.of(source));
        when(externalIdRepository.findBySourceIdAndExternalId(1, "EXT-1")).thenReturn(Optional.of(link));

        service.execute(dto("123456789"), "payme", "EXT-1");

        verify(merger).execute(eq(existing), any(), eq(source));
        verify(recordRepository, never()).findByGrInn(anyString());
    }

    private static Source source() {
        Source s = new Source();
        s.setId(1);
        s.setSourceName("payme");
        s.setTrustLevel(10);
        return s;
    }

    private static LegalEntityGoldenRecordDto dto(String inn) {
        return LegalEntityGoldenRecordDto.builder()
                .grInn(inn)
                .grFullName("ООО \"Оазис Маркет\"")
                .grIsActive(Boolean.TRUE)
                .grIsBankrupt(Boolean.FALSE)
                .grOkedCode("47110")
                .grOkedName("Розничная торговля")
                .build();
    }
}
