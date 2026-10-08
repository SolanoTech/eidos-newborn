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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Суточная статистика приёма по источнику: сколько записей принято (дошло до
 * core) и сколько отклонено валидацией (gateway или stage). Живёт в общей БД
 * {@code eidos_registry}: stage инкрементит принятые, gateway — отклонённые.
 * Питает дашборд «Активность источников».
 */
@Entity
@Table(
        name = "ingest_stats",
        uniqueConstraints = @UniqueConstraint(name = "ux_ingest_stats_source_day", columnNames = {"source_code", "day"})
)
@Getter
@Setter
@NoArgsConstructor
public class IngestStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "source_code", nullable = false, length = 64)
    private String sourceCode;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "accepted", nullable = false)
    private long accepted;

    @Column(name = "rejected", nullable = false)
    private long rejected;
}
