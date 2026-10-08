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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteResolverTest {

    private final RouteResolver resolver = new RouteResolver(
            "http://core:8080", "http://consents:8082", "http://stage:8081");

    @Test
    void routesGoldenRecordsToCore_withoutRewrite() {
        var route = resolver.resolve("/api/v1/external/golden-records/search/accurate").orElseThrow();
        assertThat(route.targetBaseUrl()).isEqualTo("http://core:8080");
        assertThat(route.targetPath()).isEqualTo("/api/v1/external/golden-records/search/accurate");
        assertThat(route.fullUri("firstName=John")).isEqualTo(
                "http://core:8080/api/v1/external/golden-records/search/accurate?firstName=John");
    }

    @Test
    void routesConsentsToConsents_withInternalRewrite() {
        var route = resolver.resolve("/api/v1/consents/active/abc").orElseThrow();
        assertThat(route.targetBaseUrl()).isEqualTo("http://consents:8082");
        assertThat(route.targetPath()).isEqualTo("/internal/api/v1/consents/active/abc");
    }

    @Test
    void routesConsentsRoot_withInternalRewrite() {
        var route = resolver.resolve("/api/v1/consents").orElseThrow();
        assertThat(route.targetPath()).isEqualTo("/internal/api/v1/consents");
    }

    @Test
    void routesClientDataToStage() {
        var route = resolver.resolve("/api/v1/client-data").orElseThrow();
        assertThat(route.targetBaseUrl()).isEqualTo("http://stage:8081");
        assertThat(route.targetPath()).isEqualTo("/api/v1/client-data");
    }

    @Test
    void unknownPath_resolvesEmpty_andIsNotExternalRoute() {
        assertThat(resolver.resolve("/something/else")).isEmpty();
        assertThat(resolver.isExternalRoute("/something/else")).isFalse();
        assertThat(resolver.isExternalRoute("/internal/api/v1/sources")).isFalse();
        assertThat(resolver.isExternalRoute("/api/v1/consents/active/x")).isTrue();
    }
}
