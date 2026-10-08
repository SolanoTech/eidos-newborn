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

package com.solano.eidoscdiuibackend.web;

import com.solano.eidoscdiuibackend.proxy.ConsentsClient;
import com.solano.eidoscdiuibackend.service.AuditService;
import com.solano.eidoscdiuibackend.web.dto.ClientConsentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Согласия клиента из CDP-консоли (Customer 360). Проксирует eidos-consents;
 * выдача и отзыв пишутся в аудит.
 */
@RestController
@RequestMapping("/api/v1/admin/consents")
@PreAuthorize("hasRole('ADMIN')")
public class ClientConsentController {

    public record GrantRequest(@NotNull UUID clientUuid, @NotBlank String type) {
    }

    private final ConsentsClient consentsClient;
    private final AuditService auditService;

    public ClientConsentController(ConsentsClient consentsClient, AuditService auditService) {
        this.consentsClient = consentsClient;
        this.auditService = auditService;
    }

    @GetMapping("/{clientUuid}")
    public List<ClientConsentStatus> summary(@PathVariable UUID clientUuid) {
        return consentsClient.summary(clientUuid);
    }

    @PostMapping("/grant")
    public Map<String, String> grant(@Valid @RequestBody GrantRequest request) {
        consentsClient.grant(request.clientUuid(), request.type());
        auditService.log("Выдано согласие " + request.type(),
                "Клиент " + request.clientUuid(), "ok");
        return Map.of("status", "GRANTED");
    }

    @PostMapping("/{consentId}/revoke")
    public Map<String, String> revoke(@PathVariable UUID consentId) {
        consentsClient.revoke(consentId);
        auditService.log("Отозвано согласие", "Consent " + consentId, "bad");
        return Map.of("status", "REVOKED");
    }
}
