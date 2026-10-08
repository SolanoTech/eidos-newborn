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

import com.solano.eidoscore.entity.Source;

import java.time.LocalDateTime;

/**
 * Провенанс одного поля записи: какому источнику принадлежит текущее значение.
 * Реализуется JPA-сущностями field_meta каждого типа записи (физлица, юрлица);
 * движку безразлично, какая таблица за этим стоит.
 */
public interface FieldProvenance {

    String getFieldName();

    Source getSource();

    void setSource(Source source);

    void setUpdatedAt(LocalDateTime updatedAt);
}
