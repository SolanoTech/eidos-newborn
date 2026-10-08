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
import com.solano.eidoscore.exception.NotFoundException;
import com.solano.eidoscore.repository.GoldenRecordExternalIdRepository;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.repository.SourceRepository;
import com.solano.shared.dto.GoldenRecordDto;
import com.solano.shared.enums.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoldenRecordSaveServiceTest {

    @Mock SourceRepository sourceRepository;
    @Mock GoldenRecordRepository goldenRecordRepository;
    @Mock GoldenRecordExternalIdRepository externalIdRepository;
    @Mock GoldenRecordCreator creator;
    @Mock GoldenRecordMerger merger;
    @Mock GoldenRecordSnapshotter snapshotter;
    @Mock com.solano.eidoscore.repository.vault.PdBlindIndexKeyRepository blindIndexKeyRepository;
    @Mock com.solano.eidoscore.crypto.DepersonalizationService depersonalizationService;

    private GoldenRecordSaveService service;
    private com.solano.eidoscore.crypto.BlindIndex blindIndex;

    @BeforeEach
    void setUp() {
        blindIndex = com.solano.eidoscore.crypto.BlindIndexFixture.inMemory(blindIndexKeyRepository);
        service = new GoldenRecordSaveService(
                sourceRepository,
                goldenRecordRepository,
                externalIdRepository,
                creator,
                merger,
                snapshotter,
                blindIndex,
                depersonalizationService
        );
    }

    @Test
    void unknownSource_writesTentative_andThrowsNotFound() {
        when(sourceRepository.findBySourceName("MISSING")).thenReturn(Optional.empty());
        GoldenRecordDto dto = baseDto();

        assertThatThrownBy(() -> service.execute(dto, "MISSING", "ext-1"))
                .isInstanceOf(NotFoundException.class);

        verify(snapshotter).tentativeFromDto(dto, "MISSING");
        verify(creator, never()).execute(any(), any(), any());
        verify(merger, never()).execute(any(), any(), any());
    }

    @Test
    void noExternalIdMatch_noAccurateMatch_callsCreator() {
        Source source = source(1, "TIETO", 50);
        when(sourceRepository.findBySourceName("TIETO")).thenReturn(Optional.of(source));
        when(externalIdRepository.findBySourceIdAndExternalId(eq(1), anyString())).thenReturn(Optional.empty());
        when(goldenRecordRepository.findByPdBiIdentity(anyString())).thenReturn(Optional.empty());
        when(goldenRecordRepository.findByPdBiIdentityDoc(anyString())).thenReturn(Optional.empty());

        GoldenRecordDto dto = baseDto();
        service.execute(dto, "TIETO", "ext-1");

        verify(creator, times(1)).execute(dto, source, "ext-1");
        verify(merger, never()).execute(any(), any(), any());
    }

    @Test
    void externalIdMatch_callsMerger() {
        Source source = source(1, "TIETO", 50);
        when(sourceRepository.findBySourceName("TIETO")).thenReturn(Optional.of(source));

        GoldenRecord existing = new GoldenRecord();
        existing.setGrClientId("GR_existing");
        GoldenRecordExternalId ext = GoldenRecordExternalId.builder()
                .goldenRecord(existing)
                .source(source)
                .externalId("ext-1")
                .isActive(Boolean.TRUE)
                .build();
        when(externalIdRepository.findBySourceIdAndExternalId(1, "ext-1")).thenReturn(Optional.of(ext));

        GoldenRecordDto dto = baseDto();
        service.execute(dto, "TIETO", "ext-1");

        verify(merger).execute(existing, dto, source);
        verify(creator, never()).execute(any(), any(), any());
    }

    @Test
    void accurateSearchByPinfl_matches_callsMerger() {
        Source source = source(1, "TIETO", 50);
        when(sourceRepository.findBySourceName("TIETO")).thenReturn(Optional.of(source));
        when(externalIdRepository.findBySourceIdAndExternalId(eq(1), anyString())).thenReturn(Optional.empty());

        GoldenRecord existing = new GoldenRecord();
        existing.setGrClientId("GR_match");
        // Ищем именно по тому индексу, который сервис посчитает из DTO.
        GoldenRecordDto probe = baseDto();
        when(goldenRecordRepository.findByPdBiIdentity(blindIndex.identity(
                probe.getGrLastName(), probe.getGrFirstName(),
                probe.getGrBirthDate(), probe.getGrPinfl())))
                .thenReturn(Optional.of(existing));

        GoldenRecordDto dto = baseDto();
        service.execute(dto, "TIETO", "ext-1");

        verify(merger).execute(existing, dto, source);
        verify(creator, never()).execute(any(), any(), any());
    }

    // ---- helpers ----

    private static Source source(int id, String name, int trust) {
        Source s = new Source();
        s.setId(id);
        s.setSourceName(name);
        s.setTrustLevel(trust);
        return s;
    }

    private static GoldenRecordDto baseDto() {
        return GoldenRecordDto.builder()
                .grMobilePhoneMain("998901234567")
                .grFirstName("John")
                .grLastName("Doe")
                .grPinfl("12345678901234")
                .grGender(Gender.MALE)
                .grBirthDate(LocalDate.of(1990, 6, 15))
                .grCitizenship("UZ")
                .grCitizenshipId("860")
                .grDocPassData("AA1234567")
                .grDocIssuedBy("MVD")
                .grDocIssuedById("01")
                .grDocIssuedDate(LocalDate.of(2020, 1, 31))
                .grAddrPermRegRegion("Tashkent")
                .grAddrPermRegCountry("UZ")
                .grAddrPermRegRegionId("01")
                .grAddrPermRegCountryId("860")
                .grAddrPermRegDistrictId("0101")
                .grAddrPermRegRegistrationDate(LocalDate.of(2010, 5, 1))
                .grAddrTempRegRegion("Tashkent")
                .grAddrTempRegDateTill(LocalDate.of(2030, 12, 31))
                .grAddrTempRegRegionId("01")
                .grAddrTempRegDistrictId("0101")
                .build();
    }
}
