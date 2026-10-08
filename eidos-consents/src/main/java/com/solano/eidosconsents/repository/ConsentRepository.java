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

package com.solano.eidosconsents.repository;

import com.solano.eidosconsents.entity.Consent;
import com.solano.eidosconsents.entity.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Доступ к согласиям. Сохранение — унаследованный {@link JpaRepository#save},
 * отдельного метода не требуется.
 */
@Repository
public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    /**
     * Последнее активное согласие клиента указанного типа на заданную дату.
     *
     * <p>«Активное» — дата попадает в диапазон {@code [start_date, end_date]};
     * «последнее» — с самым поздним {@code start_date}. Возвращается не более
     * одной записи ({@code findFirst...}).</p>
     */
    Optional<Consent> findFirstByClientUuidAndTypeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            UUID clientUuid, ConsentType type, LocalDate startBoundary, LocalDate endBoundary);

    /** Последнее согласие клиента данного типа (независимо от активности). */
    Optional<Consent> findFirstByClientUuidAndTypeOrderByStartDateDesc(UUID clientUuid, ConsentType type);

    /**
     * Согласия, у которых {@code lastActiveDay} был последним днём действия и
     * у клиента <b>не осталось</b> действующего на {@code date} согласия того
     * же типа.
     *
     * <p>Это и есть повод для события {@code expired}: важно не то, что
     * закончилась конкретная строка, а то, что клиент перестал быть согласен.
     * Если рядом лежит более свежее согласие того же типа, ничего не
     * произошло.</p>
     *
     * <p>Пары «клиент + тип», в которых в день {@code date} было отозвано
     * действовавшее согласие, пропускаются: о потере согласия в этот день уже
     * сообщил отзыв (см. {@code ConsentRevocationListener}), и второе событие
     * было бы повтором. Согласия, которые ни дня не действовали (отозваны до
     * начала срока, {@code start_date > end_date}), поводом не считаются.</p>
     *
     * <p>Порядок фиксирован, причём внутри пары «клиент + тип» первым идёт
     * самое позднее согласие — именно его даты уходят в событие.</p>
     */
    @Query("""
            select c from Consent c
            where c.endDate = :lastActiveDay
              and c.startDate <= c.endDate
              and not exists (
                  select 1 from Consent a
                  where a.clientUuid = c.clientUuid
                    and a.type = c.type
                    and a.startDate <= :date
                    and a.endDate >= :date
              )
              and not exists (
                  select 1 from Consent r
                  where r.clientUuid = c.clientUuid
                    and r.type = c.type
                    and r.revokedOn = :date
                    and r.startDate <= :date
              )
            order by c.clientUuid asc, c.type asc, c.startDate desc
            """)
    List<Consent> findExpiredWithoutActiveConsent(
            @Param("lastActiveDay") LocalDate lastActiveDay,
            @Param("date") LocalDate date);
}
