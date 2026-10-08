# Базы данных и миграции

## PostgreSQL

Все сервисы используют один сервер PostgreSQL 16 с отдельной базой на сервис.
Базы создаёт скрипт `docker/postgres-init/init-databases.sql` при первой
инициализации тома.

| База | Владелец схемы | Кто ещё обращается | Как управляется схема |
|---|---|---|---|
| `eidos_core` | `eidos-core` | — | Flyway, проверка сущностей при старте |
| `eidos_registry` | `eidos-stage` | `eidos-gateway`: читает реестр и контракты, пишет счётчик отклонённых в `ingest_stats` | Hibernate `ddl-auto=update` + стартовый скрипт |
| `gateway` | `eidos-gateway` | — | Hibernate `ddl-auto=update` + стартовый скрипт |
| `consents` | `eidos-consents` | — | Hibernate `ddl-auto=update` |
| `eidos_ui_backend` | `eidos-cdi-ui-backend` | — | Hibernate `ddl-auto=update` |
| `keycloak` | Keycloak | — | Миграции Keycloak |

### eidos_core

Схема ведётся миграциями Flyway из `eidos-core/src/main/resources/db/migration`:

| Миграция | Содержание |
|---|---|
| `V1__baseline.sql` | Расширение `pg_trgm`; Золотые записи физлиц и юрлиц, архив, внешние идентификаторы, происхождение полей, незавершённые записи, источники, индексы |
| `V2__consent_state_on_golden_record.sql` | Колонки состояния согласия в `golden_record` |
| `V3__token_vault.sql` | Схема `vault`: ключи данных и токены |
| `V4__blind_index.sql` | Колонки и индексы слепого индекса, таблица ключей индекса |

- Ядро проверяет соответствие сущностей схеме при старте (`ddl-auto=validate`):
  если код ожидает колонку, которой нет, ядро не запустится. Схему меняют
  только миграции.
- `baseline-on-migrate=true` с версией 1 позволяет подключить к Flyway базу,
  созданную до появления миграций: Flyway примет её за версию 1 и применит
  миграции начиная со второй.
- Расширение `pg_trgm` создаётся миграцией V1; при старте ядро дополнительно
  проверяет наличие расширения и GIN-индекса для нечёткого поиска мерчантов.
  Пользователю базы нужно право `CREATE` на базу: `pg_trgm` — доверенное
  расширение.

Таблицы схемы `vault` хранят ключи, завёрнутые KEK из OpenBao. Без
соответствующего KEK их содержимое бесполезно — см. [OpenBao](openbao.md).

### Сервисы на Hibernate `ddl-auto=update`

`eidos-stage`, `eidos-gateway`, `eidos-consents` и `eidos-cdi-ui-backend`
создают и дополняют таблицы при старте средствами Hibernate. У этого подхода
есть ограничения, которые важны при обновлении:

- новые колонки и таблицы добавляются, а старые **не удаляются и не меняются**;
- **check-ограничения перечислений не обновляются.** Hibernate создаёт
  ограничение со списком допустимых значений перечисления, например типов
  согласий. Если новая версия добавит значение, вставка с ним завершится
  ошибкой, пока ограничение не обновят вручную.

`eidos-stage` и `eidos-gateway` выполняют при старте идемпотентные скрипты,
которые закрывают известные случаи: тип сущности контракта, уникальность
контракта на пару «источник + тип», ограничение типов полей контракта,
уникальность канала Kafka на тип сущности.

Пример ручного обновления ограничения типов согласий:

```sql
ALTER TABLE consents DROP CONSTRAINT IF EXISTS consents_type_check;
ALTER TABLE consents ADD CONSTRAINT consents_type_check CHECK (type IN (
  'PERSONAL_DATA', 'BIO', 'MARKETING_SMS', 'MARKETING_PUSH',
  'MARKETING_EMAIL', 'PROFILING', 'THIRD_PARTY'));
```

Фактическое имя ограничения проверьте командой `\d consents` в `psql`.

### Пользователи и права

На стенде все сервисы подключаются пользователем `postgres`. На сервере
заведите пользователя на каждый сервис:

| Пользователь | Права |
|---|---|
| ядро | Владелец базы `eidos_core` |
| stage | Владелец базы `eidos_registry` |
| шлюз | Владелец базы `gateway`; в `eidos_registry` — `SELECT` на `source`, `source_contract`, `source_field`, `source_field_value_map`; `SELECT, INSERT, UPDATE` на `ingest_stats` |
| согласия | Владелец базы `consents` |
| ui-backend | Владелец базы `eidos_ui_backend` |
| Keycloak | Владелец базы `keycloak` |

Пароли передаются сервисам переменными `SPRING_DATASOURCE_USERNAME` и
`SPRING_DATASOURCE_PASSWORD` (у шлюза для реестра —
`EIDOS_REGISTRY_DATASOURCE_USERNAME` и `EIDOS_REGISTRY_DATASOURCE_PASSWORD`).

## MongoDB

| База | Коллекция | Содержимое |
|---|---|---|
| `eidos_stage` | `raw_client_data` | Сырьё: документ на каждую карточку, прошедшую шлюз |

Документ сырья:

```json
{
  "_id": "…",
  "source": "bank-1",
  "entityType": "PERSON",
  "data": { "…": "данные источника как есть" },
  "receivedAt": "2026-10-04T10:15:30.481Z"
}
```

- Имя базы задаётся в адресе подключения `SPRING_MONGODB_URI`
  (`mongodb://mongo:27017/eidos_stage`); имя коллекции —
  `EIDOS_STAGE_RAW_COLLECTION`.
- В Spring Boot 4 свойства подключения переехали с `spring.data.mongodb.*` на
  `spring.mongodb.*`. Переменная `SPRING_DATA_MONGODB_URI` игнорируется, и
  сервис подключается к `localhost`.
- Индексов, кроме `_id`, и срока хранения нет: коллекция растёт
  неограниченно. Если сырьё нужно хранить ограниченное время, создайте
  TTL-индекс:

  ```javascript
  db.raw_client_data.createIndex({ receivedAt: 1 }, { expireAfterSeconds: 60 * 60 * 24 * 365 })
  ```

  Учтите, что сырьё — единственный источник исходного состояния карточки:
  архив Золотой записи не хранит состояние на момент создания.

## Внешние базы

Вместо контейнеров `postgres` и `mongo` можно использовать управляемые или
внешние серверы тех же основных версий. Создайте базы и пользователей
вручную по таблицам выше, задайте адреса подключения через переменные
окружения сервисов и уберите контейнеры из запуска.
