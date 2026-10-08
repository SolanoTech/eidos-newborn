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

import com.solano.eidosgateway.entity.gateway.KafkaConfig;

public record KafkaConfigView(Long id, String entityType, String bootstrapServers,
                             String topic, String groupId, Boolean enabled) {

    public static KafkaConfigView fromEntity(KafkaConfig config) {
        return new KafkaConfigView(
                config.getId(),
                config.getEntityType() == null ? "PERSON" : config.getEntityType().name(),
                config.getBootstrapServers(),
                config.getTopic(),
                config.getGroupId(),
                config.getEnabled()
        );
    }
}
