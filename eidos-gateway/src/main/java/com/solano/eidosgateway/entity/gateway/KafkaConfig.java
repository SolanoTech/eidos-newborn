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

package com.solano.eidosgateway.entity.gateway;

import com.solano.eidosgateway.entity.registry.EntityType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Конфигурация Kafka-консьюмера, хранится в собственной БД Gateway.
 *
 * <p>По одной записи на тип сущности: у физлиц и юрлиц <b>разные топики</b>, и
 * именно топик определяет, какой Золотой записью станут данные — источник тип
 * не присылает. При старте сервис поднимает консьюмера на каждую включённую
 * запись; нет записей — Kafka-интерфейс не запускается (Gateway работает как
 * REST).</p>
 */
@Entity
@Table(
        name = "kafka_config",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_kafka_config_per_entity_type",
                columnNames = "entity_type"
        )
)
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Тип сущности, которую несёт топик. Существующие строки — физлица. */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    @Builder.Default
    private EntityType entityType = EntityType.PERSON;

    @Column(name = "bootstrap_servers", nullable = false)
    private String bootstrapServers;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    /** Флаг включения; позволяет хранить конфиг, но временно не поднимать консьюмера. */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = Boolean.TRUE;
}
