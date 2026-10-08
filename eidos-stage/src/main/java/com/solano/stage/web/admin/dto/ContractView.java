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

package com.solano.stage.web.admin.dto;

import com.solano.stage.entity.registry.SourceContract;

import java.util.List;

public record ContractView(
        Long id,
        Long sourceId,
        String sourceCode,
        String clientIdentifierField,
        Integer version,
        List<FieldView> fields
) {

    public static ContractView fromEntity(SourceContract contract) {
        return new ContractView(
                contract.getId(),
                contract.getSource().getId(),
                contract.getSource().getCode(),
                contract.getClientIdentifierField(),
                contract.getVersion() == null ? 1 : contract.getVersion(),
                contract.getFields().stream().map(FieldView::fromEntity).toList()
        );
    }
}
