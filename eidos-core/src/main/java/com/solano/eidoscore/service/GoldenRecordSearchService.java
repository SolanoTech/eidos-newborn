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
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.exception.NotFoundException;
import com.solano.eidoscore.exception.UnprocessableException;
import com.solano.eidoscore.repository.GoldenRecordExternalIdRepository;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.crypto.BlindIndex;
import com.solano.eidoscore.repository.SourceRepository;
import com.solano.eidoscore.search.FuzzySearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageImpl;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.LocalDate;

/**
 * Read-only queries для Golden Record. Используется внешним и внутренним
 * контроллерами.
 */
@Service
@Transactional(readOnly = true)
public class GoldenRecordSearchService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final GoldenRecordRepository goldenRecordRepository;
    private final GoldenRecordExternalIdRepository externalIdRepository;
    private final SourceRepository sourceRepository;
    private final BlindIndex blindIndex;
    private final FuzzySearchService fuzzySearch;

    public GoldenRecordSearchService(
            GoldenRecordRepository goldenRecordRepository,
            GoldenRecordExternalIdRepository externalIdRepository,
            SourceRepository sourceRepository,
            BlindIndex blindIndex,
            FuzzySearchService fuzzySearch
    ) {
        this.goldenRecordRepository = goldenRecordRepository;
        this.externalIdRepository = externalIdRepository;
        this.sourceRepository = sourceRepository;
        this.blindIndex = blindIndex;
        this.fuzzySearch = fuzzySearch;
    }

    public GoldenRecord accurateSearch(
            String firstName,
            String lastName,
            LocalDate birthDate,
            String pinfl,
            String passport
    ) {
        if ((pinfl == null || pinfl.isBlank()) && (passport == null || passport.isBlank())) {
            throw new UnprocessableException("Passport or PINFL required for accurate search");
        }
        // Сравнение по слепому индексу: та же четвёрка полей и то же точное
        // равенство, но не зависящее от того, открыты значения или нет.
        var match = pinfl != null && !pinfl.isBlank()
                ? goldenRecordRepository.findByPdBiIdentity(
                        blindIndex.identity(lastName, firstName, birthDate, pinfl))
                : goldenRecordRepository.findByPdBiIdentityDoc(
                        blindIndex.identityByDocument(lastName, firstName, birthDate, passport));
        return match.orElseThrow(() -> new NotFoundException("Golden Record Not Found"));
    }

    public GoldenRecord findBySourceAndExternalId(String sourceName, String externalId) {
        Source source = sourceRepository.findBySourceName(sourceName)
                .orElseThrow(() -> new NotFoundException("Source Not Found"));
        // ext.getGoldenRecord() — ленивый прокси; при сериализации ответа вне
        // сессии это LazyInitializationException. Перезагружаем сущность по id.
        return externalIdRepository.findBySourceIdAndExternalId(source.getId(), externalId)
                .map(ext -> goldenRecordRepository.findById(ext.getGoldenRecord().getGrClientId())
                        .orElseThrow(() -> new NotFoundException("Golden Record Not Found")))
                .orElseThrow(() -> new NotFoundException("Golden Record Not Found"));
    }

    /**
     * Нечёткий поиск клиента по имени.
     *
     * <p>Считает OpenSearch: кириллица и латиница сведены анализатором, опечатки
     * прощаются. Индекс отдаёт идентификаторы, карточки дочитываются из базы, и
     * найденный поиском порядок сохраняется — сортировка по дате создания здесь
     * была бы потерей релевантности.</p>
     *
     * <p>Обезличенные клиенты не находятся: их документов в индексе нет.</p>
     */
    public Page<GoldenRecord> search(String query, int page) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), DEFAULT_PAGE_SIZE);
        List<FuzzySearchService.Match> matches =
                fuzzySearch.searchClients(query, pageable.getPageSize() * (pageable.getPageNumber() + 1));
        return hydrate(matches, pageable);
    }

    /** Достраивает найденные идентификаторы до карточек, сохраняя порядок поиска. */
    private Page<GoldenRecord> hydrate(List<FuzzySearchService.Match> matches, Pageable pageable) {
        int from = (int) pageable.getOffset();
        if (from >= matches.size()) {
            return new PageImpl<>(List.of(), pageable, matches.size());
        }
        List<String> ids = matches.subList(from, Math.min(from + pageable.getPageSize(), matches.size()))
                .stream().map(FuzzySearchService.Match::id).toList();
        Map<String, GoldenRecord> byId = goldenRecordRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(GoldenRecord::getGrClientId, r -> r));
        return new PageImpl<>(
                ids.stream().map(byId::get).filter(Objects::nonNull).toList(),
                pageable, matches.size());
    }

    /** Нечёткий поиск для внешних потребителей: только идентификаторы и оценки. */
    public List<FuzzySearchService.Match> fuzzySearchClients(String query, int limit) {
        return fuzzySearch.searchClients(query, limit);
    }

    public GoldenRecord findById(String clientId) {
        return goldenRecordRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Golden Record Not Found"));
    }

    /**
     * Структурный поиск по реквизитам. Нормализация: имена — trim+lower,
     * паспорт — без пробелов в верхнем регистре, телефон — только цифры.
     * Все критерии пустые — вернуть первую страницу (как обзор).
     *
     * <p>ПИНФЛ сопоставляется по слепому индексу, то есть только целиком.
     * Остальные критерии остались подстрочными.</p>
     */
    public Page<GoldenRecord> structuredSearch(
            String pinfl, String passport, String lastName,
            String firstName, String middleName, String phone, int page
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page - 1, 0), DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.ASC, "createdAt"));
        // ПИНФЛ ищется по индексу и потому только целиком: null здесь означает
        // «критерий не задан», как пустая строка у остальных полей.
        String pinflDigits = digits(pinfl);
        return goldenRecordRepository.structuredSearch(
                pinflDigits.isEmpty() ? null : blindIndex.pinfl(pinflDigits),
                norm(passport).replaceAll("\\s", "").toUpperCase(),
                norm(lastName).toLowerCase(),
                norm(firstName).toLowerCase(),
                norm(middleName).toLowerCase(),
                digits(phone),
                pageable);
    }

    private static String norm(String v) {
        return v == null ? "" : v.trim();
    }

    private static String digits(String v) {
        return v == null ? "" : v.replaceAll("\\D", "");
    }
}
