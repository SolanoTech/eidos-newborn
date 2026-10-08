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
package com.solano.eidoscore.crypto;

import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.entity.vault.PdToken;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.service.mapper.GoldenRecordMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Обезличивание и обратное раскрытие: значения уходят в хранилище, в карточке
 * остаются токены, версия и индекс не затрагиваются.
 */
@ExtendWith(MockitoExtension.class)
class DepersonalizationServiceTest {

    private static final String CLIENT = "GR_7f4ea435-3e76-4a80-8c39-d35134090b0f";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 12, 0);

    @Mock GoldenRecordRepository goldenRecordRepository;
    @Mock TokenizationService tokenizationService;
    @Mock EntityManager entityManager;

    private DepersonalizationService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new DepersonalizationService(
                goldenRecordRepository, new GoldenRecordMapper(), tokenizationService, entityManager, fixed);
        lenient().when(tokenizationService.openBatch(anyString())).thenReturn(UUID.randomUUID());
        lenient().when(tokenizationService.tokenize(any(), anyString(), anyString(), anyString()))
                .thenAnswer(i -> "tok_" + i.getArgument(2));
    }

    @Test
    void personalValuesAreReplacedByTokens() {
        GoldenRecord record = record();

        assertThat(service.depersonalize(record)).isTrue();

        ArgumentCaptor<String> pinfl = ArgumentCaptor.forClass(String.class);
        verify(goldenRecordRepository).updatePersonalData(
                eq(CLIENT), anyString(), anyString(), any(), pinfl.capture(),
                anyString(), any(), anyString(), any(), any(), any(), any(), any(), any(),
                eq(NOW));
        assertThat(pinfl.getValue()).isEqualTo("tok_grPinfl").isNotEqualTo("61157669271732");
    }

    @Test
    void everyPersonalFieldGoesThroughTheVault() {
        service.depersonalize(record());

        // Пустые поля пропускаются, остальные уходят в хранилище.
        verify(tokenizationService).tokenize(any(), eq(CLIENT), eq("grPinfl"), eq("61157669271732"));
        verify(tokenizationService).tokenize(any(), eq(CLIENT), eq("grLastName"), eq("Каримов"));
        verify(tokenizationService).tokenize(any(), eq(CLIENT), eq("grMobilePhoneMain"), eq("998901234567"));
        verify(tokenizationService, never()).tokenize(any(), anyString(), eq("grBirthPlace"), any());
    }

    @Test
    void alreadyDepersonalisedRecordIsLeftAlone() {
        GoldenRecord record = record();
        record.setPdTokenizedAt(NOW);

        assertThat(service.depersonalize(record)).isFalse();
        verify(goldenRecordRepository, never()).updatePersonalData(
                anyString(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void oneKeyIsOpenedPerRecord() {
        service.depersonalize(record());
        verify(tokenizationService).openBatch(CLIENT);
    }

    @Test
    void restoreBringsValuesBackAndClearsTheMark() {
        GoldenRecord record = record();
        record.setPdTokenizedAt(NOW);
        record.setGrPinfl("tok_pinfl");
        record.setGrLastName("tok_last");
        lenient().when(tokenizationService.tokensOf(CLIENT)).thenReturn(List.of(
                token("grPinfl", "tok_pinfl"), token("grLastName", "tok_last")));
        lenient().when(tokenizationService.detokenize("tok_pinfl")).thenReturn("61157669271732");
        lenient().when(tokenizationService.detokenize("tok_last")).thenReturn("Каримов");

        assertThat(service.restore(record)).isTrue();

        ArgumentCaptor<String> pinfl = ArgumentCaptor.forClass(String.class);
        verify(goldenRecordRepository).updatePersonalData(
                eq(CLIENT), anyString(), anyString(), any(), pinfl.capture(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                eq(null));
        assertThat(pinfl.getValue()).isEqualTo("61157669271732");
    }

    @Test
    void restoringAnOpenRecordDoesNothing() {
        assertThat(service.restore(record())).isFalse();
        verify(tokenizationService, never()).tokensOf(anyString());
    }

    private static GoldenRecord record() {
        GoldenRecord r = new GoldenRecord();
        r.setGrClientId(CLIENT);
        r.setGrLastName("Каримов");
        r.setGrFirstName("Азиз");
        r.setGrPinfl("61157669271732");
        r.setGrDocPassData("AA1234567");
        r.setGrMobilePhoneMain("998901234567");
        r.setGrBirthDate(LocalDate.of(1990, 5, 17));
        return r;
    }

    private static PdToken token(String field, String value) {
        return PdToken.builder().token(value).fieldName(field).ownerId(CLIENT).build();
    }
}
