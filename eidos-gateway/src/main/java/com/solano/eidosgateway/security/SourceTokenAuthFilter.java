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

import com.solano.eidosgateway.auth.TokenAuthService;
import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.exception.UnauthorizedException;
import com.solano.eidosgateway.proxy.RouteResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Авторизует внешние (проксируемые) запросы по {@code X-Access-Token}. На
 * остальных путях (внутренний admin API, health) — пропускает дальше.
 * Авторизованный источник кладётся в атрибут запроса {@link #SOURCE_ATTRIBUTE}
 * на случай, если он понадобится ниже по цепочке.
 */
public class SourceTokenAuthFilter extends OncePerRequestFilter {

    public static final String SOURCE_ATTRIBUTE = "gateway.authorizedSource";

    private final RouteResolver routeResolver;
    private final TokenAuthService tokenAuthService;

    public SourceTokenAuthFilter(RouteResolver routeResolver, TokenAuthService tokenAuthService) {
        this.routeResolver = routeResolver;
        this.tokenAuthService = tokenAuthService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!routeResolver.isExternalRoute(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Source source = tokenAuthService.authorize(request.getHeader(TokenAuthService.ACCESS_TOKEN_HEADER));
            request.setAttribute(SOURCE_ATTRIBUTE, source);
        } catch (UnauthorizedException e) {
            writeUnauthorized(response, e.getMessage());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static void writeUnauthorized(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"detail\":\"" + detail + "\"}");
    }
}
