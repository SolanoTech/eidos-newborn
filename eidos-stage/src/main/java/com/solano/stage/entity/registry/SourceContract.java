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

package com.solano.stage.entity.registry;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Дата-контракт источника: набор {@link SourceField} плюс мета-информация о том,
 * какое поле источника является идентификатором клиента.
 *
 * <p>Контрактов у источника столько, сколько типов сущностей он поставляет:
 * один на физлиц, один на юрлиц. Уникальна пара
 * {@code (source_id, entity_type)} — один источник может слать и клиентов, и
 * мерчантов, но разными каналами.</p>
 */
@Entity
@Table(
        name = "source_contract",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contract_per_source_and_type",
                columnNames = {"source_id", "entity_type"}
        )
)
@Getter
@Setter
@ToString(exclude = "fields")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    /**
     * Тип сущности контракта. Задаётся не источником, а каналом приёма;
     * существующие строки читаются как {@link EntityType#PERSON}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    @Builder.Default
    private EntityType entityType = EntityType.PERSON;

    /**
     * Имя поля источника, содержащего идентификатор клиента в системе-источнике
     * ({@code client_source_identificator}, передаётся в eidos-core).
     */
    @Column(name = "client_identifier_field")
    private String clientIdentifierField;

    /**
     * Счётчик версии контракта: инкрементируется при любом изменении меты или
     * полей. Nullable для совместимости с существующими строками (null = 1).
     */
    @Column(name = "version")
    private Integer version;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordering ASC, id ASC")
    @Builder.Default
    private List<SourceField> fields = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Добавляет поле и связывает обе стороны отношения. */
    public void addField(SourceField field) {
        field.setContract(this);
        fields.add(field);
    }
}
