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

import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.exception.ContractValidationException;
import com.solano.eidosgateway.proxy.ProxyService;
import com.solano.eidosgateway.proxy.RouteResolver;
import com.solano.eidosgateway.security.SourceTokenAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Согласия на внешнем интерфейсе: источник может зарегистрировать согласие
 * клиента и спросить его активные согласия по типам.
 *
 * <p>Оба маршрута ведут в eidos-consents (rewrite префикса делает
 * {@link RouteResolver}), но проходят не прозрачным прокси, а через этот
 * контроллер — ради двух правок запроса:</p>
 *
 * <ul>
 *   <li><b>Источник берётся из токена, а не из тела.</b> {@code source_name}
 *       перезаписывается кодом авторизованного источника — так же, как
 *       {@code source} у карточек клиентов (см. {@link ClientDataController}).
 *       Источник не может записать согласие от чужого имени.</li>
 *   <li><b>Идентификатор клиента принимается в том виде, в каком источник его
 *       видит.</b> Внешние API отдают идентификатор Золотой записи как
 *       {@code GR_<uuid>}, а consents хранит его как {@code uuid} — префикс
 *       снимается здесь, чтобы источнику не приходилось знать про эту
 *       разницу.</li>
 * </ul>
 *
 * <p>Остальные операции с согласиями ({@code /{id}/revoke}) по-прежнему идут
 * прозрачным прокси через {@link ProxyController}.</p>
 */
@RestController
@RequestMapping("/api/v1/consents")
public class ConsentController {

    /** Префикс идентификатора Золотой записи физлица: {@code GR_<uuid>}. */
    private static final String GOLDEN_RECORD_PREFIX = "GR_";

    private final ObjectMapper objectMapper;
    private final RouteResolver routeResolver;
    private final ProxyService proxyService;

    public ConsentController(
            ObjectMapper objectMapper,
            RouteResolver routeResolver,
            ProxyService proxyService
    ) {
        this.objectMapper = objectMapper;
        this.routeResolver = routeResolver;
        this.proxyService = proxyService;
    }

    /**
     * Регистрация согласия источником. Тело — как у eidos-consents
     * ({@code type}, {@code client_uuid}, {@code initial_date},
     * {@code end_date}), но {@code source_name} присылать не нужно: он всё
     * равно будет заменён кодом источника из токена.
     */
    @PostMapping
    public ResponseEntity<byte[]> create(HttpServletRequest request, @RequestBody byte[] body) {
        Source source = (Source) request.getAttribute(SourceTokenAuthFilter.SOURCE_ATTRIBUTE);
        Map<String, Object> payload = parseBody(body);

        payload.put("client_uuid", normalizeClientId(payload.get("client_uuid")));
        payload.put("source_name", source.getCode());

        return forward(request, "/api/v1/consents", HttpMethod.POST, objectMapper.writeValueAsBytes(payload));
    }

    /**
     * Согласия клиента по всем типам: на каждый тип — последнее согласие и
     * признак активности на сегодня.
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<byte[]> byTypes(HttpServletRequest request, @PathVariable String clientId) {
        return forward(request, "/api/v1/consents/client/" + normalizeClientId(clientId),
                HttpMethod.GET, null);
    }

    /**
     * Активное согласие одного типа (query-параметр {@code type} — имя
     * константы: {@code PERSONAL_DATA}, {@code BIO}, …). Если активного нет —
     * downstream отвечает 404, и этот ответ проходит как есть.
     */
    @GetMapping("/active/{clientId}")
    public ResponseEntity<byte[]> activeOfType(HttpServletRequest request, @PathVariable String clientId) {
        return forward(request, "/api/v1/consents/active/" + normalizeClientId(clientId),
                HttpMethod.GET, null);
    }

    private ResponseEntity<byte[]> forward(HttpServletRequest request, String gatewayPath,
                                           HttpMethod method, byte[] body) {
        RouteResolver.ResolvedRoute route = routeResolver.resolve(gatewayPath).orElseThrow();
        return proxyService.forward(
                route.fullUri(request.getQueryString()), method, extractHeaders(request), body);
    }

    private Map<String, Object> parseBody(byte[] body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(body, Map.class);
            return payload;
        } catch (Exception e) {
            throw new ContractValidationException("Malformed consent body", List.of(e.getMessage()));
        }
    }

    /**
     * Приводит идентификатор клиента к тому виду, в котором его хранит
     * eidos-consents. Принимается и {@code GR_<uuid>} (как отдают внешние API
     * Золотой записи), и голый {@code uuid}.
     */
    private static String normalizeClientId(Object raw) {
        String value = raw == null ? "" : raw.toString().trim();
        if (value.isEmpty()) {
            throw new ContractValidationException("Consent has no client_uuid", List.of());
        }
        String candidate = value.startsWith(GOLDEN_RECORD_PREFIX)
                ? value.substring(GOLDEN_RECORD_PREFIX.length())
                : value;
        try {
            return UUID.fromString(candidate).toString();
        } catch (IllegalArgumentException e) {
            throw new ContractValidationException(
                    "client_uuid is not a golden record identifier",
                    List.of("expected GR_<uuid> or <uuid>, got: " + value));
        }
    }

    private static HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, Collections.list(request.getHeaders(name)));
        }
        return headers;
    }
}
