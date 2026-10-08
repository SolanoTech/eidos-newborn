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

package com.solano.stage.web.admin;

import com.solano.stage.entity.registry.Source;
import com.solano.stage.exception.BadRequestException;
import com.solano.stage.exception.NotFoundException;
import com.solano.stage.service.admin.SourceAdminService;
import com.solano.stage.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SourceAdminControllerTest {

    @Mock
    SourceAdminService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SourceAdminController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_returns201_withSourceView() throws Exception {
        when(service.create(eq("tieto"), eq("Tieto"), eq("tok-1"), eq(50), any()))
                .thenReturn(Source.builder().id(1L).code("tieto").name("Tieto").token("tok-1").trustLevel(50).enabled(true).build());

        mockMvc.perform(post("/internal/api/v1/sources")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"tieto\",\"name\":\"Tieto\",\"token\":\"tok-1\",\"trustLevel\":50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("tieto"))
                .andExpect(jsonPath("$.trustLevel").value(50));

        verify(service).create("tieto", "Tieto", "tok-1", 50, null);
    }

    @Test
    void create_duplicateCode_returns400() throws Exception {
        when(service.create(any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException("Source code 'tieto' already exists"));

        mockMvc.perform(post("/internal/api/v1/sources")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"tieto\",\"name\":\"Tieto\",\"token\":\"tok-1\",\"trustLevel\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Source code 'tieto' already exists"));
    }

    @Test
    void create_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/internal/api/v1/sources")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"tieto\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_missing_returns404() throws Exception {
        when(service.findById(99L)).thenThrow(new NotFoundException("Source 99 not found"));

        mockMvc.perform(get("/internal/api/v1/sources/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Source 99 not found"));
    }
}
