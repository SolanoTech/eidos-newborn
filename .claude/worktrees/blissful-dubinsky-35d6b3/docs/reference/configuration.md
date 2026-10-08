# Параметры конфигурации

Свойства задаются в `application.properties` сервиса и перекрываются
переменными окружения: точки и дефисы заменяются на `_`, буквы — заглавные.
Колонка «Стенд» — значение, которое задаёт `docker-compose.yml`. Как устроена
конфигурация — [Конфигурация и секреты](../operations/configuration.md).

## eidos-core

| Свойство | По умолчанию | Стенд | Назначение |
|---|---|---|---|
| `server.port` | `8080` | — | Порт |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/eidos_core` | `jdbc:postgresql://postgres:5432/eidos_core` | База Золотых записей |
| `spring.datasource.username`, `.password` | `postgres` | `postgres` | Доступ к базе |
| `spring.jpa.hibernate.ddl-auto` | `validate` | — | Сверка сущностей со схемой; не меняйте |
| `spring.flyway.enabled` | `true` | — | Миграции Flyway |
| `spring.flyway.baseline-on-migrate`, `.baseline-version` | `true`, `1` | — | Подключение базы без истории миграций как версии 1 |
| `spring.kafka.bootstrap-servers` | `localhost:19092` | `kafka:9092` | Брокер для событий о согласиях |
| `spring.kafka.consumer.auto-offset-reset` | `earliest` | — | Новая группа читает топик с начала |
| `eidos.consents.base-url` | `http://localhost:8082` | `http://eidos-consents:8082` | Сервис согласий для сверки |
| `eidos.consents.events.topic` | `consent-events` | `${CONSENT_EVENTS_TOPIC}` | Топик событий о согласиях |
| `eidos.consents.events.group-id` | `eidos-core-consent-state` | — | Группа потребителя событий |
| `eidos.consents.events.enabled` | `true` | `true` | Читать ли события о согласиях |
| `eidos.crypto.openbao.base-url` | `http://localhost:8200` | `http://openbao:8200` | Адрес OpenBao |
| `eidos.crypto.openbao.token` | `eidos-dev-root-token` | `${OPENBAO_TOKEN}` | Токен OpenBao |
| `eidos.crypto.openbao.kek-name` | `eidos-pd-kek` | `${OPENBAO_KEK_NAME}` | Имя KEK в transit |
| `eidos.crypto.enabled` | `true` | — | Создавать ли при старте движок transit и KEK. На слепой индекс не влияет |
| `eidos.crypto.blind-index.backfill-enabled` | `true` | — | Досчитывать ли слепой индекс при старте |
| `eidos.crypto.blind-index.batch-size` | `500` | — | Размер пачки досчёта |

## eidos-stage

| Свойство | По умолчанию | Стенд | Назначение |
|---|---|---|---|
| `server.port` | `8081` | — | Порт |
| `spring.mongodb.uri` | `mongodb://localhost:27017/eidos_stage` | `mongodb://mongo:27017/eidos_stage` | MongoDB для сырья; имя базы — в адресе. Старый ключ `spring.data.mongodb.uri` игнорируется |
| `eidos.stage.raw-collection` | `raw_client_data` | — | Коллекция сырья |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/eidos_registry` | `jdbc:postgresql://postgres:5432/eidos_registry` | База реестра |
| `spring.datasource.username`, `.password` | `postgres` | `postgres` | Доступ к базе |
| `spring.jpa.hibernate.ddl-auto` | `update` | — | Создание и дополнение таблиц |
| `eidos.stage.admin-token` | `change-me-admin-token` | `${ADMIN_TOKEN}` | Токен `/internal/**` |
| `eidos.core.base-url` | `http://localhost:8080` | `http://eidos-core:8080` | Адрес ядра |

## eidos-gateway

| Свойство | По умолчанию | Стенд | Назначение |
|---|---|---|---|
| `server.port` | `8090` | — | Порт |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/gateway` | `jdbc:postgresql://postgres:5432/gateway` | Собственная база: каналы Kafka |
| `spring.datasource.username`, `.password` | `postgres` | `postgres` | Доступ к собственной базе |
| `eidos.registry.datasource.url` | `jdbc:postgresql://localhost:5432/eidos_registry` | `jdbc:postgresql://postgres:5432/eidos_registry` | Реестр источников и контрактов |
| `eidos.registry.datasource.username`, `.password` | `postgres` | `postgres` | Доступ к реестру: чтение и запись `ingest_stats` |
| `gateway.admin-token` | `change-me-admin-token` | `${ADMIN_TOKEN}` | Токен `/internal/**` |
| `eidos.core.base-url` | `http://localhost:8080` | `http://eidos-core:8080` | Адрес ядра |
| `eidos.stage.base-url` | `http://localhost:8081` | `http://eidos-stage:8081` | Адрес `eidos-stage` |
| `eidos.consents.base-url` | `http://localhost:8082` | `http://eidos-consents:8082` | Адрес сервиса согласий |

