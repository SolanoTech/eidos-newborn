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

package com.solano.stage.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solano.shared.dto.GoldenRecordDto;
import com.solano.shared.enums.Gender;
import com.solano.stage.core.EidosCoreClient;
import com.solano.stage.core.EidosCoreException;
import com.solano.stage.entity.registry.Source;
import com.solano.stage.entity.registry.EntityType;
import com.solano.stage.entity.registry.SourceContract;
import com.solano.stage.json.JsonMapperFactory;
import com.solano.stage.messaging.IngestMessage;
import com.solano.stage.mongo.RawClientDataRepository;
import com.solano.stage.repository.registry.SourceContractRepository;
import com.solano.stage.service.contract.ContractMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StagePipelineTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final ObjectMapper objectMapper = JsonMapperFactory.create();

    @Mock RawClientDataRepository rawRepository;
    @Mock SourceContractRepository contractRepository;
    @Mock ContractMapper contractMapper;
    @Mock EidosCoreClient coreClient;
    @org.mockito.Mock com.solano.stage.service.stats.IngestStatsService ingestStats;

    private StagePipeline pipeline;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        pipeline = new StagePipeline(rawRepository, contractRepository, contractMapper, validator, coreClient, ingestStats);
    }

    @Test
    void happyPath_persistsRaw_maps_validates_andForwards() throws Exception {
        IngestMessage message = message("tieto", "{\"x\":1}");
        SourceContract contract = contract("tieto");
        when(contractRepository.findBySourceCodeAndEntityType("tieto", EntityType.PERSON)).thenReturn(Optional.of(contract));
        when(contractMapper.map(any(), eq(contract), eq(GoldenRecordDto.class)))
                .thenReturn(new ContractMapper.MappedRecord<>(validRecord(), "ext-1"));

        pipeline.process(message);

        verify(rawRepository).save(eq("tieto"), any(), eq("PERSON"));
        verify(coreClient).saveGoldenRecord(any(GoldenRecordDto.class), eq("tieto"), eq("ext-1"));
    }

    @Test
    void noContract_throws_andDoesNotForward() throws Exception {
        IngestMessage message = message("unknown", "{\"x\":1}");
        when(contractRepository.findBySourceCodeAndEntityType("unknown", EntityType.PERSON)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipeline.process(message))
                .isInstanceOf(StageProcessingException.class)
                .hasMessageContaining("No data contract");

        verify(rawRepository).save(eq("unknown"), any(), eq("PERSON")); // raw saved before failure
        verify(coreClient, never()).saveGoldenRecord(any(), any(), any());
    }

    @Test
    void invalidMappedRecord_failsValidation_andDoesNotForward() throws Exception {
        IngestMessage message = message("tieto", "{\"x\":1}");
        SourceContract contract = contract("tieto");
        when(contractRepository.findBySourceCodeAndEntityType("tieto", EntityType.PERSON)).thenReturn(Optional.of(contract));
        // Missing required fields → bean validation fails
        when(contractMapper.map(any(), eq(contract), eq(GoldenRecordDto.class)))
                .thenReturn(new ContractMapper.MappedRecord<>(new GoldenRecordDto(), "ext-1"));

        assertThatThrownBy(() -> pipeline.process(message))
                .isInstanceOf(StageProcessingException.class)
                .hasMessageContaining("validation failed");

        verify(coreClient, never()).saveGoldenRecord(any(), any(), any());
    }

    @Test
    void blankSource_throwsBeforePersisting() {
        IngestMessage message = new IngestMessage(objectMapper.createObjectNode(), "  ");

        assertThatThrownBy(() -> pipeline.process(message))
                .isInstanceOf(StageProcessingException.class)
                .hasMessageContaining("source");
        verify(rawRepository, never()).save(any(), any(), any());
    }

    @Test
    void coreFailure_isWrappedAsProcessingException() throws Exception {
        IngestMessage message = message("tieto", "{\"x\":1}");
        SourceContract contract = contract("tieto");
        when(contractRepository.findBySourceCodeAndEntityType("tieto", EntityType.PERSON)).thenReturn(Optional.of(contract));
        when(contractMapper.map(any(), eq(contract), eq(GoldenRecordDto.class)))
                .thenReturn(new ContractMapper.MappedRecord<>(validRecord(), "ext-1"));
        doThrow(new EidosCoreException("HTTP 500")).when(coreClient).saveGoldenRecord(any(), any(), any());

        assertThatThrownBy(() -> pipeline.process(message))
                .isInstanceOf(StageProcessingException.class)
                .hasMessageContaining("eidos-core rejected");
    }

    private IngestMessage message(String source, String dataJson) throws Exception {
        return new IngestMessage(objectMapper.readTree(dataJson), source);
    }

    private static SourceContract contract(String code) {
        return SourceContract.builder()
                .source(Source.builder().id(1L).code(code).name(code).token("t-" + code).trustLevel(50).build())
                .build();
    }

    private static GoldenRecordDto validRecord() {
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
                .build();
    }
}
