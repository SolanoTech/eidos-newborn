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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Нормализация названий мерчантов: разные написания одного бренда должны
 * сходиться к одной строке, иначе неточный поиск не сработает.
 */
class MerchantNameNormalizerTest {

    @Test
    void cyrillicAndLatinSpellings_convergeToSameString() {
        assertThat(MerchantNameNormalizer.normalize("ООО \"Оазис Маркет\""))
                .isEqualTo(MerchantNameNormalizer.normalize("OOO Oazis Market"))
                .isEqualTo("oazis market");
    }

    @Test
    void orgFormIsStripped_regardlessOfPosition() {
        assertThat(MerchantNameNormalizer.normalize("OAZIS MARKET MCHJ")).isEqualTo("oazis market");
        assertThat(MerchantNameNormalizer.normalize("МЧЖ Оазис Маркет")).isEqualTo("oazis market");
        assertThat(MerchantNameNormalizer.normalize("Oazis Market LLC")).isEqualTo("oazis market");
    }

    @Test
    void punctuationAndCaseAreIgnored_uzbekApostropheKept() {
        assertThat(MerchantNameNormalizer.normalize("«Тошкент-Савдо», ЧП")).isEqualTo("toshkent savdo");
        assertThat(MerchantNameNormalizer.normalize("Ўзбегим")).isEqualTo("o'zbegim");
    }

    @Test
    void normalizeAll_mergesFullAndShortNameWithoutDuplicates() {
        String merged = MerchantNameNormalizer.normalizeAll(
                "ООО \"Оазис Маркет\"", "Oazis");
        assertThat(merged).isEqualTo("oazis market");
    }

    @Test
    void blankInput_returnsNull() {
        assertThat(MerchantNameNormalizer.normalize(null)).isNull();
        assertThat(MerchantNameNormalizer.normalize("   ")).isNull();
        assertThat(MerchantNameNormalizer.normalize("ООО")).isNull();
        assertThat(MerchantNameNormalizer.normalizeAll(null, "")).isNull();
    }
}
