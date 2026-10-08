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
 * Заворачивание и разворачивание ключей шифрования данных.
 *
 * <p>Отдельный интерфейс, чтобы хранилище ключей было заменяемо и чтобы
 * криптографию можно было тестировать без поднятого OpenBao. Реализация обязана
 * не выпускать сам KEK наружу: сюда приходят только DEK.</p>
 */
public interface KeyWrapper {

    /** @return завёрнутый ключ в виде, пригодном для хранения в БД */
    byte[] wrap(byte[] key);

    byte[] unwrap(byte[] wrappedKey);

    /** Имя KEK, которым завёрнут ключ, — попадает в запись хранилища. */
    String kekName();
}
