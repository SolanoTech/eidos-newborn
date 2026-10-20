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

package com.solano.eidosgateway.security;

import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.proxy.RouteResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ограничивает частоту внешних запросов — по источнику, а не по адресу:
 * источник уже известен из {@code X-Access-Token}, а адрес за обратным прокси
 * у всех один.
 *
 * <p>Нечёткий поиск считается отдельно и строже остального внешнего API.
 * Поиск по имени — канал перечисления базы: один запрос возвращает список
 * идентификаторов, и без ограничения достаточно перебрать распространённые
 * фамилии, чтобы собрать существенную часть картотеки. Приём данных, наоборот,
 * по своей природе частый, и ставить ему тот же предел нельзя.</p>
 *
 * <p>Фильтр стоит после {@link SourceTokenAuthFilter}: запросы без токена
 * отклоняются раньше и квоту источника не тратят. Обратная сторона — поток
 * запросов с негодным токеном ограничителя не касается; защита от него
 * остаётся за обратным прокси.</p>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    /** Нечёткий поиск физлиц: единственный путь с отдельным, более строгим пределом. */
    static final String FUZZY_SEARCH_PATH = "/api/v1/external/golden-records/search/fuzzy";

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RouteResolver routeResolver;
    private final RateLimiter rateLimiter;
    private final boolean enabled;
    private final int perMinute;
    private final int fuzzySearchPerMinute;

    public RateLimitFilter(
            RouteResolver routeResolver,
            RateLimiter rateLimiter,
            boolean enabled,
            int perMinute,
            int fuzzySearchPerMinute
    ) {
        this.routeResolver = routeResolver;
        this.rateLimiter = rateLimiter;
        this.enabled = enabled;
        this.perMinute = perMinute;
        this.fuzzySearchPerMinute = fuzzySearchPerMinute;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!enabled || !routeResolver.isExternalRoute(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean fuzzySearch = isFuzzySearch(path);
        int limit = fuzzySearch ? fuzzySearchPerMinute : perMinute;
        String client = clientKey(request);
        RateLimiter.Decision decision =
                rateLimiter.tryAcquire(client + (fuzzySearch ? "|fuzzy" : "|external"), limit);

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("Rate limit exceeded for {} on {} ({} requests per minute)", client, path, limit);
        writeTooManyRequests(response, decision.retryAfterSeconds());
    }

    private static boolean isFuzzySearch(String path) {
        return path.equals(FUZZY_SEARCH_PATH) || path.startsWith(FUZZY_SEARCH_PATH + "/");
    }

    /**
     * Ключ, по которому считается частота: код источника из авторизации.
     * Запрос без источника до фильтра не доходит, но если область фильтра
     * когда-нибудь разойдётся с областью авторизации, считать по адресу
     * честнее, чем сложить всех в одно ведро.
     */
    private static String clientKey(HttpServletRequest request) {
        Object source = request.getAttribute(SourceTokenAuthFilter.SOURCE_ATTRIBUTE);
        if (source instanceof Source s && s.getCode() != null) {
            return "source:" + s.getCode();
        }
        return "addr:" + request.getRemoteAddr();
    }

    private static void writeTooManyRequests(HttpServletResponse response, int retryAfterSeconds)
            throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"detail\":\"Rate limit exceeded, retry in "
                + retryAfterSeconds + " seconds\"}");
    }
}
