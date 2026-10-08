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

package com.solano.eidosgateway.web;

import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.exception.ContractValidationException;
import com.solano.eidosgateway.proxy.ProxyService;
import com.solano.eidosgateway.proxy.RouteResolver;
import com.solano.eidosgateway.security.SourceTokenAuthFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Внешний интерфейс согласий: источник берётся из токена, а идентификатор
 * клиента принимается в том же виде, в каком его отдают внешние API Золотой
 * записи. Сама бизнес-логика согласий живёт в eidos-consents и здесь не
 * проверяется — контроллер только правит запрос и проксирует.
 */
@ExtendWith(MockitoExtension.class)
class ConsentControllerTest {

    private static final String UUID_STR = "3f1b2c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d";

    @Mock ProxyService proxyService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RouteResolver routeResolver = new RouteResolver(
            "http://core:8080", "http://consents:8082", "http://stage:8081");

    private ConsentController controller;

    @BeforeEach
    void setUp() {
        controller = new ConsentController(objectMapper, routeResolver, proxyService);
    }

    @Test
    void create_replacesSourceNameWithAuthorizedSource() {
        // Источник пытается записать согласие от чужого имени.
        byte[] body = json("""
                {"type":"pd","client_uuid":"%s","initial_date":"2026-01-01","source_name":"tieto"}
                """.formatted(UUID_STR));

        controller.create(request("payme"), body);

        Map<String, Object> forwarded = capturedBody();
        assertThat(forwarded).containsEntry("source_name", "payme");
        assertThat(forwarded).containsEntry("type", "pd");
        assertThat(forwarded).containsEntry("initial_date", "2026-01-01");
    }

    @Test
    void create_acceptsGoldenRecordIdWithPrefix_andStripsIt() {
        byte[] body = json("""
                {"type":"pd","client_uuid":"GR_%s","initial_date":"2026-01-01"}
                """.formatted(UUID_STR));

        controller.create(request("payme"), body);

        assertThat(capturedBody()).containsEntry("client_uuid", UUID_STR);
    }

    @Test
    void create_acceptsBareUuidUnchanged_andRoutesToConsents() {
        byte[] body = json("""
                {"type":"pd","client_uuid":"%s","initial_date":"2026-01-01"}
                """.formatted(UUID_STR));

        controller.create(request("payme"), body);

        assertThat(capturedBody()).containsEntry("client_uuid", UUID_STR);
        verify(proxyService).forward(
                eq("http://consents:8082/internal/api/v1/consents"),
                eq(HttpMethod.POST), any(HttpHeaders.class), any());
    }

    @Test
    void create_rejectsIdentifierThatIsNotAGoldenRecordId() {
        byte[] body = json("""
                {"type":"pd","client_uuid":"LT-42","initial_date":"2026-01-01"}
                """);

        assertThatThrownBy(() -> controller.create(request("payme"), body))
                .isInstanceOf(ContractValidationException.class)
                .hasMessageContaining("client_uuid");
    }

    @Test
    void create_rejectsMalformedBody() {
        assertThatThrownBy(() -> controller.create(request("payme"), json("not json at all")))
                .isInstanceOf(ContractValidationException.class)
                .hasMessageContaining("Malformed consent body");
    }

    @Test
    void byTypes_forwardsToClientSummary_withPrefixStripped() {
        controller.byTypes(request("payme"), "GR_" + UUID_STR);

        verify(proxyService).forward(
                eq("http://consents:8082/internal/api/v1/consents/client/" + UUID_STR),
                eq(HttpMethod.GET), any(HttpHeaders.class), eq(null));
    }

    @Test
    void activeOfType_keepsQueryString() {
        MockHttpServletRequest request = request("payme");
        request.setQueryString("type=PERSONAL_DATA");

        controller.activeOfType(request, UUID_STR);

        verify(proxyService).forward(
                eq("http://consents:8082/internal/api/v1/consents/active/" + UUID_STR
                        + "?type=PERSONAL_DATA"),
                eq(HttpMethod.GET), any(HttpHeaders.class), eq(null));
    }

    // --- вспомогательное ---

    private Map<String, Object> capturedBody() {
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(proxyService).forward(any(), any(), any(), captor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(captor.getValue(), Map.class);
        return payload;
    }

    private static byte[] json(String raw) {
        return raw.getBytes(StandardCharsets.UTF_8);
    }

    private static MockHttpServletRequest request(String sourceCode) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Source source = new Source();
        source.setCode(sourceCode);
        request.setAttribute(SourceTokenAuthFilter.SOURCE_ATTRIBUTE, source);
        return request;
    }
}
