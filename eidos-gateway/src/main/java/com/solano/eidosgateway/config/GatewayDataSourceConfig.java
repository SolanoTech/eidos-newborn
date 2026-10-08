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
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA для собственной БД Gateway ({@code gateway}) — хранит {@code kafka_config}.
 * Помечена {@link Primary}, чтобы дефолтные {@code @Transactional} и репозитории
 * пакета {@code repository.gateway} использовали именно её. Эта БД — владелец
 * своей схемы ({@code ddl-auto=update}).
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.solano.eidosgateway.repository.gateway",
        entityManagerFactoryRef = "gatewayEntityManagerFactory",
        transactionManagerRef = "gatewayTransactionManager"
)
public class GatewayDataSourceConfig {

    @Bean
    @Primary
    public DataSource gatewayDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName) {
        // Явная сборка: для Hikari url биндится в jdbcUrl только через .url(...),
        // а не через @ConfigurationProperties (у HikariDataSource нет setUrl).
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean gatewayEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("gatewayDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.solano.eidosgateway.entity.gateway")
                .persistenceUnit("gateway")
                .properties(hibernateProperties("update"))
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager gatewayTransactionManager(
            @Qualifier("gatewayEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    static Map<String, Object> hibernateProperties(String ddlAuto) {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        return props;
    }
}
