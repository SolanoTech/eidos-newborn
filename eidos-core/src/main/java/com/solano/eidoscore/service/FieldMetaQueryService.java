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

import com.solano.eidoscore.repository.GoldenRecordExternalIdRepository;
import com.solano.eidoscore.repository.GoldenRecordFieldMetaRepository;
import com.solano.eidoscore.web.dto.ExternalIdView;
import com.solano.eidoscore.web.dto.FieldMetaView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Чтение провенанса полей Golden Record ({@code golden_record_field_meta})
 * для карточки клиента: какой источник поставил поле и когда обновил.
 */
@Service
public class FieldMetaQueryService {

    private final GoldenRecordFieldMetaRepository fieldMetaRepository;
    private final GoldenRecordExternalIdRepository externalIdRepository;

    public FieldMetaQueryService(GoldenRecordFieldMetaRepository fieldMetaRepository,
                                 GoldenRecordExternalIdRepository externalIdRepository) {
        this.fieldMetaRepository = fieldMetaRepository;
        this.externalIdRepository = externalIdRepository;
    }

    @Transactional(readOnly = true)
    public List<FieldMetaView> byClientId(String grClientId) {
        return fieldMetaRepository.findByIdGrClientId(grClientId).stream()
                .map(meta -> new FieldMetaView(
                        meta.getId().getFieldName(),
                        meta.getSource().getSourceName(),
                        meta.getSource().getTrustLevel(),
                        meta.getUpdatedAt() == null ? null : meta.getUpdatedAt().toString()))
                .sorted(Comparator.comparing(FieldMetaView::fieldName))
                .toList();
    }

    /** Связанные идентификаторы клиента во внешних системах (граф личности). */
    @Transactional(readOnly = true)
    public List<ExternalIdView> externalIds(String grClientId) {
        return externalIdRepository.findByGoldenRecordGrClientId(grClientId).stream()
                .map(ext -> new ExternalIdView(
                        ext.getSource().getSourceName(),
                        ext.getExternalId(),
                        Boolean.TRUE.equals(ext.getIsActive()),
                        ext.getCreatedAt() == null ? null : ext.getCreatedAt().toString()))
                .sorted(Comparator.comparing(ExternalIdView::sourceName))
                .toList();
    }
}
