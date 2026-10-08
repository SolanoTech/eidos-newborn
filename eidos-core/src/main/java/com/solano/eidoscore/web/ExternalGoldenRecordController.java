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
import com.solano.eidoscore.entity.GoldenRecordExternalId;
import com.solano.eidoscore.service.ExternalIdService;
import com.solano.eidoscore.service.GoldenRecordSearchService;
import com.solano.eidoscore.web.dto.CreateExternalIdRequest;
import com.solano.eidoscore.web.dto.UpdateExternalIdWithOldRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * External Golden Record API — для партнёрских систем (поиск + управление
 * внешними идентификаторами).
 */
@RestController
@RequestMapping("/api/v1/external/golden-records")
public class ExternalGoldenRecordController {

    private final GoldenRecordSearchService searchService;
    private final ExternalIdService externalIdService;

    public ExternalGoldenRecordController(
            GoldenRecordSearchService searchService,
            ExternalIdService externalIdService
    ) {
        this.searchService = searchService;
        this.externalIdService = externalIdService;
    }

    @GetMapping("/search/accurate")
    public GoldenRecord accurateSearch(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
            @RequestParam(required = false) String pinfl,
            @RequestParam(required = false) String passport
    ) {
        return searchService.accurateSearch(firstName, lastName, birthDate, pinfl, passport);
    }

    @GetMapping("/search/source-id")
    public GoldenRecord searchBySourceId(
            @RequestParam String source,
            @RequestParam("source_id") String sourceId
    ) {
        return searchService.findBySourceAndExternalId(source, sourceId);
    }

    @PutMapping("/external-id")
    public GoldenRecordExternalId updateExternalIdByOldValue(
            @Valid @RequestBody UpdateExternalIdWithOldRequest request
    ) {
        return externalIdService.updateByOldExternalId(
                request.getSource(),
                request.getOldExternalId(),
                request.getNewExternalId()
        );
    }

    @PostMapping("/{uuid}/external-id")
    @ResponseStatus(HttpStatus.CREATED)
    public GoldenRecordExternalId createExternalIdForGoldenRecord(
            @PathVariable String uuid,
            @Valid @RequestBody CreateExternalIdRequest request
    ) {
        return externalIdService.create(uuid, request.getSource(), request.getExternalId());
    }

    @PutMapping("/{uuid}/external-id")
    public GoldenRecordExternalId updateExternalIdForGoldenRecord(
            @PathVariable String uuid,
            @Valid @RequestBody CreateExternalIdRequest request
    ) {
        return externalIdService.updateByGoldenRecordUuid(uuid, request.getSource(), request.getExternalId());
    }
}
