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

import com.solano.eidoscore.engine.FieldProvenance;
import com.solano.eidoscore.entity.Source;
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
 * Провенанс каждого поля записи юрлица: какой источник его поставил и когда.
 * Реализует {@link FieldProvenance} — движок работает с ним, не зная о таблице.
 */
@Entity
@Table(name = "legal_entity_field_meta")
@Getter
@Setter
@ToString(exclude = {"legalEntity", "source"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalEntityFieldMeta implements FieldProvenance {

    @EmbeddedId
    private LegalEntityFieldMetaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("grLegalEntityId")
    @JoinColumn(name = "gr_legal_entity_id", nullable = false)
    private LegalEntityRecord legalEntity;

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
