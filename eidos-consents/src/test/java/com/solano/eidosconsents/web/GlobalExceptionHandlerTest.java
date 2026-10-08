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

package com.solano.eidosconsents.web;

import com.solano.eidosconsents.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unknownPath_isNotFound_notServerError() {
        ResponseEntity<Map<String, String>> response = handler.handleNoResource(
                new NoResourceFoundException(HttpMethod.GET,
                        "/internal/api/v1/no-such-route", "internal/api/v1/no-such-route"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry(
                "detail", "No endpoint GET /internal/api/v1/no-such-route");
    }

    @Test
    void domainNotFound_keepsItsOwnMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNotFound(new NotFoundException("No active consent of type BIO"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("detail", "No active consent of type BIO");
    }
}
