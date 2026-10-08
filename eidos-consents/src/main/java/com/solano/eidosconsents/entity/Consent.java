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

package com.solano.eidosconsents.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Согласие/соглашение клиента, таблица {@code consents}.
 *
 * <p>Идентификатор генерируется в сервисе, поэтому {@code @GeneratedValue} не
 * используется.</p>
 */
@Entity
@Table(name = "consents")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "client_uuid", nullable = false)
    private UUID clientUuid;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConsentType type;

    @Column(name = "source", nullable = false)
    private String source;

    /**
     * День, в который действующее согласие было отозвано; {@code null}, если
     * не отзывалось. По нему планировщик истечений узнаёт, что о потере
     * согласия уже сообщил отзыв (см. {@code ConsentRepository#findExpiredWithoutActiveConsent}).
     */
    @Column(name = "revoked_on")
    private LocalDate revokedOn;

    /** Действует ли согласие в день {@code date}: {@code start_date <= date <= end_date}. */
    public boolean isActiveOn(LocalDate date) {
        return !startDate.isAfter(date) && !endDate.isBefore(date);
    }
}
