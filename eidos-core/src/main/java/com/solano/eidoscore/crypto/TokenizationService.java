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

import com.solano.eidoscore.entity.vault.PdDek;
import com.solano.eidoscore.entity.vault.PdToken;
import com.solano.eidoscore.repository.vault.PdDekRepository;
import com.solano.eidoscore.repository.vault.PdTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Замена персональных значений на токены и обратное раскрытие.
 *
 * <p>Токен непрозрачен и ничего не говорит о значении: восстановить исходное
 * можно только через это хранилище и только при доступном KEK.</p>
 *
 * <p>Ключ шифрования берётся один на владельца на прогон
 * ({@link #openBatch}) — так число обращений к хранилищу ключей не зависит от
 * числа полей, а раскрытие владельца остаётся одной операцией разворачивания.</p>
 *
 * <p>На этом этапе сервис ни с чем не связан: Золотую запись он не трогает.</p>
 */
@Service
public class TokenizationService {

    private static final String TOKEN_PREFIX = "tok_";
    private static final int TOKEN_BYTES = 16;

    private final PdDekRepository dekRepository;
    private final PdTokenRepository tokenRepository;
    private final DekCipher dekCipher;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TokenizationService(
            PdDekRepository dekRepository,
            PdTokenRepository tokenRepository,
            DekCipher dekCipher,
            Clock clock
    ) {
        this.dekRepository = dekRepository;
        this.tokenRepository = tokenRepository;
        this.dekCipher = dekCipher;
        this.clock = clock;
    }

    /**
     * Заводит ключ для одного прогона по владельцу. Полученный идентификатор
     * передаётся во все вызовы {@link #tokenize} этого прогона.
     */
    @Transactional
    public UUID openBatch(String ownerId) {
        PdDek dek = PdDek.builder()
                .dekId(UUID.randomUUID())
                .ownerId(ownerId)
                .wrappedKey(dekCipher.newWrappedKey())
                .kekName(dekCipher.kekName())
                .createdAt(LocalDateTime.now(clock))
                .build();
        dekRepository.save(dek);
        return dek.getDekId();
    }

    /** @return токен, который встанет в профильную таблицу вместо значения */
    @Transactional
    public String tokenize(UUID dekId, String ownerId, String fieldName, String value) {
        PdDek dek = dekRepository.findById(dekId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown DEK " + dekId));
        byte[] ciphertext = dekCipher.encrypt(
                dek.getWrappedKey(),
                value.getBytes(StandardCharsets.UTF_8),
                associatedData(ownerId, fieldName));

        PdToken token = PdToken.builder()
                .token(newToken())
                .dekId(dekId)
                .ownerId(ownerId)
                .fieldName(fieldName)
                .ciphertext(ciphertext)
                .createdAt(LocalDateTime.now(clock))
                .build();
        tokenRepository.save(token);
        return token.getToken();
    }

    /** Раскрывает значение по токену. */
    @Transactional(readOnly = true)
    public String detokenize(String token) {
        PdToken stored = tokenRepository.findById(token)
                .orElseThrow(() -> new UnknownTokenException(token));
        PdDek dek = dekRepository.findById(stored.getDekId())
                .orElseThrow(() -> new IllegalStateException("Missing DEK for token " + token));
        byte[] plaintext = dekCipher.decrypt(
                dek.getWrappedKey(),
                stored.getCiphertext(),
                associatedData(stored.getOwnerId(), stored.getFieldName()));
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    /** Токены владельца — для обратного перехода к открытым данным. */
    @Transactional(readOnly = true)
    public List<PdToken> tokensOf(String ownerId) {
        return tokenRepository.findByOwnerId(ownerId);
    }

    /**
     * Привязка шифротекста к месту: владелец и поле участвуют в аутентификации,
     * поэтому перенести шифротекст на другое поле или другого клиента нельзя.
     */
    private static byte[] associatedData(String ownerId, String fieldName) {
        return (ownerId + '|' + fieldName).getBytes(StandardCharsets.UTF_8);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return TOKEN_PREFIX + HexFormat.of().formatHex(bytes);
    }

    /** Токена нет в хранилище: значение раскрыть нельзя. */
    public static class UnknownTokenException extends RuntimeException {
        public UnknownTokenException(String token) {
            super("Unknown token " + token);
        }
    }
}
