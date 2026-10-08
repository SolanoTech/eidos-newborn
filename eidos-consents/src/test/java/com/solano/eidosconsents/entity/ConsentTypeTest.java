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

package com.solano.eidosconsents.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsentTypeTest {

    @Test
    void jsonCode_isShortForm() {
        // @JsonValue делегирует в getCode()
        assertThat(ConsentType.PERSONAL_DATA.getCode()).isEqualTo("pd");
        assertThat(ConsentType.BIO.getCode()).isEqualTo("bio");
    }

    @Test
    void fromCode_resolvesShortForm() {
        // @JsonCreator делегирует в fromCode()
        assertThat(ConsentType.fromCode("pd")).isEqualTo(ConsentType.PERSONAL_DATA);
        assertThat(ConsentType.fromCode("bio")).isEqualTo(ConsentType.BIO);
    }

    @Test
    void fromCode_unknownCode_throws() {
        assertThatThrownBy(() -> ConsentType.fromCode("xx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("xx");
    }

    @Test
    void dbRepresentationUsesConstantName() {
        // @Enumerated(EnumType.STRING) на сущности хранит name(), поэтому в БД
        // лежат имена констант: PERSONAL_DATA / BIO.
        assertThat(ConsentType.PERSONAL_DATA.name()).isEqualTo("PERSONAL_DATA");
        assertThat(ConsentType.BIO.name()).isEqualTo("BIO");
    }
}
