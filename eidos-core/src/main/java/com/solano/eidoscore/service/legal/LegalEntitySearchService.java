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

import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.exception.BadRequestException;
import com.solano.eidoscore.exception.NotFoundException;
import com.solano.eidoscore.repository.legal.LegalEntityExternalIdRepository;
import com.solano.eidoscore.repository.legal.LegalEntityFieldMetaRepository;
import com.solano.eidoscore.repository.legal.LegalEntityRecordRepository;
import com.solano.eidoscore.search.FuzzySearchService;
import com.solano.eidoscore.web.dto.ExternalIdView;
import com.solano.eidoscore.web.dto.FieldMetaView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * Поиск записей юрлиц: точный по ИНН и неточный по названию мерчанта, плюс
 * чтение карточки, провенанса полей и внешних идентификаторов.
 *
 * <p>Неточный поиск обслуживает поисковый индекс: транслитерация и отсечение
 * организационно-правовых форм живут в анализаторе индекса, поэтому запрос
 * уходит туда как есть — «ООО Оазис» и «OAZIS» сходятся на стороне поиска.</p>
 */
@Service
public class LegalEntitySearchService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final LegalEntityRecordRepository recordRepository;
    private final LegalEntityFieldMetaRepository fieldMetaRepository;
    private final LegalEntityExternalIdRepository externalIdRepository;
    private final FuzzySearchService fuzzySearch;

    public LegalEntitySearchService(
            LegalEntityRecordRepository recordRepository,
            LegalEntityFieldMetaRepository fieldMetaRepository,
            LegalEntityExternalIdRepository externalIdRepository,
            FuzzySearchService fuzzySearch
    ) {
        this.recordRepository = recordRepository;
        this.fieldMetaRepository = fieldMetaRepository;
        this.externalIdRepository = externalIdRepository;
        this.fuzzySearch = fuzzySearch;
    }

    /** Точный поиск по ИНН — 404, если записи нет. */
    @Transactional(readOnly = true)
    public LegalEntityRecord byInn(String inn) {
        if (inn == null || inn.isBlank()) {
            throw new BadRequestException("INN is required");
        }
        return recordRepository.findByGrInn(inn.trim())
                .orElseThrow(() -> new NotFoundException("Legal entity with INN " + inn + " not found"));
    }

    /** Неточный поиск по названию мерчанта (поисковый индекс). */
    @Transactional(readOnly = true)
    public Page<LegalEntityRecord> searchByName(String query, int page) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), DEFAULT_PAGE_SIZE);
        List<FuzzySearchService.Match> matches =
                fuzzySearch.searchMerchants(query, pageable.getPageSize() * (pageable.getPageNumber() + 1));
        return hydrate(matches, pageable);
    }

    /**
     * Достраивает результаты поиска до записей.
     *
     * <p>Индекс отдаёт идентификаторы и оценки — сами карточки читаются из базы,
     * и порядок, найденный поиском, сохраняется.</p>
     */
    private Page<LegalEntityRecord> hydrate(List<FuzzySearchService.Match> matches, Pageable pageable) {
        int from = (int) pageable.getOffset();
        if (from >= matches.size()) {
            return new PageImpl<>(List.of(), pageable, matches.size());
        }
        List<String> ids = matches.subList(from, Math.min(from + pageable.getPageSize(), matches.size()))
                .stream().map(FuzzySearchService.Match::id).toList();
        Map<String, LegalEntityRecord> byId = recordRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(LegalEntityRecord::getGrLegalEntityId, r -> r));
        List<LegalEntityRecord> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, matches.size());
    }

    @Transactional(readOnly = true)
    public LegalEntityRecord findById(String grLegalEntityId) {
        return recordRepository.findById(grLegalEntityId)
                .orElseThrow(() -> new NotFoundException("Legal entity " + grLegalEntityId + " not found"));
    }

    /** Провенанс полей записи: какой источник поставил поле и когда. */
    @Transactional(readOnly = true)
    public List<FieldMetaView> fieldMeta(String grLegalEntityId) {
        return fieldMetaRepository.findByIdGrLegalEntityId(grLegalEntityId).stream()
                .map(meta -> new FieldMetaView(
                        meta.getId().getFieldName(),
                        meta.getSource().getSourceName(),
                        meta.getSource().getTrustLevel(),
                        meta.getUpdatedAt() == null ? null : meta.getUpdatedAt().toString()))
                .sorted(Comparator.comparing(FieldMetaView::fieldName))
                .toList();
    }

    /** Идентификаторы юрлица во внешних системах. */
    @Transactional(readOnly = true)
    public List<ExternalIdView> externalIds(String grLegalEntityId) {
        return externalIdRepository.findByLegalEntityGrLegalEntityId(grLegalEntityId).stream()
                .map(ext -> new ExternalIdView(
                        ext.getSource().getSourceName(),
                        ext.getExternalId(),
                        Boolean.TRUE.equals(ext.getIsActive()),
                        ext.getCreatedAt() == null ? null : ext.getCreatedAt().toString()))
                .sorted(Comparator.comparing(ExternalIdView::sourceName))
                .toList();
    }
}
