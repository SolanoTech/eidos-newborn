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
import com.solano.shared.dto.GoldenRecordDto;
import com.solano.shared.enums.Gender;
import com.solano.stage.entity.registry.FieldDataType;
import com.solano.stage.entity.registry.Source;
import com.solano.stage.entity.registry.SourceContract;
import com.solano.stage.entity.registry.SourceField;
import com.solano.stage.json.JsonMapperFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContractMapperTest {

    private final ObjectMapper objectMapper = JsonMapperFactory.create();
    private final ContractMapper mapper = new ContractMapper(objectMapper);

    @Test
    void mapsRename_valueMap_dateReformat_andDefault() throws Exception {
        SourceContract contract = contract();
        // citizenship intentionally absent → default "UZ"
        JsonNode data = objectMapper.readTree("""
                {
                  "id": "ext-1",
                  "f_names": "John",
                  "surname": "Doe",
                  "phone": "998901234567",
                  "pin": "12345678901234",
                  "sex": "1",
                  "bdate": "15.06.1990"
                }
                """);

        ContractMapper.MappedRecord<GoldenRecordDto> result = mapper.map(data, contract);
        GoldenRecordDto gr = result.record();

        assertThat(result.clientSourceIdentificator()).isEqualTo("ext-1");
        assertThat(gr.getGrFirstName()).isEqualTo("John");
        assertThat(gr.getGrLastName()).isEqualTo("Doe");
        assertThat(gr.getGrMobilePhoneMain()).isEqualTo("998901234567");
        assertThat(gr.getGrPinfl()).isEqualTo("12345678901234");
        assertThat(gr.getGrGender()).isEqualTo(Gender.MALE);           // "1" → "M" → MALE
        assertThat(gr.getGrBirthDate()).isEqualTo(LocalDate.of(1990, 6, 15)); // dd.MM.yyyy → ISO
        assertThat(gr.getGrCitizenship()).isEqualTo("UZ");             // default
    }

    @Test
    void valueMapMapsFemale() throws Exception {
        JsonNode data = objectMapper.readTree(
                "{\"id\":\"e\",\"f_names\":\"A\",\"surname\":\"B\",\"phone\":\"998900000000\","
                        + "\"pin\":\"00000000000000\",\"sex\":\"2\",\"bdate\":\"01.01.2000\"}");
        GoldenRecordDto gr = mapper.map(data, contract()).record();
        assertThat(gr.getGrGender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    void supportsDotPathForNestedSourceFields() throws Exception {
        SourceContract contract = SourceContract.builder().source(source()).build();
        contract.addField(SourceField.builder()
                .sourceFieldName("person.firstName").targetGrField("GR_FirstName")
                .dataType(FieldDataType.STRING).build());
        JsonNode data = objectMapper.readTree("{\"person\":{\"firstName\":\"Nested\"}}");

        GoldenRecordDto gr = mapper.map(data, contract).record();

        assertThat(gr.getGrFirstName()).isEqualTo("Nested");
    }

    private SourceContract contract() {
        SourceContract contract = SourceContract.builder().source(source()).clientIdentifierField("id").build();
        contract.addField(field("f_names", "GR_FirstName", FieldDataType.STRING, null, null));
        contract.addField(field("surname", "GR_LastName", FieldDataType.STRING, null, null));
        contract.addField(field("phone", "GR_mobilePhoneMain", FieldDataType.STRING, null, null));
        contract.addField(field("pin", "GR_Pinfl", FieldDataType.STRING, null, null));
        contract.addField(field("sex", "GR_Gender", FieldDataType.GENDER, null, Map.of("1", "M", "2", "F")));
        contract.addField(field("bdate", "GR_BirthDate", FieldDataType.DATE, "dd.MM.yyyy", null));
        SourceField citizenship = field("citizenship", "GR_Citizenship", FieldDataType.STRING, null, null);
        citizenship.setDefaultValue("UZ");
        contract.addField(citizenship);
        return contract;
    }

    private static SourceField field(String src, String target, FieldDataType type,
                                     String dateFormat, Map<String, String> valueMap) {
        return SourceField.builder()
                .sourceFieldName(src)
                .targetGrField(target)
                .dataType(type)
                .sourceDateFormat(dateFormat)
                .valueMap(valueMap == null ? Map.of() : valueMap)
                .build();
    }

    private static Source source() {
        return Source.builder().id(1L).code("test").name("Test").token("t").trustLevel(50).build();
    }
}
