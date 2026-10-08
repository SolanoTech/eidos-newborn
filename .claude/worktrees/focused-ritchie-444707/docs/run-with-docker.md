# Запуск всей системы в Docker

Вся платформа Eidos SCV поднимается одной командой через `docker-compose.yml`
(в корне репозитория): единый сервер PostgreSQL (по базе на сервис + общая
`eidos_registry`), единый MongoDB и все сервисы в одной сети `eidos-net`.

## Состав

| Сервис | Образ/сборка | Порт на хосте | База данных |
|---|---|---|---|
| postgres | postgres:16 | 5433 → 5432 | сервер (см. ниже) |
| mongo | mongo:7 | 27018 → 27017 | `eidos_stage` |
| eidos-core | `eidos-core/Dockerfile` | 8080 | `eidos_core` |
| eidos-stage | `eidos-stage/Dockerfile` | 8081 | `eidos_registry` + Mongo |
| eidos-consents | `eidos-consents/Dockerfile` | 8082 | `consents` |
| eidos-gateway | `eidos-gateway/Dockerfile` | 8090 | `gateway` + `eidos_registry` (RO) |
| eidos-cdi-ui-backend | `eidos-cdi-ui-backend/Dockerfile` | 8083 | `eidos_ui_backend` |
| eidos-cdi-ui | `eidos-cdi-ui/Dockerfile` (nginx) | 8088 → 80 | — |

Базы создаёт `docker/postgres-init/init-databases.sh` при первой инициализации:
`eidos_core`, `eidos_registry`, `consents`, `gateway`, `eidos_ui_backend`.
`eidos_registry` общая: схему создаёт **stage**, **gateway** читает.

## Сборка Java 26

Базовый образ — `eclipse-temurin:26-jdk` (вынесен в build-arg `JAVA_IMAGE`).
Maven подкладывается из `maven:3.9.9-eclipse-temurin-21` в стадию сборки, поэтому
отдельный maven-образ под Java 26 не нужен. core и stage дополнительно собирают
`shared-library` (их build-контекст — корень репозитория).

Если нужен другой JDK 26 (например, EA-образ), переопределите arg:

```bash
docker compose build --build-arg JAVA_IMAGE=bellsoft/liberica-openjdk-debian:26
```

## Запуск

```bash
docker compose up --build -d      # собрать и поднять
docker compose ps                 # статус
docker compose logs -f eidos-core # логи сервиса
docker compose down               # остановить (тома сохраняются)
docker compose down -v            # остановить и удалить данные БД
```

UI: <http://localhost:8088> (вход `admin/admin` или `steward/steward`).

## Переопределяемые переменные

В `docker-compose.yml` конфигурация контейнеров задаётся через окружение
(не трогая `application.properties`):

- `ADMIN_TOKEN` — общий admin-токен stage/gateway (по умолчанию `change-me-admin-token`);
- `JWT_SECRET` — секрет подписи JWT в ui-backend (≥ 32 символов).

```bash
ADMIN_TOKEN=... JWT_SECRET=... docker compose up -d
```
