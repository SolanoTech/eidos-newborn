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

package com.solano.eidoscdiuibackend.service;

import com.solano.eidoscdiuibackend.entity.AuditEvent;
import com.solano.eidoscdiuibackend.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Журнал аудита действий консоли. Запись никогда не роняет основную операцию:
 * ошибки аудита логируются и глотаются.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /** Записать событие от имени текущего аутентифицированного пользователя. */
    public void log(String action, String details, String category) {
        logAs(currentActor(), action, details, category);
    }

    @Transactional
    public void logAs(String actor, String action, String details, String category) {
        try {
            repository.save(AuditEvent.builder()
                    .id(UUID.randomUUID())
                    .actor(actor == null || actor.isBlank() ? "system" : actor)
                    .action(truncate(action, 256))
                    .details(truncate(details, 1024))
                    .category(category == null ? "ok" : category)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit event '{}': {}", action, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> latest(int limit) {
        int size = Math.min(Math.max(limit, 1), 500);
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size)).getContent();
    }

    private static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return "system";
        }
        return auth.getName();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
