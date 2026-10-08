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

/**
 * Подменённое хранилище ключей для тестов: «заворачивание» — обратимое
 * преобразование, достаточное, чтобы проверить конвертную схему без поднятого
 * OpenBao. Криптостойкости здесь нет и не требуется.
 */
public class FakeKeyWrapper implements KeyWrapper {

    private static final byte MASK = 0x5A;

    @Override
    public byte[] wrap(byte[] key) {
        return mask(key);
    }

    @Override
    public byte[] unwrap(byte[] wrappedKey) {
        return mask(wrappedKey);
    }

    @Override
    public String kekName() {
        return "test-kek";
    }

    private static byte[] mask(byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (byte) (in[i] ^ MASK);
        }
        return out;
    }
}
