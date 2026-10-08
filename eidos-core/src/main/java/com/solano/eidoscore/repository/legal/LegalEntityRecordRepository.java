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

package com.solano.eidoscore.repository.legal;

import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LegalEntityRecordRepository extends JpaRepository<LegalEntityRecord, String> {

    /** Точный поиск: ИНН — самодостаточный ключ юрлица. */
    Optional<LegalEntityRecord> findByGrInn(String inn);

    /**
     * Неточный поиск по названию мерчанта: триграммное сходство
     * нормализованного названия (pg_trgm). Требует расширения
     * {@code pg_trgm} и GIN-индекса — см. docker/postgres-init.
     */
    @Query(value = """
            SELECT * FROM legal_entity_record r
            WHERE similarity(r.name_normalized, :query) >= :threshold
            ORDER BY similarity(r.name_normalized, :query) DESC, r.gr_full_name
            """,
            countQuery = """
                    SELECT count(*) FROM legal_entity_record r
                    WHERE similarity(r.name_normalized, :query) >= :threshold
                    """,
            nativeQuery = true)
    Page<LegalEntityRecord> fuzzyByName(@Param("query") String normalizedQuery,
                                        @Param("threshold") double threshold,
                                        Pageable pageable);
}
