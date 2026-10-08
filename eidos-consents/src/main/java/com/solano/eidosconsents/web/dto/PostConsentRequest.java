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

package com.solano.eidosconsents.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.solano.eidosconsents.entity.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Тело {@code POST /internal/api/v1/consents}: имена полей в snake_case,
 * {@code type} принимает короткие коды {@code pd} / {@code bio},
 * {@code end_date} необязателен.
 */
@Data
public class PostConsentRequest {

    @NotNull
    @JsonProperty("type")
    private ConsentType type;

    @NotNull
    @JsonProperty("client_uuid")
    private UUID clientUuid;

    @NotNull
    @JsonProperty("initial_date")
    private LocalDate initialDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @NotBlank
    @JsonProperty("source_name")
    private String sourceName;
}
