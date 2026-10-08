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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Снимок {@link LegalEntityRecord} в момент изменения — пишется движком после
 * каждого успешного merge. На один {@code gr_legal_entity_id} приходится N ревизий;
 * {@code archivedVersion} хранит значение {@code @Version} записи.
 */
@Entity
@Table(
        name = "legal_entity_archive",
        indexes = @Index(name = "ix_le_archive_record_id", columnList = "gr_legal_entity_id")
)
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class LegalEntityArchive extends LegalEntitySnapshotFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "gr_legal_entity_id", nullable = false, updatable = false)
    private String grLegalEntityId;

    @Column(name = "archived_version", nullable = false)
    private Integer archivedVersion;

    @CreationTimestamp
    @Column(name = "archived_at", nullable = false, updatable = false)
    private LocalDateTime archivedAt;
}
