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

import com.solano.eidoscore.engine.FieldProvenance;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Provenance каждого поля Золотой записи: какой источник его поставил и когда
 * последний раз обновлялся. Используется merger'ом для разрешения конфликтов
 * по trust_level.
 *
 * <p>PK композитный: {@code (grClientId, fieldName)}.</p>
 */
@Entity
@Table(name = "golden_record_field_meta")
@Getter
@Setter
@ToString(exclude = {"goldenRecord", "source"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldenRecordFieldMeta implements FieldProvenance {

    @EmbeddedId
    private GoldenRecordFieldMetaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("grClientId")
    @JoinColumn(name = "gr_client_id", nullable = false)
    private GoldenRecord goldenRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public String getFieldName() {
        return this.id.getFieldName();
    }
}
