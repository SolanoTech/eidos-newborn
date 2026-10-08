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

package com.solano.eidoscore.consent;

import java.util.List;
import java.util.UUID;

/**
 * Идентификатор Золотой записи в двух формах.
 *
 * <p>Создатель выдаёт {@code GR_<uuid>}, а сервис согласий хранит клиента как
 * колонку {@code uuid} — без префикса. В базе стенда живут обе формы: записи,
 * созданные приёмом, и пара строк из сидов. Поэтому по идентификатору из
 * события ищем оба варианта.</p>
 */
public final class GoldenRecordIds {

    private static final String PREFIX = "GR_";

    private GoldenRecordIds() {
    }

    /** Кандидаты на идентификатор Золотой записи, в порядке вероятности. */
    public static List<String> candidates(UUID clientUuid) {
        String bare = clientUuid.toString();
        return List.of(PREFIX + bare, bare);
    }
}
