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

package com.solano.stage.service.admin;

import com.solano.stage.entity.registry.Source;
import com.solano.stage.exception.BadRequestException;
import com.solano.stage.exception.NotFoundException;
import com.solano.stage.repository.registry.SourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD источников в реестре. Вызывается внутренним admin API (UI-backend).
 */
@Service
public class SourceAdminService {

    private final SourceRepository sourceRepository;

    public SourceAdminService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public List<Source> findAll() {
        return sourceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Source findById(Long id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Source " + id + " not found"));
    }

    @Transactional
    public Source create(String code, String name, String token, Integer trustLevel, Boolean enabled) {
        if (sourceRepository.existsByCode(code)) {
            throw new BadRequestException("Source code '" + code + "' already exists");
        }
        if (sourceRepository.existsByToken(token)) {
            throw new BadRequestException("Token already in use");
        }
        Source source = Source.builder()
                .code(code)
                .name(name)
                .token(token)
                .trustLevel(trustLevel)
                .enabled(enabled == null ? Boolean.TRUE : enabled)
                .build();
        return sourceRepository.save(source);
    }

    @Transactional
    public Source update(Long id, String code, String name, String token, Integer trustLevel, Boolean enabled) {
        Source source = findById(id);
        if (!source.getCode().equals(code) && sourceRepository.existsByCode(code)) {
            throw new BadRequestException("Source code '" + code + "' already exists");
        }
        if (!source.getToken().equals(token) && sourceRepository.existsByToken(token)) {
            throw new BadRequestException("Token already in use");
        }
        source.setCode(code);
        source.setName(name);
        source.setToken(token);
        source.setTrustLevel(trustLevel);
        if (enabled != null) {
            source.setEnabled(enabled);
        }
        return sourceRepository.save(source);
    }

    @Transactional
    public void delete(Long id) {
        Source source = findById(id);
        sourceRepository.delete(source);
    }
}
