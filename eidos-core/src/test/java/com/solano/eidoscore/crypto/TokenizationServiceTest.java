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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Токенизация поверх конвертной схемы: токен непрозрачен, значение достаётся
 * только через хранилище, ключ один на прогон.
 */
@ExtendWith(MockitoExtension.class)
class TokenizationServiceTest {

    private static final String OWNER = "GR_7f4ea435-3e76-4a80-8c39-d35134090b0f";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 28, 10, 0);

    @Mock PdDekRepository dekRepository;
    @Mock PdTokenRepository tokenRepository;

    private final Map<UUID, PdDek> deks = new HashMap<>();
    private final Map<String, PdToken> tokens = new HashMap<>();

    private TokenizationService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new TokenizationService(dekRepository, tokenRepository,
                new DekCipher(new FakeKeyWrapper()), fixed);

        lenient().when(dekRepository.save(any(PdDek.class))).thenAnswer(i -> {
            PdDek d = i.getArgument(0);
            deks.put(d.getDekId(), d);
            return d;
        });
        lenient().when(tokenRepository.save(any(PdToken.class))).thenAnswer(i -> {
            PdToken t = i.getArgument(0);
            tokens.put(t.getToken(), t);
            return t;
        });
        lenient().when(dekRepository.findById(any())).thenAnswer(i -> Optional.ofNullable(deks.get(i.getArgument(0))));
        lenient().when(tokenRepository.findById(any())).thenAnswer(i -> Optional.ofNullable(tokens.get(i.getArgument(0))));
    }

    @Test
    void valueIsRecoverableThroughItsToken() {
        UUID batch = service.openBatch(OWNER);

        String token = service.tokenize(batch, OWNER, "gr_pinfl", "61157669271732");

        assertThat(service.detokenize(token)).isEqualTo("61157669271732");
    }

    @Test
    void tokenRevealsNothingAboutTheValue() {
        UUID batch = service.openBatch(OWNER);

        String token = service.tokenize(batch, OWNER, "gr_pinfl", "61157669271732");

        assertThat(token).startsWith("tok_").doesNotContain("61157669271732");
        assertThat(tokens.get(token).getCiphertext()).isNotNull();
    }

    @Test
    void sameValueTwiceGivesDifferentTokens() {
        UUID batch = service.openBatch(OWNER);

        String first = service.tokenize(batch, OWNER, "gr_pinfl", "61157669271732");
        String second = service.tokenize(batch, OWNER, "gr_pinfl", "61157669271732");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void oneKeyServesTheWholeBatch() {
        UUID batch = service.openBatch(OWNER);

        service.tokenize(batch, OWNER, "gr_pinfl", "61157669271732");
        service.tokenize(batch, OWNER, "gr_first_name", "Азиз");
        service.tokenize(batch, OWNER, "gr_mobile_phone_main", "998901234567");

        assertThat(deks).hasSize(1);
        assertThat(tokens.values()).allMatch(t -> t.getDekId().equals(batch));
    }

    @Test
    void unknownTokenIsRejected() {
        assertThatThrownBy(() -> service.detokenize("tok_deadbeef"))
                .isInstanceOf(TokenizationService.UnknownTokenException.class);
    }

    @Test
    void storedRecordKeepsTheOwnerAndField() {
        UUID batch = service.openBatch(OWNER);

        String token = service.tokenize(batch, OWNER, "gr_doc_pass_data", "AB1234567");

        PdToken stored = tokens.get(token);
        assertThat(stored.getOwnerId()).isEqualTo(OWNER);
        assertThat(stored.getFieldName()).isEqualTo("gr_doc_pass_data");
        assertThat(stored.getCreatedAt()).isEqualTo(NOW);
        assertThat(deks.get(batch).getKekName()).isEqualTo("test-kek");
    }
}
