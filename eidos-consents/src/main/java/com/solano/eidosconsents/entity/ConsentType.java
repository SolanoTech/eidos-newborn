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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Тип согласия.
 *
 * <p>У типа два представления:</p>
 * <ul>
 *   <li><b>JSON</b> — короткий код {@code pd} / {@code bio}. Обеспечивается
 *       {@link JsonValue}/{@link JsonCreator}.</li>
 *   <li><b>БД</b> — имя константы {@code PERSONAL_DATA} / {@code BIO}.
 *       Обеспечивается {@code @Enumerated(EnumType.STRING)} на сущности.</li>
 * </ul>
 */
public enum ConsentType {

    PERSONAL_DATA("pd"),
    BIO("bio"),
    /** Маркетинговые коммуникации по каналам (расширение для CDP-консоли). */
    MARKETING_SMS("mk_sms"),
    MARKETING_PUSH("mk_push"),
    MARKETING_EMAIL("mk_email"),
    /** Профилирование и NBO. */
    PROFILING("profiling"),
    /** Передача данных третьим лицам. */
    THIRD_PARTY("third_party");

    private final String code;

    ConsentType(String code) {
        this.code = code;
    }

    /** Код для JSON-представления ({@code pd} / {@code bio}). */
    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ConsentType fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown consent type code: " + code));
    }
}
