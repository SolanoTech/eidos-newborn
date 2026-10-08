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

import java.util.List;
import java.util.Set;

/**
 * Доступ к полям записи по имени — реестр аксессоров типа записи.
 * Единственное место, где движок «знает» о конкретных полях; реализация
 * на тип держит карту имя поля → getter/setter в DTO и entity.
 *
 * @param <D> DTO входящего контракта
 * @param <E> JPA-сущность записи
 */
public interface RecordFieldAccess<D, E> {

    boolean knowsField(String fieldName);

    Set<String> knownFieldNames();

    void copyField(D dto, E entity, String fieldName);

    Object readFromDto(D dto, String fieldName);

    Object readFromEntity(E entity, String fieldName);

    /** Копирует все известные поля; возвращает их имена (нужно creator'у для field_meta). */
    List<String> copyToEntity(D dto, E entity);

    void copyEntityField(E from, E to, String fieldName);
}
