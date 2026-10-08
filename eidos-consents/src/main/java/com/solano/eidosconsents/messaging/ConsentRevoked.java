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

package com.solano.eidosconsents.messaging;

import java.time.LocalDate;

/**
 * Доменное событие: действовавшее согласие отозвано в день {@code revokedOn}.
 *
 * <p>Внутреннее, в Kafka как есть не уходит. {@code expired} — готовое
 * событие с датами отозванного согласия; оно публикуется, только если после
 * коммита у клиента не осталось другого действующего согласия того же типа
 * (см. {@link ConsentRevocationListener}).</p>
 */
public record ConsentRevoked(ConsentEvent expired, LocalDate revokedOn) {
}
