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

package com.solano.eidoscore.web;

import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.service.legal.LegalEntitySaveService;
import com.solano.eidoscore.service.legal.LegalEntitySearchService;
import com.solano.eidoscore.web.dto.ExternalIdView;
import com.solano.eidoscore.web.dto.FieldMetaView;
import com.solano.eidoscore.web.dto.PostLegalEntityRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Внутренний API Золотых записей юрлиц — зеркало
 * {@link InternalGoldenRecordController} для второй вертикали: приём от stage,
 * два режима поиска (точный по ИНН, неточный по названию), карточка,
 * провенанс полей и внешние идентификаторы.
 */
@RestController
@RequestMapping("/api/v1/internal/legal-records")
public class InternalLegalEntityController {

    private final LegalEntitySaveService saveService;
    private final LegalEntitySearchService searchService;

    public InternalLegalEntityController(LegalEntitySaveService saveService,
                                         LegalEntitySearchService searchService) {
        this.saveService = saveService;
        this.searchService = searchService;
    }

    /** Точный поиск по ИНН. */
    @GetMapping("/search/inn")
    public LegalEntityRecord searchByInn(@RequestParam String inn) {
        return searchService.byInn(inn);
    }

    /** Неточный поиск по названию мерчанта (триграммное сходство). */
    @GetMapping("/search/name")
    public Page<LegalEntityRecord> searchByName(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Double threshold
    ) {
        return searchService.searchByName(query, page, threshold);
    }

    @GetMapping("/{grLegalEntityId}")
    public LegalEntityRecord get(@PathVariable String grLegalEntityId) {
        return searchService.findById(grLegalEntityId);
    }

    @GetMapping("/{grLegalEntityId}/field-meta")
    public List<FieldMetaView> fieldMeta(@PathVariable String grLegalEntityId) {
        return searchService.fieldMeta(grLegalEntityId);
    }

    @GetMapping("/{grLegalEntityId}/external-ids")
    public List<ExternalIdView> externalIds(@PathVariable String grLegalEntityId) {
        return searchService.externalIds(grLegalEntityId);
    }

    @PostMapping
    public Map<String, String> postLegalEntity(@Valid @RequestBody PostLegalEntityRequest request) {
        saveService.execute(
                request.getRecord(),
                request.getSource(),
                request.getClientSourceIdentificator()
        );
        return Map.of("status", "SUCCESS");
    }
}
