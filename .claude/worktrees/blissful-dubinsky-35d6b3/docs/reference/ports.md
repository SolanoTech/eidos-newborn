# Порты и сетевые взаимодействия

## Порты стенда

| Компонент | Сервис compose | Порт в контейнере | Порт на хосте | Публиковать на сервере |
|---|---|---|---|---|
| `eidos-core` | `eidos-core` | 8080 | 8080 | Нет |
| `eidos-stage` | `eidos-stage` | 8081 | 8081 | Нет |
| `eidos-consents` | `eidos-consents` | 8082 | 8086 | Нет |
| `eidos-cdi-ui-backend` | `eidos-cdi-ui-backend` | 8083 | 8093 | Нет |
| `eidos-gateway` | `eidos-gateway` | 8090 | 8090 | Да, через TLS |
| Консоль администратора | `eidos-cdi-ui` | 80 | 8088 | Да, через TLS |
| Фронт-офис | `eidos-cdp-ui` | 80 | 8089 | Да, через TLS |
| PostgreSQL | `postgres` | 5432 | 5433 | Нет |
| MongoDB | `mongo` | 27017 | 27018 | Нет |
| Redpanda, Kafka API | `kafka` | 9092 (внутренний), 19092 (внешний) | 19092 | Нет |
| Redpanda, API администрирования | `kafka` | 9644 | 9644 | Нет |
| Keycloak | `keycloak` | 8180 | 8180 | Да, через TLS |
| OpenBao | `openbao` | 8200 | 8200 | Нет |
| Gravitee, шлюз | `gateway` (стек APIM) | 8082 | 8082 | Нет |
| Gravitee, Management API | `management_api` | 8083, 8072 | 8083, 8072 | Нет |
| Gravitee, консоль управления | `management_ui` | 8080 | 8084 | Нет |
| Gravitee, портал | `portal_ui` | 8080 | 8085 | Нет |

Порты PostgreSQL, MongoDB, `eidos-consents` и `eidos-cdi-ui-backend` на хосте
смещены: 8082 и 8083 заняты Gravitee, стандартные порты баз часто заняты
локальными установками.

## Сети Docker

| Сеть | Кто в ней | Создаёт |
|---|---|---|
| `eidos-net` (`eidos_eidos-net`) | Все сервисы и инфраструктура платформы | `docker-compose.yml` |
| `frontend` | Keycloak, `eidos-core`, `eidos-stage`, `eidos-gateway`, консоли, шлюз и Management API Gravitee | Стек Gravitee; для основного compose — внешняя |
| `storage` | Gravitee, его MongoDB и Elasticsearch | Стек Gravitee |

## Кто к кому обращается

| Откуда | Куда | Протокол | Зачем |
|---|---|---|---|
| Источники, потребители | `eidos-gateway:8090` | HTTP | Приём, поиск, согласия |
| Источники | Брокер | Kafka | Карточки |
| `eidos-gateway` | `eidos-stage:8081` | HTTP | Передача карточек |
| `eidos-gateway` | `eidos-core:8080` | HTTP | Внешний API Золотых записей |
| `eidos-gateway` | `eidos-consents:8082` | HTTP | Согласия |
| `eidos-gateway` | PostgreSQL `gateway`, `eidos_registry` | JDBC | Каналы Kafka, реестр, статистика |
| `eidos-gateway` | Брокер | Kafka | Чтение карточек |
| `eidos-stage` | MongoDB | MongoDB | Сырьё |
| `eidos-stage` | `eidos-core:8080` | HTTP | Передача Золотых записей |
| `eidos-core` | `eidos-consents:8082` | HTTP | Сверка согласия |
| `eidos-core` | Брокер | Kafka | Чтение `consent-events` |
| `eidos-core` | `openbao:8200` | HTTP | Заворачивание ключей |
| `eidos-consents` | Брокер | Kafka | Публикация `consent-events` |
| `eidos-cdi-ui-backend` | `eidos-core`, `eidos-stage`, `eidos-consents` | HTTP | Дашборд, согласия |
| `eidos-cdi-ui-backend` | `keycloak:8180` | HTTP | Ключи проверки токенов |
| nginx консолей | `gateway:8082` (Gravitee) | HTTP | Административные пути |
| nginx консолей | `eidos-cdi-ui-backend:8083` | HTTP | Остальные пути `/api/` |
| Gravitee | `eidos-stage`, `eidos-gateway`, `eidos-core`, `keycloak` | HTTP | Административные пути, ключи проверки токенов |
| Браузер | Консоли, Keycloak | HTTP(S) | Интерфейс и вход |
