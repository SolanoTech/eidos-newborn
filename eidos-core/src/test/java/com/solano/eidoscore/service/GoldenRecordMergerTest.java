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
import com.solano.eidoscore.entity.GoldenRecordFieldMeta;
import com.solano.eidoscore.entity.GoldenRecordFieldMetaId;
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.repository.GoldenRecordFieldMetaRepository;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.service.mapper.GoldenRecordMapper;
import com.solano.shared.dto.GoldenRecordDto;
import com.solano.shared.enums.Gender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoldenRecordMergerTest {

    @Mock GoldenRecordRepository goldenRecordRepository;
    @Mock GoldenRecordFieldMetaRepository fieldMetaRepository;
    @Mock GoldenRecordSnapshotter snapshotter;
    @Spy GoldenRecordMapper mapper = new GoldenRecordMapper();

    @InjectMocks GoldenRecordMerger merger;

    @Test
    void higherTrustSource_overwritesField_andArchivesSnapshot() {
        Source oldSource = source(1, "OLD", 10);
        Source newSource = source(2, "NEW", 100);

        GoldenRecord gr = baseRecord();
        gr.setGrFirstName("OldName");

        GoldenRecordFieldMeta meta = meta(gr.getGrClientId(), "grFirstName", oldSource);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(meta));
        when(goldenRecordRepository.saveAndFlush(gr)).thenReturn(gr);

        GoldenRecordDto incoming = baseDto();
        incoming.setGrFirstName("NewName");

        merger.execute(gr, incoming, newSource);

        assertThat(gr.getGrFirstName()).isEqualTo("NewName");
        assertThat(meta.getSource()).isEqualTo(newSource);
        verify(goldenRecordRepository).saveAndFlush(gr);
        verify(fieldMetaRepository).saveAll(List.of(meta));
        verify(snapshotter).archive(gr);
        verify(snapshotter, never()).tentativeFromIncoming(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }

    @Test
    void higherTrust_sameValue_takesProvenance_withoutNewVersion() {
        // Более надёжный источник подтвердил то же значение: поле закрепляется
        // за ним, но данные не менялись — версия и архив не трогаются.
        Source oldSource = source(1, "OLD", 10);
        Source newSource = source(2, "NEW", 100);

        GoldenRecord gr = baseRecord();
        gr.setGrFirstName("John");   // то же значение, что в baseDto()

        GoldenRecordFieldMeta meta = meta(gr.getGrClientId(), "grFirstName", oldSource);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(meta));

        merger.execute(gr, baseDto(), newSource);

        assertThat(meta.getSource()).isEqualTo(newSource);
        verify(fieldMetaRepository).saveAll(List.of(meta));
        verify(goldenRecordRepository, never()).saveAndFlush(any());
        verify(snapshotter, never()).archive(any());
    }

    @Test
    void confirmedByHigherTrust_previousOwnerCannotChangeIt() {
        // После подтверждения поле за надёжным источником: прежний владелец
        // с меньшим доверием его уже не меняет.
        Source crm = source(1, "CRM", 10);
        Source bank = source(2, "BANK", 100);

        GoldenRecord gr = baseRecord();
        gr.setGrFirstName("John");

        GoldenRecordFieldMeta meta = meta(gr.getGrClientId(), "grFirstName", crm);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(meta));

        merger.execute(gr, baseDto(), bank);

        GoldenRecordDto fromCrm = baseDto();
        fromCrm.setGrFirstName("Johnny");
        merger.execute(gr, fromCrm, crm);

        assertThat(gr.getGrFirstName()).isEqualTo("John");
        assertThat(meta.getSource()).isEqualTo(bank);
        verify(goldenRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void higherTrust_missingIncomingField_doesNotOverwrite() {
        // Поле, которое источник не прислал (null в DTO), не затирается и не
        // меняет провенанс, даже при более высоком trust.
        Source oldSource = source(1, "OLD", 10);
        Source newSource = source(2, "NEW", 100);

        GoldenRecord gr = baseRecord();
        gr.setGrMiddleName("OldMiddle");   // baseDto() не задаёт grMiddleName → null

        GoldenRecordFieldMeta meta = meta(gr.getGrClientId(), "grMiddleName", oldSource);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(meta));

        merger.execute(gr, baseDto(), newSource);

        assertThat(gr.getGrMiddleName()).isEqualTo("OldMiddle");
        assertThat(meta.getSource()).isEqualTo(oldSource);
        verify(goldenRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void equalTrust_differingValues_writesTentative_andLeavesGrUntouched() {
        Source oldSource = source(1, "OLD", 50);
        Source newSource = source(2, "NEW", 50);

        GoldenRecord gr = baseRecord();
        gr.setGrFirstName("OldName");

        GoldenRecordFieldMeta meta = meta(gr.getGrClientId(), "grFirstName", oldSource);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(meta));

        GoldenRecordDto incoming = baseDto();
        incoming.setGrFirstName("NewName");

        merger.execute(gr, incoming, newSource);

        assertThat(gr.getGrFirstName()).isEqualTo("OldName");
        verify(goldenRecordRepository, never()).saveAndFlush(any());
        verify(fieldMetaRepository, never()).saveAll(any());
        verify(snapshotter).tentativeFromIncoming(incoming, gr.getGrClientId(), "NEW");
        verify(snapshotter, never()).archive(any());
    }

    @Test
    void equalTrust_multipleConflictingFields_writesTentativeOnce() {
        Source oldSource = source(1, "OLD", 50);
        Source newSource = source(2, "NEW", 50);

        GoldenRecord gr = baseRecord();
        gr.setGrFirstName("OldName");
        gr.setGrLastName("OldLast");

        GoldenRecordFieldMeta metaFirst = meta(gr.getGrClientId(), "grFirstName", oldSource);
        GoldenRecordFieldMeta metaLast = meta(gr.getGrClientId(), "grLastName", oldSource);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId()))
                .thenReturn(List.of(metaFirst, metaLast));

        GoldenRecordDto incoming = baseDto();
        incoming.setGrFirstName("NewName");
        incoming.setGrLastName("NewLast");

        merger.execute(gr, incoming, newSource);

        verify(snapshotter, org.mockito.Mockito.times(1))
                .tentativeFromIncoming(incoming, gr.getGrClientId(), "NEW");
    }

    @Test
    void equalTrust_sameValue_isNoop() {
        Source oldSource = source(1, "OLD", 50);
        Source newSource = source(2, "NEW", 50);

        GoldenRecord gr = baseRecord();
        gr.setGrFirstName("SameName");

        GoldenRecordFieldMeta meta = meta(gr.getGrClientId(), "grFirstName", oldSource);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(meta));

        GoldenRecordDto incoming = baseDto();
        incoming.setGrFirstName("SameName");

        merger.execute(gr, incoming, newSource);

        verify(goldenRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void sameSource_updatesOwnField_andArchivesSnapshot() {
        // Поле принадлежит источнику — более надёжного источника для него нет,
        // и источник обновляет своё же значение.
        Source s = source(1, "ONLY", 50);

        GoldenRecord gr = baseRecord();
        gr.setGrFirstName("OldName");

        GoldenRecordFieldMeta meta = meta(gr.getGrClientId(), "grFirstName", s);
        LocalDateTime before = LocalDateTime.of(2020, 1, 1, 0, 0);
        meta.setUpdatedAt(before);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(meta));
        when(goldenRecordRepository.saveAndFlush(gr)).thenReturn(gr);

        GoldenRecordDto incoming = baseDto();
        incoming.setGrFirstName("NewName");

        merger.execute(gr, incoming, s);

        assertThat(gr.getGrFirstName()).isEqualTo("NewName");
        assertThat(meta.getSource()).isEqualTo(s);
        assertThat(meta.getUpdatedAt()).isNotEqualTo(before);
        verify(goldenRecordRepository).saveAndFlush(gr);
        verify(snapshotter).archive(gr);
        verify(snapshotter, never()).tentativeFromIncoming(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }

    @Test
    void sameSource_missingIncomingField_doesNotOverwrite() {
        // Непришедшее поле не затирает значение и у его владельца.
        Source s = source(1, "ONLY", 50);

        GoldenRecord gr = baseRecord();
        gr.setGrMiddleName("OldMiddle");   // baseDto() не задаёт grMiddleName → null

        GoldenRecordFieldMeta meta = meta(gr.getGrClientId(), "grMiddleName", s);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(meta));

        merger.execute(gr, baseDto(), s);

        assertThat(gr.getGrMiddleName()).isEqualTo("OldMiddle");
        verify(goldenRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void lowerTrustSource_doesNotOverwriteHigherTrustOwner() {
        // Поле за более надёжным источником: менее надёжный его не меняет.
        Source owner = source(1, "BANK", 80);
        Source incomingSource = source(2, "CRM", 50);

        GoldenRecord gr = baseRecord();
        gr.setGrFirstName("BankName");

        GoldenRecordFieldMeta meta = meta(gr.getGrClientId(), "grFirstName", owner);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(meta));

        GoldenRecordDto incoming = baseDto();
        incoming.setGrFirstName("CrmName");

        merger.execute(gr, incoming, incomingSource);

        assertThat(gr.getGrFirstName()).isEqualTo("BankName");
        assertThat(meta.getSource()).isEqualTo(owner);
        verify(goldenRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void unknownFieldName_inMeta_isSkipped() {
        Source oldSource = source(1, "OLD", 10);
        Source newSource = source(2, "NEW", 100);

        GoldenRecord gr = baseRecord();

        GoldenRecordFieldMeta junk = meta(gr.getGrClientId(), "doesNotExist", oldSource);
        when(fieldMetaRepository.findByIdGrClientId(gr.getGrClientId())).thenReturn(List.of(junk));

        merger.execute(gr, baseDto(), newSource);

        verify(goldenRecordRepository, never()).saveAndFlush(any());
    }

    // ---- helpers ----

    private static Source source(int id, String name, int trust) {
        Source s = new Source();
        s.setId(id);
        s.setSourceName(name);
        s.setTrustLevel(trust);
        return s;
    }

    private static GoldenRecordFieldMeta meta(String grClientId, String fieldName, Source source) {
        GoldenRecordFieldMeta m = new GoldenRecordFieldMeta();
        m.setId(new GoldenRecordFieldMetaId(grClientId, fieldName));
        m.setSource(source);
        m.setUpdatedAt(LocalDateTime.now());
        return m;
    }

    private static GoldenRecord baseRecord() {
        GoldenRecord gr = new GoldenRecord();
        gr.setGrClientId("GR_test");
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
