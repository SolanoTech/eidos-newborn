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

package com.solano.eidoscdiuibackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Событие аудита действий в консоли (кто, что, когда). Пишется автоматически
 * при операциях через ui-backend; журнал только пополняется, изменение и
 * удаление записей API не предусмотрено.
 */
@Entity
@Table(name = "audit_events", indexes = @Index(name = "ix_audit_created_at", columnList = "created_at"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Логин пользователя консоли либо {@code system}. */
    @Column(name = "actor", nullable = false, length = 64)
    private String actor;

    @Column(name = "action", nullable = false, length = 256)
    private String action;

    @Column(name = "details", length = 1024)
    private String details;

    /** Визуальная категория для UI-таймлайна: gold | ok | warn | bad. */
    @Column(name = "category", nullable = false, length = 16)
    private String category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
