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

package com.solano.eidoscore.search;

import com.solano.eidoscore.entity.GoldenRecord;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Документ клиента в поисковом индексе.
 *
 * <p>Только то, по чему ищут нечётко. Телефон, паспорт и ПИНФЛ сюда не
 * попадают: их ищут точно, и за это отвечает слепой индекс в базе. Чем меньше
 * персональных данных во второй копии, тем лучше.</p>
 *
 * @param fullName склейка ФИО — чтобы запрос «Каримов Азиз» находился одним
 *                 полем, а не требовал разбора на части
 */
public record ClientDocument(
        String grClientId,
        String lastName,
        String firstName,
        String middleName,
        String fullName,
        LocalDateTime updatedAt
) {

    public static ClientDocument of(GoldenRecord record) {
        return new ClientDocument(
                record.getGrClientId(),
                record.getGrLastName(),
                record.getGrFirstName(),
                record.getGrMiddleName(),
                join(record.getGrLastName(), record.getGrFirstName(), record.getGrMiddleName()),
                record.getUpdatedAt());
    }

    private static String join(String... parts) {
        return Stream.of(parts)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(" "));
    }
}
