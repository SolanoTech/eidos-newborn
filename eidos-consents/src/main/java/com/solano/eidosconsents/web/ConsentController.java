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

package com.solano.eidosconsents.web;

import com.solano.eidosconsents.entity.Consent;
import com.solano.eidosconsents.entity.ConsentType;
import com.solano.eidosconsents.service.ConsentSummaryService;
import com.solano.eidosconsents.service.GetActiveConsentService;
import com.solano.eidosconsents.service.RevokeConsentService;
import com.solano.eidosconsents.service.SaveConsentService;
import com.solano.eidosconsents.web.dto.ConsentView;
import com.solano.eidosconsents.web.dto.DataDetailResponse;
import com.solano.eidosconsents.web.dto.PostConsentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API согласий под префиксом {@code /internal/api/v1/consents}.
 */
@RestController
@RequestMapping("/internal/api/v1/consents")
public class ConsentController {

    private final SaveConsentService saveConsentService;
    private final GetActiveConsentService getActiveConsentService;
    private final ConsentSummaryService consentSummaryService;
    private final RevokeConsentService revokeConsentService;

    public ConsentController(
            SaveConsentService saveConsentService,
            GetActiveConsentService getActiveConsentService,
            ConsentSummaryService consentSummaryService,
            RevokeConsentService revokeConsentService
    ) {
        this.saveConsentService = saveConsentService;
        this.getActiveConsentService = getActiveConsentService;
        this.consentSummaryService = consentSummaryService;
        this.revokeConsentService = revokeConsentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataDetailResponse<ConsentView> createConsent(@Valid @RequestBody PostConsentRequest request) {
        Consent consent = saveConsentService.execute(
                request.getType(),
                request.getClientUuid(),
                request.getInitialDate(),
                request.getEndDate(),
                request.getSourceName()
        );
        return DataDetailResponse.of(ConsentView.fromEntity(consent), "Consent created");
    }

    /**
     * Последнее активное согласие клиента заданного типа.
     *
     * <p>Тип передаётся в query-параметре по <b>имени</b> константы
     * ({@code PERSONAL_DATA} / {@code BIO}) — именно так Spring биндит enum в
     * {@code @RequestParam} (короткие коды {@code pd}/{@code bio} действуют
     * только в JSON-теле). Если активного согласия нет — 404.</p>
     */
    @GetMapping("/active/{client_uuid}")
    public ConsentView getActiveConsent(
            @PathVariable("client_uuid") UUID clientUuid,
            @RequestParam("type") ConsentType type
    ) {
        Consent consent = getActiveConsentService.execute(clientUuid, type);
        return ConsentView.fromEntity(consent);
    }

    /** Сводка согласий клиента: по каждому типу — последнее согласие и активность. */
    @GetMapping("/client/{client_uuid}")
    public java.util.List<ConsentSummaryService.ConsentStatus> clientSummary(
            @PathVariable("client_uuid") UUID clientUuid
    ) {
        return consentSummaryService.summary(clientUuid);
    }

    /** Отзыв согласия: срок обрезается вчерашним днём, история сохраняется. */
    @PostMapping("/{id}/revoke")
    public ConsentView revoke(@PathVariable("id") UUID id) {
        return ConsentView.fromEntity(revokeConsentService.execute(id));
    }
}
