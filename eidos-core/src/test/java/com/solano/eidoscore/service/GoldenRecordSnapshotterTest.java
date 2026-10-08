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
import com.solano.shared.enums.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoldenRecordSnapshotterTest {

    @Mock GoldenRecordArchiveRepository archiveRepository;
    @Mock TentativeGoldenRecordRepository tentativeRepository;

    private GoldenRecordSnapshotter snapshotter;

    @BeforeEach
    void setUp() {
        snapshotter = new GoldenRecordSnapshotter(
                archiveRepository,
                tentativeRepository,
                new GoldenRecordMapper()
        );
    }

    @Test
    void archive_copiesAllBusinessFields_andCapturesVersion() {
        GoldenRecord gr = baseRecord();
        gr.setVersion(7);

        snapshotter.archive(gr);

        ArgumentCaptor<GoldenRecordArchive> captor = ArgumentCaptor.forClass(GoldenRecordArchive.class);
        verify(archiveRepository).save(captor.capture());
        GoldenRecordArchive saved = captor.getValue();

        assertThat(saved.getGrClientId()).isEqualTo(gr.getGrClientId());
        assertThat(saved.getArchivedVersion()).isEqualTo(7);
        // Business fields propagated
        assertThat(saved.getGrFirstName()).isEqualTo("John");
        assertThat(saved.getGrLastName()).isEqualTo("Doe");
        assertThat(saved.getGrPinfl()).isEqualTo("12345678901234");
        assertThat(saved.getGrGender()).isEqualTo(Gender.MALE);
        assertThat(saved.getGrBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
    }

    @Test
    void tentativeFromIncoming_capturesReasonSourceGrClientId_andIncomingValues() {
        GoldenRecordDto incoming = baseDto();
        incoming.setGrFirstName("Johnny");

        snapshotter.tentativeFromIncoming(incoming, "GR_existing", "TIETO");

        ArgumentCaptor<TentativeGoldenRecord> captor = ArgumentCaptor.forClass(TentativeGoldenRecord.class);
        verify(tentativeRepository).save(captor.capture());
        TentativeGoldenRecord saved = captor.getValue();

        assertThat(saved.getReason()).isEqualTo(TentativeReason.GREY_ZONE_CONFLICT);
        assertThat(saved.getSourceName()).isEqualTo("TIETO");
        assertThat(saved.getGrClientId()).isEqualTo("GR_existing");
        // В снимке — ВХОДЯЩИЕ значения конфликтующего источника.
        assertThat(saved.getGrFirstName()).isEqualTo("Johnny");
        assertThat(saved.getGrLastName()).isEqualTo("Doe");
    }

    @Test
    void tentativeFromDto_unknownSource_leavesGrClientIdNull_andCopiesDtoFields() {
        GoldenRecordDto dto = baseDto();

        snapshotter.tentativeFromDto(dto, "MISTYPED_NAME");

        ArgumentCaptor<TentativeGoldenRecord> captor = ArgumentCaptor.forClass(TentativeGoldenRecord.class);
        verify(tentativeRepository).save(captor.capture());
        TentativeGoldenRecord saved = captor.getValue();

        assertThat(saved.getGrClientId()).isNull();
        assertThat(saved.getReason()).isEqualTo(TentativeReason.UNKNOWN_SOURCE);
        assertThat(saved.getSourceName()).isEqualTo("MISTYPED_NAME");
        assertThat(saved.getGrFirstName()).isEqualTo("John");
        assertThat(saved.getGrBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
    }

    // ---- helpers ----

    private static GoldenRecord baseRecord() {
        GoldenRecord gr = new GoldenRecord();
        gr.setGrClientId("GR_existing");
        gr.setGrFirstName("John");
        gr.setGrLastName("Doe");
        gr.setGrPinfl("12345678901234");
        gr.setGrGender(Gender.MALE);
        gr.setGrBirthDate(LocalDate.of(1990, 6, 15));
        return gr;
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
