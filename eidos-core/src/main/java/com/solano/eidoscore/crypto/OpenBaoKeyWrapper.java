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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Заворачивание DEK в OpenBao через секретный движок transit.
 *
 * <p>Ключевое свойство: KEK из хранилища не выходит. Наружу доступны только две
 * операции — зашифровать и расшифровать переданный блоб, — поэтому в памяти
 * приложения KEK не существует ни на каком этапе.</p>
 */
@Component
public class OpenBaoKeyWrapper implements KeyWrapper {

    private static final String TOKEN_HEADER = "X-Vault-Token";

    private final RestClient openBaoRestClient;
    private final String token;
    private final String kekName;

    public OpenBaoKeyWrapper(
            RestClient openBaoRestClient,
            @Value("${eidos.crypto.openbao.token}") String token,
            @Value("${eidos.crypto.openbao.kek-name}") String kekName
    ) {
        this.openBaoRestClient = openBaoRestClient;
        this.token = token;
        this.kekName = kekName;
    }

    @Override
    public byte[] wrap(byte[] key) {
        Map<?, ?> body = call("/v1/transit/encrypt/" + kekName,
                Map.of("plaintext", Base64.getEncoder().encodeToString(key)));
        String ciphertext = (String) data(body).get("ciphertext");
        return ciphertext.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] unwrap(byte[] wrappedKey) {
        Map<?, ?> body = call("/v1/transit/decrypt/" + kekName,
                Map.of("ciphertext", new String(wrappedKey, StandardCharsets.UTF_8)));
        return Base64.getDecoder().decode((String) data(body).get("plaintext"));
    }

    @Override
    public String kekName() {
        return kekName;
    }

    private Map<?, ?> call(String uri, Map<String, String> payload) {
        try {
            return openBaoRestClient.post()
                    .uri(uri)
                    .header(TOKEN_HEADER, token)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            throw new KeyStoreUnavailableException(uri, e);
        }
    }

    private static Map<?, ?> data(Map<?, ?> body) {
        Object data = body == null ? null : body.get("data");
        if (!(data instanceof Map<?, ?> map)) {
            throw new KeyStoreUnavailableException("unexpected response shape", null);
        }
        return map;
    }

    /** Хранилище ключей недоступно: без него ни зашифровать, ни прочитать нельзя. */
    public static class KeyStoreUnavailableException extends RuntimeException {
        public KeyStoreUnavailableException(String detail, Throwable cause) {
            super("Key store operation failed: " + detail, cause);
        }
    }
}
