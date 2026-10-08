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

package com.solano.eidoscore.repository;

import com.solano.eidoscore.entity.TentativeGoldenRecord;
import com.solano.eidoscore.entity.TentativeReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TentativeGoldenRecordRepository extends JpaRepository<TentativeGoldenRecord, Long> {

    List<TentativeGoldenRecord> findByGrClientId(String grClientId);

    List<TentativeGoldenRecord> findByReason(TentativeReason reason);

    Page<TentativeGoldenRecord> findByReason(TentativeReason reason, Pageable pageable);

    long countByReason(TentativeReason reason);
}
