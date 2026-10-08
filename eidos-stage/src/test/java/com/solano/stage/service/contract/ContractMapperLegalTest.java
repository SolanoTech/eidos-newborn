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

package com.solano.stage.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solano.stage.json.JsonMapperFactory;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import com.solano.stage.entity.registry.FieldDataType;
import com.solano.stage.entity.registry.SourceContract;
import com.solano.stage.entity.registry.SourceField;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Маппинг в контракт юрлица: тот же конструктор, другой целевой DTO. Отдельно
 * проверяются типы, которых не было у физлиц — массивы и структуры переносятся
 * поддеревом JSON, а не построчным приведением.
 */
class ContractMapperLegalTest {

    // Тот же ObjectMapper, что в приложении (JSR-310 и прочие модули).
    private final ObjectMapper objectMapper = JsonMapperFactory.create();
    private final ContractMapper mapper = new ContractMapper(objectMapper);

    @Test
    void mapsScalarsAndCollectionsIntoLegalEntityContract() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {
                  "merchant_id": "MERCH-1",
                  "tin": "301234567",
                  "name": "ООО \\"Оазис Маркет\\"",
                  "reg_date": "14.03.2019",
                  "capital": "50000000.00",
                  "active": "true",
                  "score": "87",
                  "contacts": { "phones": ["998901234567", "998712001122"] },
                  "owners": [ { "name": "Каримов А.", "sharePercent": 100.00 } ]
                }
                """);

        SourceContract contract = contract(
                field("tin", "GR_Inn", FieldDataType.STRING),
                field("name", "GR_FullName", FieldDataType.STRING),
                dateField("reg_date", "GR_RegistrationDate", "dd.MM.yyyy"),
                field("capital", "GR_StatutoryFund", FieldDataType.STRING),
                field("active", "GR_IsActive", FieldDataType.BOOLEAN),
                field("score", "GR_TrustScore", FieldDataType.INTEGER),
                field("contacts.phones", "GR_Phones", FieldDataType.ARRAY),
                field("owners", "GR_Founders", FieldDataType.STRUCT)
        );

        ContractMapper.MappedRecord<LegalEntityGoldenRecordDto> result =
                mapper.map(data, contract, LegalEntityGoldenRecordDto.class);

        LegalEntityGoldenRecordDto record = result.record();
        assertThat(result.clientSourceIdentificator()).isEqualTo("MERCH-1");
        assertThat(record.getGrInn()).isEqualTo("301234567");
        assertThat(record.getGrRegistrationDate()).isEqualTo(java.time.LocalDate.of(2019, 3, 14));
        assertThat(record.getGrStatutoryFund()).isEqualByComparingTo(new BigDecimal("50000000.00"));
        assertThat(record.getGrIsActive()).isTrue();
        assertThat(record.getGrTrustScore()).isEqualTo((short) 87);
        assertThat(record.getGrPhones()).containsExactly("998901234567", "998712001122");
        assertThat(record.getGrFounders()).hasSize(1);
        assertThat(record.getGrFounders().get(0).getName()).isEqualTo("Каримов А.");
    }

    @Test
    void missingCollectionField_isOmittedRatherThanEmptied() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {"merchant_id": "MERCH-2", "tin": "301234567"}
                """);
        SourceContract contract = contract(
                field("tin", "GR_Inn", FieldDataType.STRING),
                field("contacts.phones", "GR_Phones", FieldDataType.ARRAY)
        );

        LegalEntityGoldenRecordDto record =
                mapper.map(data, contract, LegalEntityGoldenRecordDto.class).record();

        assertThat(record.getGrInn()).isEqualTo("301234567");
        assertThat(record.getGrPhones()).isNull();
    }

    private static SourceContract contract(SourceField... fields) {
        SourceContract contract = SourceContract.builder()
                .clientIdentifierField("merchant_id")
                .fields(new java.util.ArrayList<>(List.of(fields)))
                .build();
        contract.getFields().forEach(f -> f.setContract(contract));
        return contract;
    }

    private static SourceField field(String source, String target, FieldDataType type) {
        SourceField f = new SourceField();
        f.setSourceFieldName(source);
        f.setTargetGrField(target);
        f.setDataType(type);
        return f;
    }

    private static SourceField dateField(String source, String target, String format) {
        SourceField f = field(source, target, FieldDataType.DATE);
        f.setSourceDateFormat(format);
        return f;
    }
}
