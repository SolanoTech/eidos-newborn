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

package com.solano.eidosgateway.entity.registry;

/**
 * Логический тип поля источника в конструкторе дата-контрактов. Зеркало
 * перечисления, которым владеет eidos-stage; используется gateway для валидации.
 */
public enum FieldDataType {
    STRING,
    DATE,
    INTEGER,
    BOOLEAN,
    GENDER,

    /** Массив простых значений — переносится поддеревом JSON. */
    ARRAY,

    /** Массив структур — переносится поддеревом JSON. */
    STRUCT
}
