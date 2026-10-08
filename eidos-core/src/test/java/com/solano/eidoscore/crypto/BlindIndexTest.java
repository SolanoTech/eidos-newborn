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

import com.solano.eidoscore.entity.vault.PdBlindIndexKey;
import com.solano.eidoscore.repository.vault.PdBlindIndexKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Слепой индекс обязан воспроизводить действующую семантику сравнения: те же
 * поля, то же точное равенство, без приведения регистра.
 */
@ExtendWith(MockitoExtension.class)
class BlindIndexTest {

    private static final LocalDate DOB = LocalDate.of(1990, 5, 17);

    @Mock PdBlindIndexKeyRepository keyRepository;

    private final Map<Integer, PdBlindIndexKey> keys = new HashMap<>();
    private BlindIndex blindIndex;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(LocalDate.of(2026, 9, 28)
                .atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        lenient().when(keyRepository.save(any(PdBlindIndexKey.class))).thenAnswer(i -> {
            PdBlindIndexKey k = i.getArgument(0);
            keys.put(k.getKeyVersion(), k);
            return k;
        });
        lenient().when(keyRepository.findFirstByOrderByKeyVersionDesc())
                .thenAnswer(i -> keys.values().stream().findFirst());
        blindIndex = new BlindIndex(keyRepository, new FakeKeyWrapper(), fixed);
    }

    @Test
    void sameValuesGiveSameIndex() {
        String a = blindIndex.identity("Каримов", "Азиз", DOB, "61157669271732");
        String b = blindIndex.identity("Каримов", "Азиз", DOB, "61157669271732");

        assertThat(a).isEqualTo(b).hasSize(64);
    }

    @Test
    void indexRevealsNothingAboutTheValues() {
        String index = blindIndex.identity("Каримов", "Азиз", DOB, "61157669271732");

        assertThat(index).doesNotContain("61157669271732").doesNotContain("Каримов");
    }

    @Test
    void anyDifferingFieldChangesTheIndex() {
        String base = blindIndex.identity("Каримов", "Азиз", DOB, "61157669271732");

        assertThat(blindIndex.identity("Юсупов", "Азиз", DOB, "61157669271732")).isNotEqualTo(base);
        assertThat(blindIndex.identity("Каримов", "Бек", DOB, "61157669271732")).isNotEqualTo(base);
        assertThat(blindIndex.identity("Каримов", "Азиз", DOB.plusDays(1), "61157669271732")).isNotEqualTo(base);
        assertThat(blindIndex.identity("Каримов", "Азиз", DOB, "61157669271733")).isNotEqualTo(base);
    }

    @Test
    void caseIsNotFolded() {
        // Действующий матчинг сравнивает строки точно; индекс обязан вести себя
        // так же, иначе он начал бы склеивать записи, которые сейчас разные.
        assertThat(blindIndex.identity("каримов", "Азиз", DOB, "61157669271732"))
                .isNotEqualTo(blindIndex.identity("Каримов", "Азиз", DOB, "61157669271732"));
    }

    @Test
    void fieldBoundariesCannotBeShifted() {
        // Без разделителя «Каримов»+«Азиз» и «Карим»+«овАзиз» дали бы одно и то же.
        assertThat(blindIndex.identity("Каримов", "Азиз", DOB, "611"))
                .isNotEqualTo(blindIndex.identity("Карим", "овАзиз", DOB, "611"));
    }

    @Test
    void variantsNeverCollideEvenOnEqualValues() {
        // Области разделены явно, поэтому совпадение значений в разных ролях
        // (паспорт одного и ПИНФЛ другого) не превращается в совпадение записей.
        String sameValue = "AB1234567";
        assertThat(blindIndex.identityByDocument("Каримов", "Азиз", DOB, sameValue))
                .isNotEqualTo(blindIndex.identity("Каримов", "Азиз", DOB, sameValue));
        assertThat(blindIndex.pinfl(sameValue))
                .isNotEqualTo(blindIndex.identity("", "", DOB, sameValue));
    }

    @Test
    void singlePinflIndexIsIndependent() {
        assertThat(blindIndex.pinfl("61157669271732"))
                .isEqualTo(blindIndex.pinfl("61157669271732"))
                .isNotEqualTo(blindIndex.pinfl("61157669271733"));
    }

    @Test
    void missingComponentYieldsNoIndex() {
        assertThat(blindIndex.identity("Каримов", "Азиз", DOB, null)).isNull();
        assertThat(blindIndex.identity(null, "Азиз", DOB, "61157669271732")).isNull();
        assertThat(blindIndex.pinfl(null)).isNull();
    }

    @Test
    void firstUseGeneratesKeyVersionOne() {
        assertThat(blindIndex.currentKeyVersion()).isEqualTo(1);
        assertThat(keys).containsKey(1);
    }
}
