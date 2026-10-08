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

package com.solano.eidoscdiuibackend.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Цепочка безопасности resource server'а: без токена — 401, с токеном без роли
 * ADMIN — 403, с ролью — 200, /health публичен. Разбор realm_access.roles из
 * реального токена Keycloak проверяется e2e (юнитом jwt() подставляет
 * authorities напрямую, минуя конвертер).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "viewer"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit")
                        .with(jwt()
                                .jwt(j -> j.claim("preferred_username", "admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }
}
