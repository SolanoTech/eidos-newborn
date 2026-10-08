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
 * Тип сущности дата-контракта источника — зеркало значения из реестра.
 *
 * <p>Источники этот тип не присылают: он определяется каналом приёма
 * (свой REST-эндпоинт и свой топик Kafka на каждый тип). Gateway использует
 * его, чтобы выбрать нужный контракт источника и путь во внутренний stage.</p>
 */
public enum EntityType {

    /** Физическое лицо — клиентская карточка. */
    PERSON,

    /** Юридическое лицо — карточка мерчанта. */
    LEGAL_ENTITY
}
