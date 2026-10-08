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

package com.solano.eidosgateway.web;

import com.solano.eidosgateway.entity.registry.EntityType;
import com.solano.eidosgateway.proxy.ProxyService;
import com.solano.eidosgateway.proxy.RouteResolver;
import com.solano.eidosgateway.repository.registry.SourceContractRepository;
import com.solano.eidosgateway.service.contract.ContractValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * Приём карточек юридических лиц (мерчантов) на внешнем интерфейсе —
 * <b>отдельный эндпоинт</b> рядом с {@code /api/v1/client-data}.
 *
 * <p>Разные эндпоинты вместо поля {@code entity_type} в теле: так источник не
 * может подменить целевую Золотую запись, а права и квоты на два потока
 * настраиваются независимо. Логика приёма общая — см.
 * {@link ClientDataController#ingestAs}.</p>
 */
@RestController
@RequestMapping("/api/v1/legal-data")
public class LegalDataController extends ClientDataController {

    public LegalDataController(
            ObjectMapper objectMapper,
            SourceContractRepository contractRepository,
            ContractValidator contractValidator,
            RouteResolver routeResolver,
            ProxyService proxyService,
            com.solano.eidosgateway.service.stats.RejectStatsService rejectStats
    ) {
        super(objectMapper, contractRepository, contractValidator, routeResolver, proxyService, rejectStats);
    }

    @PostMapping
    @Override
    public ResponseEntity<byte[]> ingest(HttpServletRequest request, @RequestBody byte[] body) {
        return ingestAs(EntityType.LEGAL_ENTITY, request, body);
    }
}
