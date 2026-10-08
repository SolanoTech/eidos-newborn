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

package com.solano.eidoscore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * «Неопределённая» Golden Record. Сюда падают данные, которые система не может
 * безопасно записать в основную таблицу {@code golden_record}:
 *
 * <ul>
 *   <li>{@link TentativeReason#UNKNOWN_SOURCE} — пришёл запрос от источника,
 *       которого нет в справочнике. {@code grClientId} = null.</li>
 *   <li>{@link TentativeReason#GREY_ZONE_CONFLICT} — merge столкнулся с
 *       равным trust_level, но разными значениями. {@code grClientId} = id
 *       Golden Record, в которой произошёл конфликт.</li>
 * </ul>
 *
 * <p>Все бизнес-поля nullable: при unknown source мы получаем DTO «как есть»,
 * а в случае grey-zone сохраняем снимок текущей Golden Record.</p>
 */
@Entity
@Table(
        name = "tentative_golden_record",
        indexes = {
                @Index(name = "ix_tentative_gr_client_id", columnList = "gr_client_id"),
                @Index(name = "ix_tentative_reason", columnList = "reason")
        }
)
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class TentativeGoldenRecord extends GoldenRecordSnapshotFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Nullable. Установлен для grey-zone, null для unknown source. */
    @Column(name = "gr_client_id")
    private String grClientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private TentativeReason reason;

    /**
     * Имя источника, с которым связано появление записи: для
     * {@code UNKNOWN_SOURCE} — то, что прислал клиент; для
     * {@code GREY_ZONE_CONFLICT} — имя нового (конфликтующего) источника.
     */
    @Column(name = "source_name")
    private String sourceName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
