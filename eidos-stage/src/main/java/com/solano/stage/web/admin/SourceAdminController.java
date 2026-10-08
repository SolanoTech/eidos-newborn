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

import com.solano.stage.entity.registry.Source;
import com.solano.stage.service.admin.SourceAdminService;
import com.solano.stage.web.admin.dto.SourceRequest;
import com.solano.stage.web.admin.dto.SourceView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Внутренний admin API управления источниками (UI-backend). Защищён
 * {@code X-Admin-Token}. Источники хранятся в общем реестре.
 */
@RestController
@RequestMapping("/internal/api/v1/sources")
public class SourceAdminController {

    private final SourceAdminService sourceAdminService;

    public SourceAdminController(SourceAdminService sourceAdminService) {
        this.sourceAdminService = sourceAdminService;
    }

    @GetMapping
    public List<SourceView> list() {
        return sourceAdminService.findAll().stream().map(SourceView::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public SourceView get(@PathVariable Long id) {
        return SourceView.fromEntity(sourceAdminService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SourceView create(@Valid @RequestBody SourceRequest request) {
        Source created = sourceAdminService.create(
                request.getCode(), request.getName(), request.getToken(),
                request.getTrustLevel(), request.getEnabled());
        return SourceView.fromEntity(created);
    }

    @PutMapping("/{id}")
    public SourceView update(@PathVariable Long id, @Valid @RequestBody SourceRequest request) {
        Source updated = sourceAdminService.update(
                id, request.getCode(), request.getName(), request.getToken(),
                request.getTrustLevel(), request.getEnabled());
        return SourceView.fromEntity(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        sourceAdminService.delete(id);
    }
}
