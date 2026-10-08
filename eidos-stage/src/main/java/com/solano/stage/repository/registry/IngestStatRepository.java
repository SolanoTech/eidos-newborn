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

package com.solano.stage.repository.registry;

import com.solano.stage.entity.registry.IngestStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface IngestStatRepository extends JpaRepository<IngestStat, Long> {

    /** Атомарный upsert суточного счётчика (PostgreSQL). */
    @Modifying
    @Query(value = """
            INSERT INTO ingest_stats(source_code, day, accepted, rejected)
            VALUES (:code, :day, :acc, :rej)
            ON CONFLICT (source_code, day)
            DO UPDATE SET accepted = ingest_stats.accepted + :acc,
                          rejected = ingest_stats.rejected + :rej
            """, nativeQuery = true)
    void increment(@Param("code") String code, @Param("day") LocalDate day,
                   @Param("acc") long accepted, @Param("rej") long rejected);

    List<IngestStat> findByDayGreaterThanEqual(LocalDate from);
}
