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

package com.solano.eidoscdiuibackend.web.dto;

import com.solano.eidoscdiuibackend.entity.AuditEvent;

import java.util.UUID;

/** Событие аудита для таймлайна консоли. */
public record AuditEventView(
        UUID id,
        String actor,
        String action,
        String details,
        String category,
        String createdAt
) {
    public static AuditEventView of(AuditEvent e) {
        return new AuditEventView(
                e.getId(), e.getActor(), e.getAction(), e.getDetails(), e.getCategory(),
                e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
    }
}
