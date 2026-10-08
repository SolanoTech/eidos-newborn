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

package com.solano.stage.core;

/**
 * Ошибка при обращении к eidos-core (не-2xx ответ или сетевой сбой).
 * Pipeline трактует её как невосстановимую для данного сообщения и отправляет
 * сообщение в dead-letter топик.
 */
public class EidosCoreException extends RuntimeException {

    public EidosCoreException(String message) {
        super(message);
    }

    public EidosCoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
