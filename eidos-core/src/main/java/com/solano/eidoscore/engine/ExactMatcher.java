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

package com.solano.eidoscore.engine;

import java.util.Optional;

/**
 * Точный матчинг по идентифицирующим полям DTO — стратегия типа записи:
 * физлица — ФИО+дата рождения+ПИНФЛ/паспорт, юрлица — ИНН.
 */
@FunctionalInterface
public interface ExactMatcher<D, E> {
    Optional<E> find(D dto);
}
