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
import com.solano.eidoscore.entity.GoldenRecordFieldMeta;
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.repository.GoldenRecordExternalIdRepository;
import com.solano.eidoscore.repository.GoldenRecordFieldMetaRepository;
import com.solano.eidoscore.repository.GoldenRecordRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoldenRecordCreatorTest {

    @Mock GoldenRecordRepository goldenRecordRepository;
    @Mock GoldenRecordExternalIdRepository externalIdRepository;
    @Mock GoldenRecordFieldMetaRepository fieldMetaRepository;

    private GoldenRecordCreator creator;

    @BeforeEach
    void setUp() {
        creator = new GoldenRecordCreator(
                goldenRecordRepository,
                externalIdRepository,
                fieldMetaRepository,
                new GoldenRecordMapper()
        );
    }

    @Test
    void execute_assignsUuidStartingWithGrPrefix_andSavesEntity() {
        GoldenRecord saved = creator.execute(baseDto(), source(1, "TIETO", 50), "ext-1");

        assertThat(saved.getGrClientId()).startsWith("GR_");
        verify(goldenRecordRepository).save(saved);
    }

    @Test
    void execute_copiesAllDtoFieldsToEntity() {
        GoldenRecordDto dto = baseDto();
        dto.setGrMiddleName("Olegovich");

        GoldenRecord saved = creator.execute(dto, source(1, "TIETO", 50), "ext-1");

        assertThat(saved.getGrFirstName()).isEqualTo("John");
        assertThat(saved.getGrMiddleName()).isEqualTo("Olegovich");
        assertThat(saved.getGrBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
        assertThat(saved.getGrGender()).isEqualTo(Gender.MALE);
    }

    @Test
    void execute_persistsExternalIdAttachedToNewRecord() {
        Source source = source(7, "SFD", 80);
        creator.execute(baseDto(), source, "ext-42");

        ArgumentCaptor<GoldenRecordExternalId> captor = ArgumentCaptor.forClass(GoldenRecordExternalId.class);
        verify(externalIdRepository).save(captor.capture());
        GoldenRecordExternalId stored = captor.getValue();
        assertThat(stored.getExternalId()).isEqualTo("ext-42");
        assertThat(stored.getSource()).isEqualTo(source);
        assertThat(stored.getIsActive()).isTrue();
        assertThat(stored.getGoldenRecord().getGrClientId()).startsWith("GR_");
    }

    @Test
    void execute_writesFieldMetaForEveryMappedField() {
        Source source = source(1, "TIETO", 50);
        GoldenRecordMapper mapper = new GoldenRecordMapper();

        creator.execute(baseDto(), source, "ext-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GoldenRecordFieldMeta>> captor = ArgumentCaptor.forClass(List.class);
        verify(fieldMetaRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .hasSize(mapper.knownFieldNames().size())
                .allSatisfy(meta -> {
                    assertThat(meta.getSource()).isEqualTo(source);
                    assertThat(meta.getId().getGrClientId()).startsWith("GR_");
                });
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
