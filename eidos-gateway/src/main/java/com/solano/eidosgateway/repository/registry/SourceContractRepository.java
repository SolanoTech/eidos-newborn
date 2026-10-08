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

package com.solano.eidosgateway.repository.registry;

import com.solano.eidosgateway.entity.registry.EntityType;
import com.solano.eidosgateway.entity.registry.SourceContract;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SourceContractRepository extends JpaRepository<SourceContract, Long> {

    // Жадно подгружаем поля контракта И их value-map: ContractValidator читает
    // то и другое вне транзакции (open-in-view=false). Граф типа FETCH делает не
    // перечисленные атрибуты LAZY, поэтому value-map указываем явно.
    @EntityGraph(attributePaths = {"fields", "fields.valueMap"})
    Optional<SourceContract> findBySourceCodeAndEntityType(String sourceCode, EntityType entityType);
}
