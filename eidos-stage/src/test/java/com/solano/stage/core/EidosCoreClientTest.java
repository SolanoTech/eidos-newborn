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

import com.solano.shared.dto.GoldenRecordDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class EidosCoreClientTest {

    private static final String BASE_URL = "http://core:8080";
    private static final String EXPECTED_URL = BASE_URL + "/api/v1/internal/golden-records";

    private MockRestServiceServer server;
    private EidosCoreClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new EidosCoreClient(builder, BASE_URL);
    }

    @Test
    void postsToCorrectEndpoint_onSuccess() {
        server.expect(requestTo(EXPECTED_URL))
                .andExpect(method(POST))
                .andRespond(withSuccess());

        assertThatCode(() -> client.saveGoldenRecord(record(), "standard", "ext-1"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void serverError_throwsEidosCoreException() {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"boom\"}"));

        assertThatThrownBy(() -> client.saveGoldenRecord(record(), "standard", "ext-1"))
                .isInstanceOf(EidosCoreException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void clientError_throwsEidosCoreException() {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("bad"));

        assertThatThrownBy(() -> client.saveGoldenRecord(record(), "standard", "ext-1"))
                .isInstanceOf(EidosCoreException.class)
                .hasMessageContaining("HTTP 400");
    }

    private static GoldenRecordDto record() {
        return GoldenRecordDto.builder()
                .grMobilePhoneMain("998901234567")
                .grFirstName("John")
                .grLastName("Doe")
                .grPinfl("12345678901234")
                .build();
    }
}
