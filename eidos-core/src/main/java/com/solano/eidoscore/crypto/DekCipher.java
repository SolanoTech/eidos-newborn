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

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.BinaryKeysetReader;
import com.google.crypto.tink.BinaryKeysetWriter;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.TinkProtoKeysetFormat;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;

/**
 * Работа с ключами шифрования данных: создание, заворачивание, шифрование.
 *
 * <p>Схема конвертная. Tink генерирует DEK, сам DEK немедленно заворачивается
 * KEK'ом из внешнего хранилища и дальше живёт только в таком виде. В открытом
 * виде ключ существует лишь в памяти на время операции — на диск в открытом
 * виде он не попадает никогда.</p>
 */
@Component
public class DekCipher {

    static {
        try {
            AeadConfig.register();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot initialise Tink AEAD", e);
        }
    }

    private final KeyWrapper keyWrapper;

    public DekCipher(KeyWrapper keyWrapper) {
        this.keyWrapper = keyWrapper;
    }

    /** Имя KEK, которым заворачиваются ключи, — попадает в запись хранилища. */
    public String kekName() {
        return keyWrapper.kekName();
    }

    /** Новый DEK, сразу завёрнутый KEK: наружу отдаётся только защищённая форма. */
    public byte[] newWrappedKey() {
        try {
            KeysetHandle handle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM);
            byte[] raw = TinkProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get());
            return keyWrapper.wrap(raw);
        } catch (GeneralSecurityException e) {
            throw new CryptoFailureException("Cannot generate data encryption key", e);
        }
    }

    /**
     * Шифрует значение ключом из завёрнутого DEK.
     *
     * @param associatedData связанные данные: не шифруются, но подмешиваются в
     *                       аутентификацию. Сюда идёт «владелец + поле», поэтому
     *                       шифротекст нельзя переставить на другое поле или
     *                       другого клиента — расшифровка просто не пройдёт.
     */
    public byte[] encrypt(byte[] wrappedKey, byte[] plaintext, byte[] associatedData) {
        try {
            return aead(wrappedKey).encrypt(plaintext, associatedData);
        } catch (GeneralSecurityException e) {
            throw new CryptoFailureException("Cannot encrypt value", e);
        }
    }

    public byte[] decrypt(byte[] wrappedKey, byte[] ciphertext, byte[] associatedData) {
        try {
            return aead(wrappedKey).decrypt(ciphertext, associatedData);
        } catch (GeneralSecurityException e) {
            throw new CryptoFailureException("Cannot decrypt value", e);
        }
    }

    private Aead aead(byte[] wrappedKey) throws GeneralSecurityException {
        byte[] raw = keyWrapper.unwrap(wrappedKey);
        KeysetHandle handle = TinkProtoKeysetFormat.parseKeyset(raw, InsecureSecretKeyAccess.get());
        return handle.getPrimitive(RegistryConfiguration.get(), Aead.class);
    }

    /** Криптооперация не удалась. Наружу не выносит ни ключей, ни значений. */
    public static class CryptoFailureException extends RuntimeException {
        public CryptoFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
