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

package com.solano.eidoscore.service;

import com.solano.eidoscore.service.legal.LegalEntitySnapshotter;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регрессия на два дефекта, найденных живой проверкой обеих вертикалей.
 *
 * <p><b>1. Tentative от неизвестного источника терялся.</b> {@code save}-поток
 * пишет снимок и сразу бросает 404; в общей транзакции запись откатывалась
 * вместе с исключением, и очередь «неизвестный источник» всегда была пуста.
 * Лечится собственной транзакцией {@code REQUIRES_NEW} — здесь проверяется,
 * что аннотация на месте (поведение отката воспроизводится только с реальным
 * транзакционным менеджером).</p>
 *
 * <p><b>2. Архивная ревизия писалась со старой версией.</b> {@code @Version}
 * инкрементируется на flush, а не на {@code save}, поэтому архив получал номер
 * на единицу меньше. Лечится {@code saveAndFlush} перед архивированием —
 * проверяется в тестах merger'а.</p>
 */
class SnapshotDefectsTest {

    @Test
    void personTentativeFromDto_runsInItsOwnTransaction() throws Exception {
        assertRequiresNew(GoldenRecordSnapshotter.class, "tentativeFromDto");
    }

    @Test
    void legalTentativeFromDto_runsInItsOwnTransaction() throws Exception {
        assertRequiresNew(LegalEntitySnapshotter.class, "tentativeFromDto");
    }

    @Test
    void greyZoneTentative_staysInTheCallerTransaction() throws Exception {
        // Grey-zone снимок пишется внутри успешного merge — он обязан быть
        // атомарным с ним, поэтому REQUIRES_NEW здесь был бы ошибкой.
        assertJoinsCallerTransaction(GoldenRecordSnapshotter.class, "tentativeFromIncoming");
        assertJoinsCallerTransaction(LegalEntitySnapshotter.class, "tentativeFromIncoming");
    }

    private static void assertRequiresNew(Class<?> type, String methodName) throws Exception {
        Transactional tx = findMethod(type, methodName).getAnnotation(Transactional.class);
        assertThat(tx).as("%s.%s must be transactional", type.getSimpleName(), methodName).isNotNull();
        assertThat(tx.propagation())
                .as("%s.%s must commit independently of the failing caller", type.getSimpleName(), methodName)
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    private static void assertJoinsCallerTransaction(Class<?> type, String methodName) throws Exception {
        Transactional tx = findMethod(type, methodName).getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.propagation()).isEqualTo(Propagation.REQUIRED);
    }

    private static Method findMethod(Class<?> type, String name) {
        for (Method m : type.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new AssertionError("No method " + name + " on " + type.getSimpleName());
    }
}