Настройки потребителей Kafka хранятся в базе шлюза — см.
[API eidos-gateway](api/gateway-admin.md).

## eidos-consents

| Свойство | По умолчанию | Стенд | Назначение |
|---|---|---|---|
| `server.port` | `8082` | — | Порт; на хосте стенда — 8086 |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/consents` | `jdbc:postgresql://postgres:5432/consents` | База согласий |
| `spring.datasource.username`, `.password` | `postgres` | `postgres` | Доступ к базе |
| `spring.jpa.hibernate.ddl-auto` | `update` | — | Создание и дополнение таблиц |
| `spring.kafka.bootstrap-servers` | `localhost:19092` | `kafka:9092` | Брокер |
| `spring.kafka.producer.acks` | `1` | — | Подтверждение записи событий |
| `eidos.consents.events.topic` | `consent-events` | `${CONSENT_EVENTS_TOPIC}` | Топик событий |
| `eidos.consents.events.enabled` | `true` | `true` | Публиковать ли события |
| `eidos.consents.events.expiry-cron` | `0 5 0 * * *` | `${CONSENT_EXPIRY_CRON}` | Расписание прохода по истёкшим согласиям (cron Spring: секунды, минуты, часы, день, месяц, день недели) |

## eidos-cdi-ui-backend

| Свойство | По умолчанию | Стенд | Назначение |
|---|---|---|---|
| `server.port` | `8083` | — | Порт; на хосте стенда — 8093 |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/eidos_ui_backend` | `jdbc:postgresql://postgres:5432/eidos_ui_backend` | База журнала действий |
| `spring.datasource.username`, `.password` | `postgres` | `postgres` | Доступ к базе |
| `spring.jpa.hibernate.ddl-auto` | `update` | — | Создание и дополнение таблиц |
| `eidos.stage.base-url` | `http://localhost:8081` | `http://eidos-stage:8081` | Реестр и статистика для дашборда |
| `eidos.stage.admin-token` | `change-me-admin-token` | `${ADMIN_TOKEN}` | Токен административного API `eidos-stage` |
| `eidos.gateway.base-url` | `http://localhost:8090` | `http://eidos-gateway:8090` | Административный API шлюза |
| `eidos.gateway.admin-token` | `change-me-admin-token` | `${ADMIN_TOKEN}` | Токен административного API шлюза |
| `eidos.core.base-url` | `http://localhost:8080` | `http://eidos-core:8080` | Метрики ядра |
| `eidos.consents.base-url` | `http://localhost:8086` | `http://eidos-consents:8082` | Сервис согласий |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `http://localhost:8180/realms/eidos/protocol/openid-connect/certs` | `http://keycloak:8180/realms/eidos/protocol/openid-connect/certs` | Ключи Keycloak для проверки токенов |

## Консоли

Параметры задаются **при сборке** образа (переменные Vite):

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `VITE_KC_URL` | `http://localhost:8180` | Адрес Keycloak, который видит браузер |
| `VITE_KC_REALM` | `eidos` | Realm |
| `VITE_KC_CLIENT_ID` | `eidos-console` | Клиент OpenID Connect |

Аргумент сборки `APP` выбирает приложение: `admin` или `front-office`.
Маршрутизация запросов к API задана в `apps/<приложение>/nginx.conf` —
см. [Gravitee APIM](../operations/gravitee.md#что-идёт-через-gravitee).

## Общие для Java-сервисов

| Переменная | Назначение |
|---|---|
| `JAVA_TOOL_OPTIONS` | Параметры JVM, например `-XX:MaxRAMPercentage=75` |
| `TZ` | Часовой пояс: влияет на «сегодня» в согласиях, сутки статистики и расписание прохода |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | Размер пула соединений с базой, по умолчанию 10 |
| `LOGGING_LEVEL_COM_SOLANO` | Уровень журналирования кода модуля, например `DEBUG` |
