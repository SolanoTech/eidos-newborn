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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Доводит схему до состояния, которое {@code ddl-auto=update} обеспечить не
 * может: расширение {@code pg_trgm} и GIN-индекс под неточный поиск по
 * названию мерчанта.
 *
 * <p>Hibernate умеет создавать таблицы и обычные индексы, но не расширения и
 * не индексы с operator class. Раньше на этом уже обжигались (check-constraint
 * enum'а согласий не расширился сам), поэтому здесь DDL выполняется явно и
 * идемпотентно при старте.</p>
 *
 * <p>Когда появится Flyway — этот шаг переезжает в миграцию, а класс
 * удаляется.</p>
 */
@Component
public class LegalSearchIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegalSearchIndexInitializer.class);

    private static final String CREATE_EXTENSION = "CREATE EXTENSION IF NOT EXISTS pg_trgm";
    private static final String CREATE_INDEX = """
            CREATE INDEX IF NOT EXISTS ix_le_name_normalized_trgm
                ON legal_entity_record USING gin (name_normalized gin_trgm_ops)
            """;

    private final JdbcTemplate jdbcTemplate;

    public LegalSearchIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(CREATE_EXTENSION);
            jdbcTemplate.execute(CREATE_INDEX);
            log.info("Legal-entity fuzzy search ready: pg_trgm extension and GIN index in place");
        } catch (Exception e) {
            // Неточный поиск по названию не заработает, но остальной сервис
            // должен подняться — например, если у роли нет прав на CREATE EXTENSION.
            log.error("Could not prepare fuzzy search for legal entities: {}. "
                    + "Run manually: {} ; {}", e.getMessage(), CREATE_EXTENSION, CREATE_INDEX.strip());
        }
    }
}
