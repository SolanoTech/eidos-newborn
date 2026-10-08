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

package com.solano.eidosgateway.config;

import com.solano.eidosgateway.auth.TokenAuthService;
import com.solano.eidosgateway.proxy.RouteResolver;
import com.solano.eidosgateway.security.AdminTokenAuthFilter;
import com.solano.eidosgateway.security.SourceTokenAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security сам по себе пропускает всё ({@code permitAll}); фактическая
 * авторизация выполняется двумя кастомными фильтрами:
 *
 * <ul>
 *   <li>{@link AdminTokenAuthFilter} — внутренний {@code /internal/**} по
 *       {@code X-Admin-Token};</li>
 *   <li>{@link SourceTokenAuthFilter} — внешние проксируемые маршруты по
 *       {@code X-Access-Token} из БД источников.</li>
 * </ul>
 *
 * <p>Каждый фильтр действует только на своей области путей, на прочих —
 * прозрачно пропускает запрос.</p>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            RouteResolver routeResolver,
            TokenAuthService tokenAuthService,
            @Value("${gateway.admin-token}") String adminToken
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new AdminTokenAuthFilter(adminToken),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new SourceTokenAuthFilter(routeResolver, tokenAuthService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
