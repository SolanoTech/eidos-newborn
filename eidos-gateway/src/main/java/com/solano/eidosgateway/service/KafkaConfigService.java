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

package com.solano.eidosgateway.service;

import com.solano.eidosgateway.entity.gateway.KafkaConfig;
import com.solano.eidosgateway.entity.registry.EntityType;
import com.solano.eidosgateway.exception.NotFoundException;
import com.solano.eidosgateway.kafka.KafkaConfigChangedEvent;
import com.solano.eidosgateway.repository.gateway.KafkaConfigRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Управление Kafka-конфигурациями Gateway — по одной на тип сущности
 * (физлица и юрлица приходят разными топиками). Изменения применяются сразу:
 * консьюмеры пересоздаются после коммита транзакции.
 */
@Service
public class KafkaConfigService {

    private final KafkaConfigRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public KafkaConfigService(KafkaConfigRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public KafkaConfig getCurrent() {
        return getByEntityType(EntityType.PERSON);
    }

    @Transactional(readOnly = true)
    public KafkaConfig getByEntityType(EntityType entityType) {
        return repository.findByEntityType(entityType)
                .orElseThrow(() -> new NotFoundException(
                        "Kafka config for " + entityType + " is not set"));
    }

    @Transactional(readOnly = true)
    public java.util.List<KafkaConfig> listAll() {
        return repository.findAll();
    }

    /** Upsert конфигурации физлиц (совместимость со старым API). */
    @Transactional
    public KafkaConfig save(String bootstrapServers, String topic, String groupId, Boolean enabled) {
        return save(EntityType.PERSON, bootstrapServers, topic, groupId, enabled);
    }

    /** Upsert конфигурации для конкретного типа сущности. */
    @Transactional
    public KafkaConfig save(EntityType entityType, String bootstrapServers,
                            String topic, String groupId, Boolean enabled) {
        KafkaConfig config = repository.findByEntityType(entityType).orElseGet(KafkaConfig::new);
        config.setEntityType(entityType);
        config.setBootstrapServers(bootstrapServers);
        config.setTopic(topic);
        config.setGroupId(groupId);
        config.setEnabled(enabled);
        KafkaConfig saved = repository.save(config);
        // Консьюмер перечитает БД и пересоздаст контейнер после коммита транзакции.
        eventPublisher.publishEvent(new KafkaConfigChangedEvent());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<KafkaConfig> findActive() {
        return repository.findFirstByEnabledTrueOrderByIdAsc();
    }

    /** Все включённые конфигурации — на каждую поднимается свой консьюмер. */
    @Transactional(readOnly = true)
    public java.util.List<KafkaConfig> findAllActive() {
        return repository.findByEnabledTrueOrderByIdAsc();
    }
}
