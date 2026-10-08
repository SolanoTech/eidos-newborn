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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Доводит собственную схему Gateway до состояния, которое {@code ddl-auto=update}
 * обеспечить не может: добавляет {@code kafka_config.entity_type}.
 *
 * <p>Hibernate пытается создать колонку сразу {@code NOT NULL} и на таблице с
 * существующей конфигурацией падает — колонка не появляется вовсе, а следом
 * рушится старт консьюмера. Поэтому колонку добавляем сами: сначала nullable,
 * потом backfill (всё, что было, — каналы физлиц), потом {@code NOT NULL}.</p>
 *
 * <p>Выполняется как {@link ApplicationRunner}, то есть до
 * {@code ApplicationReadyEvent}, на котором поднимаются Kafka-консьюмеры.</p>
 */
@Component
@Order(0)
public class GatewaySchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GatewaySchemaInitializer.class);

    private static final String ADD_COLUMN = """
            ALTER TABLE kafka_config ADD COLUMN IF NOT EXISTS entity_type varchar(32)
            """;

    private static final String BACKFILL = """
            UPDATE kafka_config SET entity_type = 'PERSON' WHERE entity_type IS NULL
            """;

    private static final String ENFORCE_NOT_NULL = """
            ALTER TABLE kafka_config ALTER COLUMN entity_type SET NOT NULL
            """;

    private static final String ADD_UNIQUE = """
            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'uk_kafka_config_per_entity_type'
                ) THEN
                    ALTER TABLE kafka_config
                        ADD CONSTRAINT uk_kafka_config_per_entity_type UNIQUE (entity_type);
                END IF;
            END $$
            """;

    private final JdbcTemplate jdbcTemplate;

    public GatewaySchemaInitializer(@Qualifier("gatewayDataSource") DataSource gatewayDataSource) {
        this.jdbcTemplate = new JdbcTemplate(gatewayDataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(ADD_COLUMN);
            jdbcTemplate.execute(BACKFILL);
            jdbcTemplate.execute(ENFORCE_NOT_NULL);
            jdbcTemplate.execute(ADD_UNIQUE);
            log.info("Gateway schema ready: kafka_config has one channel per entity type");
        } catch (Exception e) {
            log.error("Could not migrate kafka_config: {}. Kafka consumers may not start.", e.getMessage());
        }
    }
}
