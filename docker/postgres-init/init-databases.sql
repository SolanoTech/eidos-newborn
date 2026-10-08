-- Создаёт отдельную базу данных под каждый сервис в едином сервере PostgreSQL.
-- Выполняется один раз при инициализации пустого тома (через psql, без +x).
-- eidos_registry — общая база: владелец схемы eidos-stage, eidos-gateway читает.
CREATE DATABASE eidos_core;
CREATE DATABASE eidos_registry;
CREATE DATABASE consents;
CREATE DATABASE gateway;
CREATE DATABASE eidos_ui_backend;
CREATE DATABASE keycloak;
