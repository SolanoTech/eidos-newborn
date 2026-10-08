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

import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.service.GoldenRecordSaveService;
import com.solano.eidoscore.service.GoldenRecordSearchService;
import com.solano.eidoscore.web.dto.PostGoldenRecordRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal Golden Record API — для сервисов внутри CDP-кластера.
 */
@RestController
@RequestMapping("/api/v1/internal/golden-records")
public class InternalGoldenRecordController {

    private final GoldenRecordSaveService saveService;
    private final GoldenRecordSearchService searchService;
    private final com.solano.eidoscore.service.FieldMetaQueryService fieldMetaQueryService;
    private final com.solano.eidoscore.crypto.DisclosureService disclosureService;

    public InternalGoldenRecordController(
            GoldenRecordSaveService saveService,
            GoldenRecordSearchService searchService,
            com.solano.eidoscore.service.FieldMetaQueryService fieldMetaQueryService,
            com.solano.eidoscore.crypto.DisclosureService disclosureService
    ) {
        this.saveService = saveService;
        this.searchService = searchService;
        this.fieldMetaQueryService = fieldMetaQueryService;
        this.disclosureService = disclosureService;
    }

    @GetMapping("/search")
    public Page<GoldenRecord> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page
    ) {
        return searchService.search(query, page);
    }

    @GetMapping("/search/structured")
    public Page<GoldenRecord> structuredSearch(
            @RequestParam(required = false) String pinfl,
            @RequestParam(required = false) String passport,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String middleName,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "1") int page
    ) {
        return searchService.structuredSearch(pinfl, passport, lastName, firstName, middleName, phone, page);
    }

    @GetMapping("/{clientId}")
    public GoldenRecord getById(@PathVariable String clientId) {
        return searchService.findById(clientId);
    }

    @GetMapping("/{clientId}/field-meta")
    public java.util.List<com.solano.eidoscore.web.dto.FieldMetaView> fieldMeta(@PathVariable String clientId) {
        return fieldMetaQueryService.byClientId(clientId);
    }

    @GetMapping("/{clientId}/external-ids")
    public java.util.List<com.solano.eidoscore.web.dto.ExternalIdView> externalIds(@PathVariable String clientId) {
        return fieldMetaQueryService.externalIds(clientId);
    }

    @PostMapping
    public Map<String, String> postGoldenRecord(@Valid @RequestBody PostGoldenRecordRequest request) {
        saveService.execute(
                request.getRecord(),
                request.getSource(),
                request.getClientSourceIdentificator()
        );
        return Map.of("status", "SUCCESS");
    }
    /**
     * Раскрытие обезличенных значений записи.
     *
     * <p>Разовый просмотр: форма хранения не меняется, запись остаётся с
     * токенами. Основание обязательно и попадает в журнал раскрытий вместе с
     * тем, кто запросил.</p>
     */
    @PostMapping("/{clientId}/reveal")
    public java.util.Map<String, String> reveal(
            @PathVariable String clientId,
            @RequestBody RevealRequest request
    ) {
        return disclosureService.reveal(clientId, request.fields(), request.actor(), request.reason());
    }

    /**
     * @param fields какие поля раскрыть; пусто — все персональные
     * @param actor  кто запрашивает
     * @param reason основание, не короче десяти символов
     */
    public record RevealRequest(java.util.List<String> fields, String actor, String reason) {
    }

}
