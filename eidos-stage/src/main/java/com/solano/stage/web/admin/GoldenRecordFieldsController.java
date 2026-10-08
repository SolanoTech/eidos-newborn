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

package com.solano.stage.web.admin;

import com.solano.shared.dto.RecordFieldCatalog;
import com.solano.stage.entity.registry.EntityType;
import com.solano.stage.service.admin.ContractAdminService;
import com.solano.stage.web.admin.dto.GoldenRecordFieldView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Каталог целевых полей Золотой записи для UI конструктора. Защищён
 * {@code X-Admin-Token} (путь под {@code /internal/}).
 */
@RestController
@RequestMapping("/internal/api/v1/golden-record-fields")
public class GoldenRecordFieldsController {

    @GetMapping
    public List<GoldenRecordFieldView> list(
            @RequestParam(defaultValue = "PERSON") EntityType entityType
    ) {
        return RecordFieldCatalog.of(ContractAdminService.contractClass(entityType)).fields().stream()
                .map(GoldenRecordFieldView::fromDescriptor)
                .toList();
    }
}
