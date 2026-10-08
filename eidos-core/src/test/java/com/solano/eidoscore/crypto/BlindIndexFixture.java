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

import com.solano.eidoscore.entity.vault.PdBlindIndexKey;
import com.solano.eidoscore.repository.vault.PdBlindIndexKeyRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Настоящий слепой индекс на подменённом хранилище ключей — для тестов, где
 * важно, что сервис ищет по тому же значению, которое сам и посчитал.
 */
public final class BlindIndexFixture {

    private BlindIndexFixture() {
    }

    public static BlindIndex inMemory(PdBlindIndexKeyRepository keyRepository) {
        Map<Integer, PdBlindIndexKey> keys = new HashMap<>();
        lenient().when(keyRepository.save(any(PdBlindIndexKey.class))).thenAnswer(i -> {
            PdBlindIndexKey k = i.getArgument(0);
            keys.put(k.getKeyVersion(), k);
            return k;
        });
        lenient().when(keyRepository.findFirstByOrderByKeyVersionDesc())
                .thenAnswer(i -> keys.values().stream().findFirst());
        Clock fixed = Clock.fixed(LocalDate.of(2026, 9, 28)
                .atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        return new BlindIndex(keyRepository, new FakeKeyWrapper(), fixed);
    }
}
