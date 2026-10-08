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

package com.solano.eidoscore.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Тело {@code POST /api/v1/internal/legal-records} — зеркало
 * {@link PostGoldenRecordRequest} для второй вертикали.
 */
@Data
public class PostLegalEntityRequest {

    @NotNull
    @Valid
    @JsonProperty("record")
    private LegalEntityGoldenRecordDto record;

    @NotBlank
    @JsonProperty("source")
    private String source;

    @NotBlank
    @JsonProperty("client_source_identificator")
    private String clientSourceIdentificator;
}
