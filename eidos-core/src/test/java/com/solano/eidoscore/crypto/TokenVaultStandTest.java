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

import com.solano.eidoscore.entity.vault.PdToken;
import com.solano.eidoscore.repository.vault.PdTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверка криптоконтура на поднятом стенде: настоящий OpenBao, настоящая
 * схема vault. Юнит-тесты рядом проверяют ту же логику на подменённом
 * хранилище ключей — здесь важно, что связка работает целиком.
 *
 * <p>По умолчанию пропускается: нужны запущенные postgres, kafka и openbao.
 * Запуск:</p>
 *
 * <pre>
 * ./mvnw test -Dtest=TokenVaultStandTest -Deidos.stand=true \
 *   -Dspring.datasource.url=jdbc:postgresql://localhost:5433/eidos_core \
 *   -Dspring.kafka.bootstrap-servers=localhost:19092 \
 *   -Deidos.consents.base-url=http://localhost:8086
 * </pre>
 *
 * <p>Порты не совпадают с настройками по умолчанию: на стенде postgres отдан
 * наружу как 5433, а consents — как 8086, потому что 5432 и 8082 заняты.</p>
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "eidos.stand", matches = "true")
class TokenVaultStandTest {

    private static final String PINFL = "61157669271732";

    @Autowired TokenizationService tokenizationService;
    @Autowired PdTokenRepository tokenRepository;

    @Test
    void roundTripThroughTheRealKeyStore() {
        String owner = "GR_" + UUID.randomUUID();
        UUID batch = tokenizationService.openBatch(owner);

        String token = tokenizationService.tokenize(batch, owner, "gr_pinfl", PINFL);

        // Токен непрозрачен и в нём нет исходного значения.
        assertThat(token).startsWith("tok_").doesNotContain(PINFL);

        // В хранилище лежит шифротекст, а не значение.
        PdToken stored = tokenRepository.findById(token).orElseThrow();
        assertThat(new String(stored.getCiphertext(), StandardCharsets.ISO_8859_1))
                .doesNotContain(PINFL);

        // И при этом значение восстанавливается.
        assertThat(tokenizationService.detokenize(token)).isEqualTo(PINFL);
    }

    @Test
    void oneKeyCoversEveryFieldOfTheOwner() {
        String owner = "GR_" + UUID.randomUUID();
        UUID batch = tokenizationService.openBatch(owner);

        tokenizationService.tokenize(batch, owner, "gr_pinfl", PINFL);
        tokenizationService.tokenize(batch, owner, "gr_first_name", "Азиз");
        tokenizationService.tokenize(batch, owner, "gr_mobile_phone_main", "998901234567");

        List<PdToken> tokens = tokenizationService.tokensOf(owner);
        assertThat(tokens).hasSize(3);
        assertThat(tokens).allMatch(t -> t.getDekId().equals(batch));
        assertThat(tokenizationService.detokenize(tokens.get(0).getToken())).isNotBlank();
    }
}
