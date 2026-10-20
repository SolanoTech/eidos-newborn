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

package com.solano.eidoscore.config;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.opensearch.client.json.jackson3.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Клиент OpenSearch для нечёткого поиска.
 *
 * <p>Кластер поднят с включённым плагином безопасности, поэтому разговор идёт
 * по HTTPS с базовой аутентификацией.</p>
 */
@Configuration
public class OpenSearchConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchConfig.class);

    @Bean
    public OpenSearchClient openSearchClient(
            @Value("${eidos.search.host}") String host,
            @Value("${eidos.search.port}") int port,
            @Value("${eidos.search.scheme}") String scheme,
            @Value("${eidos.search.username}") String username,
            @Value("${eidos.search.password}") String password,
            @Value("${eidos.search.trust-self-signed}") boolean trustSelfSigned
    ) throws Exception {
        HttpHost httpHost = new HttpHost(scheme, host, port);

        BasicCredentialsProvider credentials = new BasicCredentialsProvider();
        credentials.setCredentials(
                new AuthScope(httpHost),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        var transport = ApacheHttpClient5TransportBuilder.builder(httpHost)
                .setMapper(new JacksonJsonpMapper())
                .setHttpClientConfigCallback(builder -> {
                    builder.setDefaultCredentialsProvider(credentials);
                    if (URIScheme.HTTPS.id.equals(scheme) && trustSelfSigned) {
                        // Кластер на стенде поднят с демонстрационными
                        // сертификатами: они самоподписанные, и проверка цепочки
                        // на них не проходит. Для среды выше стенда нужен
                        // настоящий сертификат, а этот ключ — выключенным.
                        log.warn("OpenSearch TLS certificate is not verified "
                                + "(eidos.search.trust-self-signed=true) — stand configuration only");
                        try {
                            builder.setConnectionManager(
                                    PoolingAsyncClientConnectionManagerBuilder.create()
                                            .setTlsStrategy(ClientTlsStrategyBuilder.create()
                                                    .setSslContext(SSLContextBuilder.create()
                                                            .loadTrustMaterial(null, (chain, authType) -> true)
                                                            .build())
                                                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                                    .build())
                                            .build());
                        } catch (Exception e) {
                            throw new IllegalStateException("Cannot relax OpenSearch TLS verification", e);
                        }
                    }
                    return builder;
                })
                .build();

        return new OpenSearchClient(transport);
    }
}
