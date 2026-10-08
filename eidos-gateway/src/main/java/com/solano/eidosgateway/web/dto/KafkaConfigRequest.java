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

package com.solano.eidosgateway.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Тело создания/обновления Kafka-конфигурации во внутреннем admin API.
 * Изменения применяются после перезапуска сервиса (консьюмер поднимается на старте).
 */
@Data
public class KafkaConfigRequest {

    /** Тип сущности, которую несёт топик; по умолчанию — физлица. */
    private com.solano.eidosgateway.entity.registry.EntityType entityType;

    @NotBlank
    private String bootstrapServers;

    @NotBlank
    private String topic;

    @NotBlank
    private String groupId;

    @NotNull
    private Boolean enabled;
}
