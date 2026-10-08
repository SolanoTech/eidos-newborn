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

package com.solano.shared.dto;

import com.solano.shared.dto.RecordFieldCatalog.FieldDescriptor;
import com.solano.shared.dto.RecordFieldCatalog.FieldType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Каталог полей, параметризованный классом контракта: физлица и юрлица
 * дают разные каталоги, новые Java-типы юрлиц распознаются корректно.
 */
class RecordFieldCatalogTest {

    private final RecordFieldCatalog person = RecordFieldCatalog.of(GoldenRecordDto.class);
    private final RecordFieldCatalog legal = RecordFieldCatalog.of(LegalEntityGoldenRecordDto.class);

    @Test
    void personCatalog_keepsExistingSemantics() {
        FieldDescriptor pinfl = person.byJsonName("GR_Pinfl");
        assertThat(pinfl).isNotNull();
        assertThat(pinfl.required()).isTrue();
        assertThat(pinfl.pattern()).isEqualTo("^\\d{14}$");
        assertThat(pinfl.type()).isEqualTo(FieldType.STRING);

        assertThat(person.byJsonName("GR_Gender").type()).isEqualTo(FieldType.GENDER);
        assertThat(person.byJsonName("GR_BirthDate").type()).isEqualTo(FieldType.DATE);
        assertThat(person.isValidTarget("GR_FirstName")).isTrue();
        assertThat(person.isValidTarget("GR_Inn")).isFalse();
    }

    @Test
    void legalCatalog_resolvesNewJavaTypes() {
        assertThat(legal.byJsonName("GR_Inn").type()).isEqualTo(FieldType.STRING);
        assertThat(legal.byJsonName("GR_Inn").required()).isTrue();
        assertThat(legal.byJsonName("GR_Inn").pattern()).isEqualTo("^\\d{9}$");

        assertThat(legal.byJsonName("GR_RegistrationDate").type()).isEqualTo(FieldType.DATE);
        assertThat(legal.byJsonName("GR_IsActive").type()).isEqualTo(FieldType.BOOLEAN);
        assertThat(legal.byJsonName("GR_IsActive").required()).isTrue();
        assertThat(legal.byJsonName("GR_TrustScore").type()).isEqualTo(FieldType.INTEGER);
        assertThat(legal.byJsonName("GR_CourtsTotal").type()).isEqualTo(FieldType.INTEGER);
        assertThat(legal.byJsonName("GR_StatutoryFund").type()).isEqualTo(FieldType.DECIMAL);
        assertThat(legal.byJsonName("GR_Phones").type()).isEqualTo(FieldType.STRING_ARRAY);
        assertThat(legal.byJsonName("GR_Founders").type()).isEqualTo(FieldType.STRUCT_ARRAY);
    }

    @Test
    void catalogs_areIndependentAndCached() {
        assertThat(legal.isValidTarget("GR_FirstName")).isFalse();
        assertThat(person.isValidTarget("GR_Founders")).isFalse();
        // Один и тот же класс → тот же кэшированный экземпляр.
        assertThat(RecordFieldCatalog.of(GoldenRecordDto.class)).isSameAs(person);
    }

    @Test
    void legalCatalog_containsAllSpecFields() {
        // 60 полей спецификации: все аннотированы @JsonProperty и попадают в каталог.
        assertThat(legal.fields()).hasSize(60);
        assertThat(legal.fields().stream().filter(FieldDescriptor::required))
                .extracting(FieldDescriptor::jsonName)
                .containsExactlyInAnyOrder(
                        "GR_Inn", "GR_FullName", "GR_IsActive",
                        "GR_IsBankrupt", "GR_OkedCode", "GR_OkedName");
    }
}
