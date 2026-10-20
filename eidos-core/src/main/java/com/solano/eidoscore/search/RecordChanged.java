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

package com.solano.eidoscore.search;

import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.entity.legal.LegalEntityRecord;

/**
 * Запись изменилась и её надо привести в индекс.
 *
 * <p>Внутреннее событие приложения, не Kafka: публикуется внутри транзакции, а
 * обрабатывается после её коммита.</p>
 */
public sealed interface RecordChanged {

    record Client(GoldenRecord record) implements RecordChanged {
    }

    record Merchant(LegalEntityRecord record) implements RecordChanged {
    }
}
