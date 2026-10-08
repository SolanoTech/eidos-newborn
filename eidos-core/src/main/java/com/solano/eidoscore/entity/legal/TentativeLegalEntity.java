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

package com.solano.eidoscore.entity.legal;

import com.solano.eidoscore.entity.TentativeReason;
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
 * «Неопределённая» запись юрлица — очередь на разбор оператором:
 * {@code UNKNOWN_SOURCE} (источника нет в справочнике, {@code grLegalEntityId} = null)
 * и {@code GREY_ZONE_CONFLICT} (равный trust, значения расходятся — хранится
 * ВХОДЯЩИЙ снимок конфликтующего источника).
 *
 * <p>Причины переиспользуют общий {@link TentativeReason} — семантика та же,
 * что у физлиц.</p>
 */
@Entity
@Table(
        name = "tentative_legal_entity",
        indexes = {
                @Index(name = "ix_tentative_le_record_id", columnList = "gr_legal_entity_id"),
                @Index(name = "ix_tentative_le_reason", columnList = "reason")
        }
)
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class TentativeLegalEntity extends LegalEntitySnapshotFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Nullable: установлен для grey-zone, null для unknown source. */
    @Column(name = "gr_legal_entity_id")
    private String grLegalEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private TentativeReason reason;

    @Column(name = "source_name")
    private String sourceName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
