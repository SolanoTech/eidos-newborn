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

package com.solano.stage.entity.registry;

/**
 * Тип сущности, к которой относится дата-контракт источника.
 *
 * <p>Источники этот тип <b>не присылают</b> — он определяется каналом, по
 * которому пришли данные (отдельный REST-эндпоинт и отдельный топик Kafka на
 * каждый тип). Во внутренних контрактах тип нужен, чтобы выбрать нужный
 * контракт источника, целевую Золотую запись и эндпоинт core.</p>
 */
public enum EntityType {

    /** Физическое лицо — клиентская карточка. */
    PERSON,

    /** Юридическое лицо — карточка мерчанта. */
    LEGAL_ENTITY
}
