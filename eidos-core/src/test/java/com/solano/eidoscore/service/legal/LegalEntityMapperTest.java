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

import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.service.legal.mapper.LegalEntityMapper;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import com.solano.shared.dto.LegalEntityGoldenRecordDto.Founder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Реестр аксессоров юрлица: покрывает все типы контракта, включая те, которых
 * не было у физлиц (Boolean, Short, BigDecimal, списки).
 */
class LegalEntityMapperTest {

    private final LegalEntityMapper mapper = new LegalEntityMapper();

    @Test
    void knowsAllSixtyContractFields() {
        assertThat(mapper.knownFieldNames()).hasSize(60);
        assertThat(mapper.knowsField("grInn")).isTrue();
        assertThat(mapper.knowsField("grFounders")).isTrue();
        assertThat(mapper.knowsField("grFirstName")).isFalse();
    }

    @Test
    void copyToEntity_transfersEveryTypeOfField() {
        LegalEntityGoldenRecordDto dto = fullDto();
        LegalEntityRecord entity = new LegalEntityRecord();

        List<String> copied = mapper.copyToEntity(dto, entity);

        assertThat(copied).hasSize(60);
        assertThat(entity.getGrInn()).isEqualTo("123456789");
        assertThat(entity.getGrIsActive()).isTrue();                       // Boolean
        assertThat(entity.getGrTrustScore()).isEqualTo((short) 87);        // Short
        assertThat(entity.getGrCourtsTotal()).isEqualTo(3);                // Integer
        assertThat(entity.getGrStatutoryFund())
                .isEqualByComparingTo(new BigDecimal("50000000.00"));      // BigDecimal
        assertThat(entity.getGrRegistrationDate()).isEqualTo(LocalDate.of(2019, 3, 14));
        assertThat(entity.getGrPhones()).containsExactly("998901234567", "998712001122");
        assertThat(entity.getGrFounders()).hasSize(1);
        assertThat(entity.getGrFounders().get(0).getName()).isEqualTo("Каримов А.");
    }

    @Test
    void readFromDtoAndEntity_agreeOnValues() {
        LegalEntityGoldenRecordDto dto = fullDto();
        LegalEntityRecord entity = new LegalEntityRecord();
        mapper.copyToEntity(dto, entity);

        assertThat(mapper.readFromDto(dto, "grFullName"))
                .isEqualTo(mapper.readFromEntity(entity, "grFullName"));
        assertThat(mapper.readFromDto(dto, "grPhones"))
                .isEqualTo(mapper.readFromEntity(entity, "grPhones"));
        assertThat(mapper.readFromDto(dto, "unknownField")).isNull();
    }

    @Test
    void copyEntityField_movesSingleFieldBetweenEntities() {
        LegalEntityRecord from = new LegalEntityRecord();
        from.setGrFullName("ООО \"Новое\"");
        from.setGrInn("999999999");
        LegalEntityRecord to = new LegalEntityRecord();
        to.setGrFullName("ООО \"Старое\"");
        to.setGrInn("111111111");

        mapper.copyEntityField(from, to, "grFullName");

        assertThat(to.getGrFullName()).isEqualTo("ООО \"Новое\"");
        assertThat(to.getGrInn()).isEqualTo("111111111");
    }

    private static LegalEntityGoldenRecordDto fullDto() {
        return LegalEntityGoldenRecordDto.builder()
                .grInn("123456789")
                .grFullName("ООО \"Оазис Маркет\"")
                .grShortName("Оазис")
                .grIsActive(Boolean.TRUE)
                .grIsBankrupt(Boolean.FALSE)
                .grOkedCode("47110")
                .grOkedName("Розничная торговля")
                .grRegistrationDate(LocalDate.of(2019, 3, 14))
                .grStatutoryFund(new BigDecimal("50000000.00"))
                .grTrustScore((short) 87)
                .grCourtsTotal(3)
                .grPhones(List.of("998901234567", "998712001122"))
                .grFounders(List.of(Founder.builder()
                        .name("Каримов А.")
                        .pinfl("12345678901234")
                        .sharePercent(new BigDecimal("100.00"))
                        .build()))
                .build();
    }
}
