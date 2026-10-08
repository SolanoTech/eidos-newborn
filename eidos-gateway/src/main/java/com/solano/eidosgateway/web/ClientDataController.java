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
import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.entity.registry.SourceContract;
import com.solano.eidosgateway.exception.ContractValidationException;
import com.solano.eidosgateway.proxy.ProxyService;
import com.solano.eidosgateway.proxy.RouteResolver;
import com.solano.eidosgateway.repository.registry.SourceContractRepository;
import com.solano.eidosgateway.security.SourceTokenAuthFilter;
import com.solano.eidosgateway.service.contract.ContractValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Приём карточек клиентов на внешнем интерфейсе. В отличие от прочих маршрутов
 * (прозрачный прокси), здесь Gateway <b>валидирует</b> входящие данные против
 * дата-контракта источника и только при успехе проксирует их в eidos-stage.
 *
 * <p>Источник уже авторизован {@code SourceTokenAuthFilter} (лежит в атрибуте
 * запроса). Валидируются данные из поля {@code data} тела {@code {data, source}}.</p>
 */
@RestController
@RequestMapping("/api/v1/client-data")
public class ClientDataController {

    protected final ObjectMapper objectMapper;
    protected final SourceContractRepository contractRepository;
    protected final ContractValidator contractValidator;
    protected final RouteResolver routeResolver;
    protected final ProxyService proxyService;
    protected final com.solano.eidosgateway.service.stats.RejectStatsService rejectStats;

    public ClientDataController(
            ObjectMapper objectMapper,
            SourceContractRepository contractRepository,
            ContractValidator contractValidator,
            RouteResolver routeResolver,
            ProxyService proxyService,
            com.solano.eidosgateway.service.stats.RejectStatsService rejectStats
    ) {
        this.objectMapper = objectMapper;
        this.contractRepository = contractRepository;
        this.contractValidator = contractValidator;
        this.routeResolver = routeResolver;
        this.proxyService = proxyService;
        this.rejectStats = rejectStats;
    }

    @PostMapping
    public ResponseEntity<byte[]> ingest(HttpServletRequest request, @RequestBody byte[] body) {
        return ingestAs(EntityType.PERSON, request, body);
    }

    /**
     * Общий приём для обоих каналов. Тип сущности задаётся эндпоинтом, а не
     * телом запроса: источник не присылает {@code entity_type} и не может им
     * управлять.
     */
    protected ResponseEntity<byte[]> ingestAs(EntityType entityType,
                                              HttpServletRequest request, byte[] body) {
        Source source = (Source) request.getAttribute(SourceTokenAuthFilter.SOURCE_ATTRIBUTE);
        SourceContract contract = contractRepository
                .findBySourceCodeAndEntityType(source.getCode(), entityType)
                .orElseThrow(() -> {
                    rejectStats.recordRejected(source.getCode());
                    return new ContractValidationException(
                            "No " + entityType + " data contract configured for source "
                                    + source.getCode(), List.of());
                });

        Map<String, Object> envelope = parseEnvelope(body);
        Map<String, Object> dataNode = extractData(envelope);
        List<String> violations = contractValidator.validate(dataNode, contract);
        if (!violations.isEmpty()) {
            rejectStats.recordRejected(source.getCode());
            throw new ContractValidationException("Client data failed contract validation", violations);
        }

        // Источник — авторитетно из токена, а не из тела клиента. Проставляем на
        // верхнем уровне конверта {data, source} и проксируем ИЗМЕНЁННОЕ тело в stage.
        envelope.put("source", source.getCode());
        byte[] forwarded = objectMapper.writeValueAsBytes(envelope);
        RouteResolver.ResolvedRoute route = routeResolver.resolve(request.getRequestURI()).orElseThrow();
        return proxyService.forward(
                route.fullUri(request.getQueryString()), HttpMethod.POST, extractHeaders(request), forwarded);
    }

    protected Map<String, Object> parseEnvelope(byte[] body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> envelope = objectMapper.readValue(body, Map.class);
            return envelope;
        } catch (Exception e) {
            throw new ContractValidationException("Malformed client-data body", List.of(e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> extractData(Map<String, Object> envelope) {
        Object data = envelope.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    protected static HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, Collections.list(request.getHeaders(name)));
        }
        return headers;
    }
}
