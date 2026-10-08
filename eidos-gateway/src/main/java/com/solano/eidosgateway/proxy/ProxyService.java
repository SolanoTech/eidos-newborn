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

package com.solano.eidosgateway.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Set;

/**
 * Прозрачно проксирует HTTP-запрос в downstream-сервис: переносит метод, путь,
 * query, тело и заголовки, возвращает ответ как есть (любой статус, включая
 * 4xx/5xx — через {@code exchange}, без выброса исключений). Gateway ничего не
 * валидирует.
 */
@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);

    /** Hop-by-hop и служебные заголовки, которые не пробрасываем. */
    private static final Set<String> SKIP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade",
            "host", "content-length",
            "x-access-token", "x-admin-token"
    );

    private final RestClient restClient;

    public ProxyService(RestClient gatewayRestClient) {
        this.restClient = gatewayRestClient;
    }

    public ResponseEntity<byte[]> forward(String uri, HttpMethod method, HttpHeaders incomingHeaders, byte[] body) {
        log.debug("Proxying {} {}", method, uri);

        RestClient.RequestBodySpec spec = restClient.method(method).uri(URI.create(uri));
        incomingHeaders.forEach((name, values) -> {
            if (!SKIP_HEADERS.contains(name.toLowerCase())) {
                spec.header(name, values.toArray(new String[0]));
            }
        });
        if (body != null && body.length > 0) {
            spec.body(body);
        }

        return spec.exchange((request, response) -> {
            byte[] responseBody = response.getBody().readAllBytes();
            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((name, values) -> {
                if (!SKIP_HEADERS.contains(name.toLowerCase())) {
                    responseHeaders.put(name, values);
                }
            });
            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(responseBody);
        });
    }
}
