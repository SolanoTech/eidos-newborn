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

import com.solano.eidosgateway.exception.NotFoundException;
import com.solano.eidosgateway.proxy.ProxyService;
import com.solano.eidosgateway.proxy.RouteResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Enumeration;

/**
 * Внешний интерфейс Gateway. Авторизация уже выполнена
 * {@code SourceTokenAuthFilter}; здесь запрос только маршрутизируется и
 * прозрачно проксируется в downstream-сервис.
 *
 * <p>Маршруты заданы префиксами (а не {@code /**}), чтобы не перехватывать
 * служебные пути ({@code /health}, {@code /internal/**}, {@code /error}).</p>
 *
 * <p>Исключение — согласия: регистрация и чтение по типам обслуживаются
 * {@link ConsentController}, потому что там запрос правится (источник из
 * токена, идентификатор Золотой записи). Сюда попадают остальные операции
 * согласий, например отзыв по идентификатору.</p>
 */
@RestController
@RequestMapping({
        "/api/v1/external/golden-records/**",
        "/api/v1/consents/**"
})
public class ProxyController {

    private final RouteResolver routeResolver;
    private final ProxyService proxyService;

    public ProxyController(RouteResolver routeResolver, ProxyService proxyService) {
        this.routeResolver = routeResolver;
        this.proxyService = proxyService;
    }

    @RequestMapping
    public ResponseEntity<byte[]> proxy(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI();
        RouteResolver.ResolvedRoute route = routeResolver.resolve(path)
                .orElseThrow(() -> new NotFoundException("No downstream route for " + path));

        String uri = route.fullUri(request.getQueryString());
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        return proxyService.forward(uri, method, extractHeaders(request), body);
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
