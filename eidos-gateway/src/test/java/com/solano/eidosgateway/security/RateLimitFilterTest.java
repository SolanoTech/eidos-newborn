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

package com.solano.eidosgateway.security;

import com.solano.eidosgateway.entity.registry.Source;
import com.solano.eidosgateway.proxy.RouteResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static final String FUZZY = "/api/v1/external/golden-records/search/fuzzy";
    private static final String ACCURATE = "/api/v1/external/golden-records/search/accurate";

    private final RouteResolver resolver = new RouteResolver(
            "http://core:8080", "http://consents:8082", "http://stage:8081");
    private final AtomicLong clock = new AtomicLong(0);
    private final RateLimiter limiter = new RateLimiter(clock::get);

    private RateLimitFilter filter(int perMinute, int fuzzyPerMinute) {
        return new RateLimitFilter(resolver, limiter, true, perMinute, fuzzyPerMinute);
    }

    @Test
    void passesWhileWithinLimit() throws Exception {
        RateLimitFilter filter = filter(600, 3);
        for (int i = 0; i < 3; i++) {
            assertThat(call(filter, FUZZY, "bank-1").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void rejectsOverLimitWithRetryAfter() throws Exception {
        RateLimitFilter filter = filter(600, 2);
        call(filter, FUZZY, "bank-1");
        call(filter, FUZZY, "bank-1");

        MockHttpServletResponse response = call(filter, FUZZY, "bank-1");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    void refillsOverTime() throws Exception {
        RateLimitFilter filter = filter(600, 2);
        call(filter, FUZZY, "bank-1");
        call(filter, FUZZY, "bank-1");
        assertThat(call(filter, FUZZY, "bank-1").getStatus()).isEqualTo(429);

        clock.addAndGet(TimeUnit.MINUTES.toNanos(1));

        assertThat(call(filter, FUZZY, "bank-1").getStatus()).isEqualTo(200);
    }

    @Test
    void countsSourcesSeparately() throws Exception {
        RateLimitFilter filter = filter(600, 1);
        assertThat(call(filter, FUZZY, "bank-1").getStatus()).isEqualTo(200);
        assertThat(call(filter, FUZZY, "bank-1").getStatus()).isEqualTo(429);

        assertThat(call(filter, FUZZY, "bank-2").getStatus()).isEqualTo(200);
    }

    /** Нечёткий поиск не должен расходовать квоту приёма данных и наоборот. */
    @Test
    void countsFuzzySearchSeparatelyFromTheRestOfTheApi() throws Exception {
        RateLimitFilter filter = filter(600, 1);
        call(filter, FUZZY, "bank-1");
        assertThat(call(filter, FUZZY, "bank-1").getStatus()).isEqualTo(429);

        assertThat(call(filter, ACCURATE, "bank-1").getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresInternalPaths() throws Exception {
        RateLimitFilter filter = filter(1, 1);
        assertThat(call(filter, "/internal/api/v1/kafka-config", null).getStatus()).isEqualTo(200);
        assertThat(call(filter, "/internal/api/v1/kafka-config", null).getStatus()).isEqualTo(200);
    }

    @Test
    void disabledFilterLetsEverythingThrough() throws Exception {
        RateLimitFilter disabled = new RateLimitFilter(resolver, limiter, false, 1, 1);
        assertThat(call(disabled, FUZZY, "bank-1").getStatus()).isEqualTo(200);
        assertThat(call(disabled, FUZZY, "bank-1").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse call(RateLimitFilter filter, String path, String sourceCode)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (sourceCode != null) {
            Source source = new Source();
            source.setCode(sourceCode);
            request.setAttribute(SourceTokenAuthFilter.SOURCE_ATTRIBUTE, source);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, passThrough());
        return response;
    }

    private static FilterChain passThrough() {
        return new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request,
                                 jakarta.servlet.ServletResponse response) {
                ((HttpServletResponse) response).setStatus(200);
            }
        };
    }
}
