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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Защищает внутренний (admin) REST {@code /internal/**} статичным токеном из
 * конфигурации ({@code gateway.admin-token}), предъявляемым в заголовке
 * {@code X-Admin-Token}. На остальных путях — пропускает дальше.
 */
public class AdminTokenAuthFilter extends OncePerRequestFilter {

    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private static final String INTERNAL_PREFIX = "/internal/";

    private final String adminToken;

    public AdminTokenAuthFilter(String adminToken) {
        this.adminToken = adminToken;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith(INTERNAL_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        String provided = request.getHeader(ADMIN_TOKEN_HEADER);
        if (provided == null || !provided.equals(adminToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"detail\":\"Missing or invalid " + ADMIN_TOKEN_HEADER + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
