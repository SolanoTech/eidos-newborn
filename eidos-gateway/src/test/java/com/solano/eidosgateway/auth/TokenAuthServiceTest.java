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

package com.solano.eidosgateway.auth;

import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.exception.UnauthorizedException;
import com.solano.eidosgateway.repository.registry.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TokenAuthServiceTest {

    @Mock
    SourceRepository sourceRepository;

    private TokenAuthService service;

    @BeforeEach
    void setUp() {
        service = new TokenAuthService(sourceRepository);
    }

    @Test
    void validToken_returnsSource() {
        Source source = new Source();
        source.setId(1L);
        source.setCode("tieto");
        source.setToken("tok-1");
        lenient().when(sourceRepository.findByToken("tok-1")).thenReturn(Optional.of(source));

        assertThat(service.authorize("tok-1")).isSameAs(source);
    }

    @Test
    void unknownToken_throwsUnauthorized() {
        lenient().when(sourceRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authorize("nope"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void blankToken_throwsUnauthorized() {
        assertThatThrownBy(() -> service.authorize("  ")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.authorize(null)).isInstanceOf(UnauthorizedException.class);
    }
}
