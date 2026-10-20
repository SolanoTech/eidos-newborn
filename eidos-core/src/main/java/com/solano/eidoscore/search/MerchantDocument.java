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

import com.solano.eidoscore.entity.legal.LegalEntityRecord;

import java.time.LocalDateTime;

/**
 * Документ мерчанта в поисковом индексе: полное и сокращённое название.
 *
 * <p>ИНН не индексируется — он ищется точно в базе. Нормализацию названия
 * делает анализатор индекса, а не этот код: транслитерация и отсечение
 * организационно-правовых форм описаны в соответствии полей.</p>
 */
public record MerchantDocument(
        String grLegalEntityId,
        String fullName,
        String shortName,
        LocalDateTime updatedAt
) {

    public static MerchantDocument of(LegalEntityRecord record) {
        return new MerchantDocument(
                record.getGrLegalEntityId(),
                record.getGrFullName(),
                record.getGrShortName(),
                record.getUpdatedAt());
    }
}
