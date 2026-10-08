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

package com.solano.eidosgateway.proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProxyServiceTest {

    private MockRestServiceServer server;
    private ProxyService proxyService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        proxyService = new ProxyService(builder.build());
    }

    @Test
    void forwardsSuccessfulResponse_withStatusAndBody() {
        server.expect(requestTo("http://core:8080/api/v1/external/golden-records/search/accurate?pinfl=123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"GR_1\"}", MediaType.APPLICATION_JSON));

        ResponseEntity<byte[]> response = proxyService.forward(
                "http://core:8080/api/v1/external/golden-records/search/accurate?pinfl=123",
                HttpMethod.GET, new HttpHeaders(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).contains("GR_1");
    }

    @Test
    void passesThroughErrorStatus_withoutThrowing() {
        server.expect(requestTo("http://core:8080/api/v1/external/golden-records/search/accurate"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Golden Record Not Found\"}"));

        ResponseEntity<byte[]> response = proxyService.forward(
                "http://core:8080/api/v1/external/golden-records/search/accurate",
                HttpMethod.GET, new HttpHeaders(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).contains("Not Found");
    }

    @Test
    void forwardsPostBody() {
        server.expect(requestTo("http://stage:8081/api/v1/client-data"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"status\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        byte[] body = "{\"data\":{},\"source\":\"tieto\"}".getBytes(StandardCharsets.UTF_8);
        ResponseEntity<byte[]> response = proxyService.forward(
                "http://stage:8081/api/v1/client-data", HttpMethod.POST, new HttpHeaders(), body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).contains("SUCCESS");
    }
}
