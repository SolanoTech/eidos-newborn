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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Конвертная схема: ключ хранится завёрнутым, значение шифруется им. */
class DekCipherTest {

    private final DekCipher cipher = new DekCipher(new FakeKeyWrapper());

    private static final byte[] AD = "GR_1|gr_pinfl".getBytes(StandardCharsets.UTF_8);

    @Test
    void valueSurvivesRoundTrip() {
        byte[] key = cipher.newWrappedKey();
        byte[] plaintext = "61157669271732".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = cipher.encrypt(key, plaintext, AD);

        assertThat(new String(cipher.decrypt(key, ciphertext, AD), StandardCharsets.UTF_8))
                .isEqualTo("61157669271732");
    }

    @Test
    void ciphertextDoesNotLeakThePlaintext() {
        byte[] key = cipher.newWrappedKey();
        byte[] ciphertext = cipher.encrypt(key, "61157669271732".getBytes(StandardCharsets.UTF_8), AD);

        assertThat(new String(ciphertext, StandardCharsets.ISO_8859_1)).doesNotContain("61157669271732");
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        // AES-GCM со случайным одноразовым числом: одинаковые значения дают
        // разные шифротексты, поэтому по ним нельзя сопоставлять клиентов.
        byte[] key = cipher.newWrappedKey();
        byte[] first = cipher.encrypt(key, "998901234567".getBytes(StandardCharsets.UTF_8), AD);
        byte[] second = cipher.encrypt(key, "998901234567".getBytes(StandardCharsets.UTF_8), AD);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void ciphertextCannotBeMovedToAnotherField() {
        byte[] key = cipher.newWrappedKey();
        byte[] ciphertext = cipher.encrypt(key, "61157669271732".getBytes(StandardCharsets.UTF_8), AD);
        byte[] otherField = "GR_1|gr_doc_pass_data".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> cipher.decrypt(key, ciphertext, otherField))
                .isInstanceOf(DekCipher.CryptoFailureException.class);
    }

    @Test
    void anotherKeyCannotRead() {
        byte[] key = cipher.newWrappedKey();
        byte[] otherKey = cipher.newWrappedKey();
        byte[] ciphertext = cipher.encrypt(key, "секрет".getBytes(StandardCharsets.UTF_8), AD);

        assertThatThrownBy(() -> cipher.decrypt(otherKey, ciphertext, AD))
                .isInstanceOf(DekCipher.CryptoFailureException.class);
    }

    @Test
    void everyKeyIsDistinct() {
        assertThat(cipher.newWrappedKey()).isNotEqualTo(cipher.newWrappedKey());
    }
}
