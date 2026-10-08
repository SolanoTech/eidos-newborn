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

package com.solano.eidoscore.entity.vault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Факт раскрытия обезличенного значения. */
@Entity
@Table(name = "pd_disclosure", schema = "vault")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdDisclosure {

    @Id
    @Column(name = "disclosure_id", nullable = false, updatable = false)
    private UUID disclosureId;

    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    @Column(name = "field_name", nullable = false, length = 64)
    private String fieldName;

    /** Кто запросил раскрытие. */
    @Column(name = "actor", nullable = false, length = 128)
    private String actor;

    /** Основание: без него раскрытие не выполняется. */
    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "disclosed_at", nullable = false)
    private LocalDateTime disclosedAt;
}
