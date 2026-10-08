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

package com.solano.eidosgateway.entity.registry;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * Определение поля источника в конструкторе (read-only для gateway). Содержит
 * правила, по которым gateway валидирует входящие данные.
 */
@Entity
@Table(name = "source_field")
@Getter
@Setter
@ToString(exclude = "contract")
@NoArgsConstructor
public class SourceField {

    @Id
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private SourceContract contract;

    @Column(name = "source_field_name")
    private String sourceFieldName;

    @Column(name = "target_gr_field")
    private String targetGrField;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 16)
    private FieldDataType dataType;

    @Column(name = "required")
    private Boolean required;

    @Column(name = "source_date_format")
    private String sourceDateFormat;

    @Column(name = "validation_regex")
    private String validationRegex;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "ordering")
    private Integer ordering;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "source_field_value_map", joinColumns = @JoinColumn(name = "field_id"))
    @MapKeyColumn(name = "source_value")
    @Column(name = "target_value")
    private Map<String, String> valueMap = new HashMap<>();
}
