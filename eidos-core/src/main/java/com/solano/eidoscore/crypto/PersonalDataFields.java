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

package com.solano.eidoscore.crypto;

import java.util.List;

/**
 * Какие поля Золотой записи физлица считаются персональными и подлежат
 * обезличиванию, когда клиент теряет согласие на обработку данных.
 *
 * <p>Список намеренно собран в одном месте: он задаёт охват защиты, и менять
 * его нужно осознанно, а не по месту использования.</p>
 *
 * <p>В список входят прямые идентификаторы человека и его контактные данные.
 * Не входят:</p>
 *
 * <ul>
 *   <li><b>Даты</b> — рождения, выдачи и окончания документа, регистрации.
 *       Колонки типа {@code date}: замена на токен означала бы смену типа
 *       колонки и поломку всего, что работает с ними как с датами.</li>
 *   <li><b>Справочные значения</b> — пол, гражданство, национальность, страна,
 *       область, район и их коды. По отдельности человека не определяют, а в
 *       совокупности остаются квазиидентификаторами: это известное ограничение
 *       текущего охвата.</li>
 *   <li>{@code grClientId} — суррогатный ключ, персональными данными не
 *       является.</li>
 * </ul>
 */
public final class PersonalDataFields {

    /** Имена в терминах каталога полей — те же, что знает маппер записи. */
    public static final List<String> NAMES = List.of(
            "grLastName",
            "grFirstName",
            "grMiddleName",
            "grPinfl",
            "grDocPassData",
            "grDocIssuedBy",
            "grMobilePhoneMain",
            "grContactsEmail",
            "grBirthPlace",
            "grAddrPermanentAddress",
            "grAddrTemporaryAddress",
            "grAddrPermRegAddress",
            "grAddrTempRegAddress");

    private PersonalDataFields() {
    }
}
