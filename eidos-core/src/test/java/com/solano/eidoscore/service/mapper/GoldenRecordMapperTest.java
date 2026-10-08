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

package com.solano.eidoscore.service.mapper;

import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.shared.dto.GoldenRecordDto;
import com.solano.shared.enums.Gender;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoldenRecordMapperTest {

    private final GoldenRecordMapper mapper = new GoldenRecordMapper();

    @Test
    void copyToEntity_copiesStrings_dates_andEnum() {
        GoldenRecordDto dto = baseDto();
        GoldenRecord entity = new GoldenRecord();

        List<String> copied = mapper.copyToEntity(dto, entity);

        assertThat(entity.getGrMobilePhoneMain()).isEqualTo("998901234567");
        assertThat(entity.getGrFirstName()).isEqualTo("John");
        assertThat(entity.getGrLastName()).isEqualTo("Doe");
        assertThat(entity.getGrPinfl()).isEqualTo("12345678901234");
        assertThat(entity.getGrGender()).isEqualTo(Gender.MALE);
        assertThat(entity.getGrBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15));
        assertThat(entity.getGrDocIssuedDate()).isEqualTo(LocalDate.of(2020, 1, 31));
        assertThat(copied).contains("grMobilePhoneMain", "grFirstName", "grLastName", "grPinfl",
                "grGender", "grBirthDate", "grDocIssuedDate");
    }

    @Test
    void copyField_byKnownName_writesSingleField() {
        GoldenRecordDto dto = baseDto();
        dto.setGrMiddleName("Olegovich");

        GoldenRecord entity = new GoldenRecord();
        mapper.copyField(dto, entity, "grMiddleName");

        assertThat(entity.getGrMiddleName()).isEqualTo("Olegovich");
        assertThat(entity.getGrFirstName()).isNull();
    }

    @Test
    void copyField_unknownName_isNoOp() {
        GoldenRecord entity = new GoldenRecord();
        mapper.copyField(baseDto(), entity, "definitely_not_a_field");
        assertThat(entity.getGrFirstName()).isNull();
    }

    @Test
    void readFromDto_returnsTypedValue() {
        GoldenRecordDto dto = baseDto();
        assertThat(mapper.readFromDto(dto, "grBirthDate")).isEqualTo(LocalDate.of(1990, 6, 15));
        assertThat(mapper.readFromDto(dto, "grGender")).isEqualTo(Gender.MALE);
        assertThat(mapper.readFromDto(dto, "nope")).isNull();
    }

    @Test
    void knownFieldNames_coverEveryMappedField() {
        // Sanity check: catch accidental removals/additions in the mapper registry.
        assertThat(mapper.knownFieldNames())
                .contains("grMobilePhoneMain", "grContactsEmail",
                        "grFirstName", "grMiddleName", "grLastName",
                        "grPinfl", "grGender",
                        "grBirthDate", "grBirthPlace", "grBirthCountry", "grBirthCountryId",
                        "grNationality", "grNationalityId", "grCitizenship", "grCitizenshipId",
                        "grDocPassData", "grDocIssuedBy", "grDocIssuedById",
                        "grDocIssuedDate", "grDocExpiryDate",
                        "grAddrPermanentAddress", "grAddrTemporaryAddress",
                        "grAddrPermRegRegion", "grAddrPermRegCountry",
                        "grAddrPermRegRegistrationDate",
                        "grAddrTempRegRegion", "grAddrTempRegDateTill");
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
