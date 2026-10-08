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

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * Определение одного поля источника в конструкторе: откуда брать значение в
 * исходных данных, в какое поле Золотой записи его класть и какие правила
 * приведения/валидации применять.
 */
@Entity
@Table(name = "source_field")
@Getter
@Setter
@ToString(exclude = "contract")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private SourceContract contract;

    /** Имя/путь поля в исходных данных (dot-path, напр. {@code person.firstName}). */
    @Column(name = "source_field_name", nullable = false)
    private String sourceFieldName;

    /** Целевое поле Золотой записи — JSON-имя, напр. {@code GR_FirstName}. */
    @Column(name = "target_gr_field", nullable = false)
    private String targetGrField;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 16)
    private FieldDataType dataType;

    @Column(name = "required", nullable = false)
    @Builder.Default
    private Boolean required = Boolean.FALSE;

    /** Формат даты в источнике (напр. {@code dd.MM.yyyy}); для {@code DATE}. */
    @Column(name = "source_date_format")
    private String sourceDateFormat;

    /** Regex, которому должно соответствовать строковое значение (валидация на gateway). */
    @Column(name = "validation_regex")
    private String validationRegex;

    /** Значение по умолчанию / константа, если в источнике значения нет. */
    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "ordering", nullable = false)
    @Builder.Default
    private Integer ordering = 0;

    /** Таблица соответствий значений источника → значениям Золотой записи (напр. {@code 1→M}). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "source_field_value_map",
            joinColumns = @JoinColumn(name = "field_id")
    )
    @MapKeyColumn(name = "source_value")
    @Column(name = "target_value")
    @Builder.Default
    private Map<String, String> valueMap = new HashMap<>();
}
