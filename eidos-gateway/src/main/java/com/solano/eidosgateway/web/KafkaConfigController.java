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

package com.solano.eidosgateway.web;

import com.solano.eidosgateway.entity.gateway.KafkaConfig;
import com.solano.eidosgateway.entity.registry.EntityType;
import com.solano.eidosgateway.service.KafkaConfigService;
import com.solano.eidosgateway.web.dto.KafkaConfigRequest;
import com.solano.eidosgateway.web.dto.KafkaConfigView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Внутренний admin API Kafka-конфигураций — по одной на тип сущности
 * (физлица и юрлица приходят разными топиками). Защищён {@code X-Admin-Token};
 * изменения применяются сразу — консьюмеры пересоздаются.
 */
@RestController
@RequestMapping("/internal/api/v1/kafka-config")
public class KafkaConfigController {

    private final KafkaConfigService kafkaConfigService;

    public KafkaConfigController(KafkaConfigService kafkaConfigService) {
        this.kafkaConfigService = kafkaConfigService;
    }

    @GetMapping
    public KafkaConfigView get(@RequestParam(defaultValue = "PERSON") EntityType entityType) {
        return KafkaConfigView.fromEntity(kafkaConfigService.getByEntityType(entityType));
    }

    /** Все настроенные каналы — по одному на тип сущности. */
    @GetMapping("/all")
    public java.util.List<KafkaConfigView> list() {
        return kafkaConfigService.listAll().stream().map(KafkaConfigView::fromEntity).toList();
    }

    @PutMapping
    public KafkaConfigView upsert(@Valid @RequestBody KafkaConfigRequest request) {
        KafkaConfig saved = kafkaConfigService.save(
                request.getEntityType() == null ? EntityType.PERSON : request.getEntityType(),
                request.getBootstrapServers(),
                request.getTopic(),
                request.getGroupId(),
                request.getEnabled()
        );
        return KafkaConfigView.fromEntity(saved);
    }
}
