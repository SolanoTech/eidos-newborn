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

package com.solano.eidosgateway.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * JPA для общего реестра ({@code eidos_registry}) — read-only: {@code source},
 * {@code source_contract}, {@code source_field}. Владелец схемы — eidos-stage,
 * поэтому здесь {@code ddl-auto=none} (никаких DDL-операций). Репозитории пакета
 * {@code repository.registry} привязаны к этому EMF.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.solano.eidosgateway.repository.registry",
        entityManagerFactoryRef = "registryEntityManagerFactory",
        transactionManagerRef = "registryTransactionManager"
)
public class RegistryDataSourceConfig {

    @Bean
    public DataSource registryDataSource(
            @Value("${eidos.registry.datasource.url}") String url,
            @Value("${eidos.registry.datasource.username}") String username,
            @Value("${eidos.registry.datasource.password}") String password,
            @Value("${eidos.registry.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean registryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("registryDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.solano.eidosgateway.entity.registry")
                .persistenceUnit("registry")
                .properties(GatewayDataSourceConfig.hibernateProperties("none"))
                .build();
    }

    @Bean
    public PlatformTransactionManager registryTransactionManager(
            @Qualifier("registryEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
