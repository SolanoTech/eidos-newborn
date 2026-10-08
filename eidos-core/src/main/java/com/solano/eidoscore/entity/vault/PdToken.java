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

/** Отображение токена на зашифрованное персональное значение. */
@Entity
@Table(name = "pd_token", schema = "vault")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdToken {

    /** Значение, которое встанет в профильную таблицу вместо персональных данных. */
    @Id
    @Column(name = "token", nullable = false, updatable = false, length = 64)
    private String token;

    @Column(name = "dek_id", nullable = false)
    private UUID dekId;

    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    /** Какое поле заменено — нужно при обратном переходе. */
    @Column(name = "field_name", nullable = false, length = 64)
    private String fieldName;

    @Column(name = "ciphertext", nullable = false)
    private byte[] ciphertext;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
