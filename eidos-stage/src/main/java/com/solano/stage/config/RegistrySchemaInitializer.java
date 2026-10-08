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

package com.solano.stage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Доводит схему реестра до состояния, которое {@code ddl-auto=update} обеспечить
 * не может: снимает старое ограничение «один контракт на источник» и ставит
 * новое — «один контракт на источник и тип сущности».
 *
 * <p>Hibernate добавляет колонки и таблицы, но не удаляет и не пересоздаёт
 * существующие constraints: на этом уже обжигались (check-constraint enum'а
 * согласий не расширился сам). Поэтому DDL выполняется явно и идемпотентно.</p>
 *
 * <p>Когда появится Flyway — шаг переезжает в миграцию, а класс удаляется.</p>
 */
@Component
public class RegistrySchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RegistrySchemaInitializer.class);

    /**
     * Колонку добавляем сами: Hibernate пытается создать её сразу
     * {@code NOT NULL} и на непустой таблице падает — сначала нужен backfill.
     */
    private static final String ADD_ENTITY_TYPE_COLUMN = """
            ALTER TABLE source_contract ADD COLUMN IF NOT EXISTS entity_type varchar(32)
            """;

    /** Существующие строки появились до разделения по типам — это контракты физлиц. */
    private static final String BACKFILL_ENTITY_TYPE = """
            UPDATE source_contract SET entity_type = 'PERSON' WHERE entity_type IS NULL
            """;

    private static final String ENFORCE_NOT_NULL = """
            ALTER TABLE source_contract ALTER COLUMN entity_type SET NOT NULL
            """;

    /**
     * Старое ограничение уникальности по source_id (имя сгенерировано Hibernate,
     * поэтому ищем его по составу колонок) мешает второму контракту источника.
     */
    private static final String DROP_LEGACY_UNIQUE = """
            DO $$
            DECLARE constraint_name text;
            BEGIN
                SELECT con.conname INTO constraint_name
                FROM pg_constraint con
                JOIN pg_class rel ON rel.oid = con.conrelid
                WHERE rel.relname = 'source_contract'
                  AND con.contype = 'u'
                  AND (SELECT count(*) FROM unnest(con.conkey)) = 1
                  AND con.conkey[1] = (
                        SELECT attnum FROM pg_attribute
                        WHERE attrelid = rel.oid AND attname = 'source_id')
                LIMIT 1;
                IF constraint_name IS NOT NULL THEN
                    EXECUTE format('ALTER TABLE source_contract DROP CONSTRAINT %I', constraint_name);
                END IF;
            END $$
            """;

    private static final String ADD_COMPOSITE_UNIQUE = """
            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'uk_contract_per_source_and_type'
                ) THEN
                    ALTER TABLE source_contract
                        ADD CONSTRAINT uk_contract_per_source_and_type UNIQUE (source_id, entity_type);
                END IF;
            END $$
            """;

    /**
     * Check-constraint enum'а {@code data_type} перечисляет значения поимённо и
     * при расширении enum'а не обновляется сам — ровно та грабля, на которой мы
     * уже спотыкались с типами согласий. Пересоздаём его под актуальный набор,
     * иначе поля типов ARRAY/STRUCT не сохранить.
     */
    private static final String REFRESH_DATA_TYPE_CHECK = """
            DO $$
            DECLARE constraint_name text;
            BEGIN
                SELECT con.conname INTO constraint_name
                FROM pg_constraint con
                JOIN pg_class rel ON rel.oid = con.conrelid
                WHERE rel.relname = 'source_field' AND con.contype = 'c'
                  AND pg_get_constraintdef(con.oid) LIKE '%data_type%'
                LIMIT 1;
                IF constraint_name IS NOT NULL THEN
                    EXECUTE format('ALTER TABLE source_field DROP CONSTRAINT %I', constraint_name);
                END IF;
                ALTER TABLE source_field ADD CONSTRAINT source_field_data_type_check
                    CHECK (data_type IN ('STRING','DATE','INTEGER','BOOLEAN','GENDER','ARRAY','STRUCT'));
            END $$
            """;

    private final JdbcTemplate jdbcTemplate;

    public RegistrySchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(ADD_ENTITY_TYPE_COLUMN);
            jdbcTemplate.execute(BACKFILL_ENTITY_TYPE);
            jdbcTemplate.execute(ENFORCE_NOT_NULL);
            jdbcTemplate.execute(DROP_LEGACY_UNIQUE);
            jdbcTemplate.execute(ADD_COMPOSITE_UNIQUE);
            jdbcTemplate.execute(REFRESH_DATA_TYPE_CHECK);
            log.info("Registry schema ready: contracts are unique per (source, entity_type)");
        } catch (Exception e) {
            // Сервис должен подняться и без миграции: приём физлиц от неё не зависит.
            log.error("Could not migrate source_contract uniqueness: {}. "
                    + "A source will not be able to have both PERSON and LEGAL_ENTITY contracts "
                    + "until this is applied manually.", e.getMessage());
        }
    }
}
