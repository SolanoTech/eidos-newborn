# Eidos CDP-SCV

Платформа единого клиентского профиля: собирает данные о клиенте из разрозненных
систем-источников, сводит их в одну Золотую запись и отдаёт потребителям.

Работают две вертикали с общим движком, но разными моделями хранения и поиска:

* **физические лица** — сопоставление по ПИНФЛ и паспорту, точный поиск;
* **юридические лица** — сопоставление по ИНН, нечёткий поиск по названию.

## Из чего состоит

| Репозиторий | Роль | Порт на хосте |
|---|---|---|
| [eidos-gateway](eidos-gateway) | внешний интерфейс: приём данных от источников, валидация по контракту | 8090 |
| [eidos-stage](eidos-stage) | сырьё в MongoDB, приведение к общему контракту, реестр источников | 8081 |
| [eidos-core](eidos-core) | Золотая запись: создание, слияние, архив, поиск | 8080 |
| [eidos-consents](eidos-consents) | согласия на обработку данных | 8086 |
| [eidos-cdi-ui-backend](eidos-cdi-ui-backend) | backend-for-frontend для консолей | 8093 |
| [eidos-cdi-ui](eidos-cdi-ui) | консоли: администратор, фронт-офис, маркетинг | 8088, 8089 |
| [shared-library](shared-library) | общий контракт данных | — |

Инфраструктура: PostgreSQL, MongoDB, Redpanda (Kafka-совместимый брокер),
Keycloak, OpenBao, Gravitee APIM.

## Как данные проходят через платформу

```
источник ──REST──▶ gateway ──▶ stage ──▶ core ──▶ Золотая запись
         └─Kafka──▶
```

Gateway проверяет входящее против дата-контракта источника и подставляет код
источника из токена — сам источник его не объявляет. Stage сохраняет сырьё,
приводит к контракту и передаёт в core. Core сопоставляет запись с существующей
и либо создаёт новую, либо сливает по уровню доверия источника.

Тип сущности определяется каналом, а не содержимым: у физлиц и юрлиц разные
эндпоинты и разные топики.

## Установка

Нужны Docker и Docker Compose. Все модули собираются из исходников.

```bash
git clone git@github.com:SolanoTech/eidos-infrastructure.git eidos && cd eidos
```

Сервисные модули — самостоятельные репозитории, их нужно склонировать рядом:

```bash
for r in shared-library eidos-core eidos-stage eidos-consents eidos-gateway; do git clone git@github.com:SolanoTech/eidos-cdi-${r#eidos-}.git $r; done
git clone git@github.com:SolanoTech/eidos-cdi-ui-backend.git eidos-cdi-ui-backend
```

Консоли лежат в `eidos-cdi-console`:

```bash
git clone git@github.com:SolanoTech/eidos-cdi-console.git eidos-cdi-ui
```

Перед первым запуском задайте секреты — значений по умолчанию в репозитории нет:

```bash
cp .env.example .env
```

Консоли отправляют административные запросы через Gravitee APIM, а
`docker-compose.yml` подключается к его сети `frontend`, поэтому Gravitee
поднимается первым, а после запуска платформы в нём настраиваются маршруты
(подробности — в [документации](docs/quickstart/stand.md)):

```bash
docker compose -f gravitee/docker-compose-apim.yml up -d
```

Полный стенд:

```bash
docker compose up -d
ADMIN_TOKEN=<значение из .env> python3 keycloak/setup_gravitee.py
```

Отдельные модули (зависимости поднимутся сами):

```bash
docker compose up -d eidos-core
docker compose up -d eidos-stage
docker compose up -d eidos-consents
docker compose up -d eidos-gateway
docker compose up -d eidos-cdi-ui-backend
docker compose up -d eidos-cdi-ui
```

Только инфраструктура, без приложений:

```bash
docker compose up -d postgres mongo kafka keycloak openbao
```

Состояние и логи:

```bash
docker compose ps
docker compose logs -f eidos-core
```

## Конфигурация

Секреты и адреса задаются переменными окружения; в репозитории хранятся только
имена переменных и безопасные умолчания для локального стенда.

| Переменная | Назначение |
|---|---|
| `ADMIN_TOKEN` | внутренний административный API (заголовок `X-Admin-Token`) |
| `POSTGRES_PASSWORD` | пароль PostgreSQL |
| `OPENBAO_TOKEN` | доступ к хранилищу ключей |
| `OPENBAO_KEK_NAME` | имя ключа шифрования ключей |
| `CONSENT_EVENTS_TOPIC` | топик событий о согласиях |
| `CONSENT_EXPIRY_CRON` | расписание проверки истёкших согласий |

Токены источников выдаются через административный API реестра и в конфигурации
не хранятся.

Базы данных создаются при инициализации тома PostgreSQL скриптом
`docker/postgres-init/init-databases.sql` — по одной на сервис.

## Порты на хосте

| Сервис | Порт |
|---|---|
| eidos-core | 8080 |
| eidos-stage | 8081 |
| eidos-consents | 8086 |
| eidos-gateway | 8090 |
| eidos-cdi-ui-backend | 8093 |
| консоль администратора | 8088 |
| консоль фронт-офиса | 8089 |
| PostgreSQL | 5433 |
| MongoDB | 27018 |
| Kafka | 19092 |
| Keycloak | 8180 |
| OpenBao | 8200 |

Порты PostgreSQL, MongoDB и consents смещены относительно стандартных: те заняты
сторонними стеками на той же машине.

## Спецификации API

Каждый сервис отдаёт спецификацию OpenAPI на `/v3/api-docs` (YAML —
`/v3/api-docs.yaml`). На стенде включён и Swagger UI:

| Сервис | Swagger UI |
|---|---|
| eidos-gateway | http://localhost:8090/swagger-ui.html |
| eidos-core | http://localhost:8080/swagger-ui.html |
| eidos-stage | http://localhost:8081/swagger-ui.html |
| eidos-consents | http://localhost:8086/swagger-ui.html |
| eidos-cdi-ui-backend | http://localhost:8093/swagger-ui.html |

Вне стенда Swagger UI по умолчанию выключен, включает его переменная
`SPRINGDOC_SWAGGER_UI_ENABLED=true`. Выгруженные спецификации лежат в
`docs/reference/openapi/`, обновляет их `tools/docs/export_openapi.sh`.

## Документация

Полная документация модуля — в каталоге [docs](docs/index.md), сайт
собирается MkDocs Material:

```bash
pip install -r docs/requirements.txt
mkdocs serve
```

С чего начать:

* [Быстрый старт](docs/quickstart/stand.md) — стенд и первая Золотая запись
* [Подключение источников](docs/sources/onboarding.md) — для интеграторов
* [Установка](docs/operations/install.md) и [чек-лист безопасности](docs/security/hardening.md) — для эксплуатации
* [Статус и ограничения](docs/intro/status.md) — что учесть до внедрения

Материалы проектирования: [docs/eidos-cdp-archimate.xml](docs/eidos-cdp-archimate.xml) —
модель архитектуры в нотации ArchiMate, [docs/cjm.md](docs/cjm.md) — пути
пользователей по ролям. Нагрузочное тестирование —
[loadtest/README.md](loadtest/README.md).

## Лицензия

Apache License 2.0 — см. [LICENSE](LICENSE) и [NOTICE](NOTICE).

«Eidos» — товарный знак LLC SOLANOTECH; лицензия прав на имя и логотип не даёт,
условия использования описаны в [TRADEMARK.md](TRADEMARK.md).
