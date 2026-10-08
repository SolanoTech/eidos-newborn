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

package com.solano.eidosgateway.kafka;

import com.solano.eidosgateway.auth.TokenAuthService;
import com.solano.eidosgateway.entity.registry.EntityType;
import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.entity.registry.SourceContract;
import com.solano.eidosgateway.exception.ContractValidationException;
import com.solano.eidosgateway.exception.UnauthorizedException;
import com.solano.eidosgateway.forward.StageClient;
import com.solano.eidosgateway.repository.registry.SourceContractRepository;
import com.solano.eidosgateway.service.contract.ContractValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayMessageHandlerTest {

    @Mock TokenAuthService tokenAuthService;
    @Mock SourceContractRepository contractRepository;
    @Mock ContractValidator contractValidator;
    @Mock StageClient stageClient;
    @Mock com.solano.eidosgateway.service.stats.RejectStatsService rejectStats;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private GatewayMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GatewayMessageHandler(
                tokenAuthService, contractRepository, contractValidator, objectMapper, stageClient, rejectStats);
    }

    @Test
    void validTokenAndData_forwardsToStage() {
        byte[] payload = "{\"data\":{\"phone\":\"998901234567\"},\"source\":\"tieto\"}".getBytes(StandardCharsets.UTF_8);
        when(tokenAuthService.authorize("tok-1")).thenReturn(source("tieto"));
        when(contractRepository.findBySourceCodeAndEntityType("tieto", EntityType.PERSON)).thenReturn(Optional.of(new SourceContract()));
        when(contractValidator.validate(any(), any())).thenReturn(List.of());

        handler.handle("tok-1", payload);

        verify(stageClient).forwardClientData(payload);
    }

    @Test
    void invalidToken_doesNotForward() {
        when(tokenAuthService.authorize("bad")).thenThrow(new UnauthorizedException("Invalid access token"));

        assertThatThrownBy(() -> handler.handle("bad", new byte[]{'{', '}'}))
                .isInstanceOf(UnauthorizedException.class);

        verify(stageClient, never()).forwardClientData(any());
    }

    @Test
    void contractViolations_doesNotForward() {
        byte[] payload = "{\"data\":{},\"source\":\"tieto\"}".getBytes(StandardCharsets.UTF_8);
        when(tokenAuthService.authorize("tok-1")).thenReturn(source("tieto"));
        when(contractRepository.findBySourceCodeAndEntityType("tieto", EntityType.PERSON)).thenReturn(Optional.of(new SourceContract()));
        when(contractValidator.validate(any(), any())).thenReturn(List.of("missing required field: phone"));

        assertThatThrownBy(() -> handler.handle("tok-1", payload))
                .isInstanceOf(ContractValidationException.class);

        verify(stageClient, never()).forwardClientData(any());
    }

    @Test
    void noContract_doesNotForward() {
        byte[] payload = "{\"data\":{},\"source\":\"tieto\"}".getBytes(StandardCharsets.UTF_8);
        when(tokenAuthService.authorize("tok-1")).thenReturn(source("tieto"));
        when(contractRepository.findBySourceCodeAndEntityType("tieto", EntityType.PERSON)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle("tok-1", payload))
                .isInstanceOf(ContractValidationException.class);

        verify(stageClient, never()).forwardClientData(any());
    }

    private static Source source(String code) {
        Source source = new Source();
        source.setId(1L);
        source.setCode(code);
        source.setToken("tok-1");
        return source;
    }
}
