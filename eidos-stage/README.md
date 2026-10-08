# eidos-stage

Приём и трансформация данных: сохраняет присланное источником сырьё, приводит его
к общему контракту по настраиваемому маппингу и передаёт в eidos-core. Здесь же
живёт реестр источников и их дата-контрактов.

## Что делает

На каждое входящее сообщение `{data, source}`:

1. **Сохраняет сырьё** в MongoDB как есть — первым шагом, чтобы ничего не
   потерять, даже если дальше обработка сорвётся.
2. **Подбирает дата-контракт** источника для нужного типа сущности из реестра.
3. **Преобразует** поля источника в поля Золотой записи по маппингу контракта:
   переименование, приведение типов, разбор дат в формате источника, копирование
   массивов и структур поддеревом.
4. **Проверяет** обязательность и форматы.
5. **Отправляет** результат в eidos-core.

Тип сущности определяется каналом, а не содержимым сообщения: у физлиц и юрлиц
разные эндпоинты. Источник тип не присылает и подменить его не может.

**Реестр источников** хранит сами источники, их уровни доверия и токены доступа,
а также дата-контракты: какое поле источника во что превращается. Контракт свой
на каждую пару «источник + тип сущности».

**Статистика приёма** считает принятые и отклонённые сообщения по источникам.

## Установка

```bash
docker compose up -d eidos-stage
```

Поднимет заодно PostgreSQL, MongoDB и eidos-core. Отдельно, без приложения:

```bash
docker compose up -d postgres mongo
```

Локально из исходников (нужны JDK 26, установленная `shared-library` и
запущенный eidos-core):

```bash
mvn -f shared-library/pom.xml clean install
cd eidos-stage && mvn spring-boot:run
```

Тесты:

```bash
cd eidos-stage && mvn test
```

## Конфигурация

`src/main/resources/application.properties`; в контейнере переопределяется
переменными окружения.

| Ключ | Назначение |
|---|---|
| `server.port` | порт сервиса, по умолчанию `8081` |
| `spring.mongodb.uri` | адрес MongoDB вместе с именем базы |
| `eidos.stage.raw-collection` | коллекция для сырья |
| `spring.datasource.url` | база реестра источников |
| `spring.datasource.username` / `.password` | доступ к базе |
| `eidos.stage.admin-token` | токен административного API (заголовок `X-Admin-Token`) |
| `eidos.core.base-url` | адрес eidos-core |

Имя базы MongoDB задаётся внутри `spring.mongodb.uri` — отдельного ключа для него
нет.

## Эндпоинты

### Внешние — приём данных

Вызываются через eidos-gateway, напрямую источникам не отдаются.

| Метод | Путь | Назначение |
|---|---|---|
| `POST` | `/api/v1/client-data` | карточка физлица, тело `{"data": {...}, "source": "..."}` |
| `POST` | `/api/v1/legal-data` | карточка юрлица, тело то же |

Ответы: `200` при успехе, `400` на битом JSON, `422` если не найден контракт,
сорвался маппинг, не прошла проверка или отказал core.

### Внутренние — администрирование

Закрыты заголовком `X-Admin-Token`.

| Метод | Путь | Назначение |
|---|---|---|
| `GET` | `/internal/api/v1/sources` | список источников |
| `POST` | `/internal/api/v1/sources` | завести источник |
| `GET` | `/internal/api/v1/sources/{id}` | источник |
| `PUT` | `/internal/api/v1/sources/{id}` | изменить источник |
| `DELETE` | `/internal/api/v1/sources/{id}` | удалить источник |
| `GET` | `/internal/api/v1/sources/{sourceId}/contract` | дата-контракт источника |
| `PUT` | `/internal/api/v1/sources/{sourceId}/contract` | заменить контракт целиком |
| `POST` | `/internal/api/v1/sources/{sourceId}/contract/fields` | добавить поле |
| `PUT` | `/internal/api/v1/sources/{sourceId}/contract/fields/{fieldId}` | изменить поле |
| `DELETE` | `/internal/api/v1/sources/{sourceId}/contract/fields/{fieldId}` | удалить поле |
| `GET` | `/internal/api/v1/golden-record-fields` | доступные поля Золотой записи для маппинга |
| `GET` | `/internal/api/v1/ingest-stats` | статистика приёма по источникам |
| `GET` | `/health` | проба живости |

## Лицензия

Apache License 2.0 — см. [LICENSE](LICENSE) и [NOTICE](NOTICE).

«Eidos» — товарный знак LLC SOLANOTECH; лицензия прав на имя и логотип не даёт,
условия использования описаны в [TRADEMARK.md](TRADEMARK.md).
