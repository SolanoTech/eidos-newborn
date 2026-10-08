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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Сопоставляет путь внешнего запроса Gateway с downstream-сервисом и целевым
 * путём. Внешний контракт «чистый» (без {@code /internal} наружу); для consents
 * выполняется rewrite префикса в его внутренний путь.
 *
 * <p>Маршруты (выбирается самый длинный совпавший префикс):</p>
 * <ul>
 *   <li>{@code /api/v1/external/golden-records/**} → eidos-core (без rewrite);</li>
 *   <li>{@code /api/v1/consents/**} → eidos-consents, rewrite в
 *       {@code /internal/api/v1/consents/**};</li>
 *   <li>{@code /api/v1/client-data/**} → eidos-stage (без rewrite);</li>
 *   <li>{@code /api/v1/legal-data/**} → eidos-stage (без rewrite) — отдельный
 *       канал юрлиц: тип сущности задаётся эндпоинтом.</li>
 * </ul>
 */
@Component
public class RouteResolver {

    /** Один маршрут: префикс на Gateway → база downstream + целевой префикс. */
    public record Route(String gatewayPrefix, String targetBaseUrl, String targetPrefix) {
    }

    /** Разрешённый downstream-адрес для конкретного запроса. */
    public record ResolvedRoute(String targetBaseUrl, String targetPath) {
        public String fullUri(String query) {
            String url = targetBaseUrl + targetPath;
            return (query == null || query.isBlank()) ? url : url + "?" + query;
        }
    }

    private final List<Route> routes;

    public RouteResolver(
            @Value("${eidos.core.base-url}") String coreBaseUrl,
            @Value("${eidos.consents.base-url}") String consentsBaseUrl,
            @Value("${eidos.stage.base-url}") String stageBaseUrl
    ) {
        this.routes = List.of(
                new Route("/api/v1/external/golden-records",
                        stripTrailingSlash(coreBaseUrl), "/api/v1/external/golden-records"),
                new Route("/api/v1/consents",
                        stripTrailingSlash(consentsBaseUrl), "/internal/api/v1/consents"),
                new Route("/api/v1/client-data",
                        stripTrailingSlash(stageBaseUrl), "/api/v1/client-data"),
                new Route("/api/v1/legal-data",
                        stripTrailingSlash(stageBaseUrl), "/api/v1/legal-data")
        );
    }

    /** Находит downstream-адрес для пути запроса, выполняя rewrite префикса. */
    public Optional<ResolvedRoute> resolve(String requestPath) {
        return routes.stream()
                .filter(r -> matches(requestPath, r.gatewayPrefix()))
                .findFirst()
                .map(r -> {
                    String remainder = requestPath.substring(r.gatewayPrefix().length());
                    return new ResolvedRoute(r.targetBaseUrl(), r.targetPrefix() + remainder);
                });
    }

    /** Является ли путь внешним проксируемым маршрутом (используется фильтром авторизации). */
    public boolean isExternalRoute(String requestPath) {
        return routes.stream().anyMatch(r -> matches(requestPath, r.gatewayPrefix()));
    }

    private static boolean matches(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
