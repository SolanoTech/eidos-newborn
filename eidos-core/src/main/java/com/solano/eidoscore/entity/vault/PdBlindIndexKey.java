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

/** Ключ слепого индекса — хранится только завёрнутым, как и ключи данных. */
@Entity
@Table(name = "pd_blind_index_key", schema = "vault")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdBlindIndexKey {

    @Id
    @Column(name = "key_version", nullable = false, updatable = false)
    private Integer keyVersion;

    @Column(name = "wrapped_key", nullable = false)
    private byte[] wrappedKey;

    @Column(name = "kek_name", nullable = false, length = 64)
    private String kekName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
