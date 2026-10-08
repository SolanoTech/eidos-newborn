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

package com.solano.eidoscore.entity;

import com.solano.eidoscore.crypto.BlindIndex;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Держит слепой индекс Золотой записи в актуальном состоянии.
 *
 * <p>Единая точка пересчёта: и создание, и слияние проходят через сохранение
 * сущности, поэтому ни создатель, ни слиятель об индексе знать не обязаны.</p>
 *
 * <p>Точечный UPDATE состояния согласия слушатели не вызывает — и не должен:
 * индексируемых полей он не трогает.</p>
 *
 * <p>Индекс берётся отложенно, а не внедряется в конструктор: слушатель нужен
 * фабрике сущностей, а сама она нужна репозиторию, из которого индекс читает
 * ключ. Прямое внедрение замкнуло бы этот круг и не дало приложению стартовать.</p>
 */
@Component
public class BlindIndexListener {

    private final ObjectProvider<BlindIndex> blindIndexProvider;

    public BlindIndexListener(ObjectProvider<BlindIndex> blindIndexProvider) {
        this.blindIndexProvider = blindIndexProvider;
    }

    @PrePersist
    @PreUpdate
    public void refresh(GoldenRecord record) {
        if (record.isTokenized()) {
            // Значения заменены токенами: пересчёт по ним испортил бы индекс, и
            // обезличенная запись перестала бы находиться. Индекс посчитан от
            // открытых значений и таким остаётся.
            return;
        }
        BlindIndex blindIndex = blindIndexProvider.getObject();
        record.setPdBiIdentity(blindIndex.identity(
                record.getGrLastName(), record.getGrFirstName(),
                record.getGrBirthDate(), record.getGrPinfl()));
        record.setPdBiIdentityDoc(blindIndex.identityByDocument(
                record.getGrLastName(), record.getGrFirstName(),
                record.getGrBirthDate(), record.getGrDocPassData()));
        record.setPdBiPinfl(blindIndex.pinfl(record.getGrPinfl()));
        record.setPdBiKeyVersion(blindIndex.currentKeyVersion());
    }
}
