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

package com.solano.eidoscore.web.dto;

import java.util.List;
import java.util.Map;

/**
 * Элемент очереди конфликтов для UI.
 *
 * @param conflicts для GREY_ZONE_CONFLICT — отличающиеся поля (текущее vs входящее)
 * @param snapshot  все непустые поля входящего снимка (для UNKNOWN_SOURCE — вся карточка)
 */
public record TentativeItemView(
        Long id,
        String reason,
        String grClientId,
        String sourceName,
        Integer sourceTrust,
        String personName,
        String createdAt,
        List<TentativeConflictField> conflicts,
        Map<String, String> snapshot
) {
}
