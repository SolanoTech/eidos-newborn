# Gravitee APIM

Gravitee APIM — API-шлюз, через который консоли выполняют административные
операции. Он проверяет токен пользователя, выпущенный Keycloak, и передаёт
запрос сервису, добавляя служебный токен `X-Admin-Token`.

!!! info "Опциональный компонент"
    Gravitee нужен только административным экранам консолей. Приём данных от
    источников, выдача данных потребителям, согласия и настройка платформы
    через API работают без него. В текущей версии консоли без Gravitee не
    работают: nginx консолей отправляет административные пути только в
    Gravitee. См. [Работа без Gravitee](#работа-без-gravitee).

## Что идёт через Gravitee

| Путь консоли | API в Gravitee | Сервис и путь | `X-Admin-Token` |
|---|---|---|---|
| `/api/v1/admin/sources/**` | `/eidos/sources` | `eidos-stage` `/internal/api/v1/sources` | да |
| `/api/v1/admin/golden-record-fields` | `/eidos/golden-record-fields` | `eidos-stage` `/internal/api/v1/golden-record-fields` | да |
| `/api/v1/admin/kafka-config` | `/eidos/kafka-config` | `eidos-gateway` `/internal/api/v1/kafka-config` | да |
| `/api/v1/admin/golden-records/**` | `/eidos/golden-records` | `eidos-core` `/api/v1/internal/golden-records` | нет |
| `/api/v1/admin/conflicts/**` | `/eidos/conflicts` | `eidos-core` `/api/v1/internal/tentative` | нет |
| `/api/v1/admin/legal-records/**` | `/eidos/legal-records` | `eidos-core` `/api/v1/internal/legal-records` | нет |
| `/api/v1/admin/conflicts-legal/**` | `/eidos/conflicts-legal` | `eidos-core` `/api/v1/internal/tentative-legal` | нет |

Остальные пути консоли (`/api/v1/admin/dashboard`, `/audit`, `/consents`)
обслуживает `eidos-cdi-ui-backend` напрямую.

## Стек

`gravitee/docker-compose-apim.yml` поднимает:

| Сервис | Контейнер | Порт на хосте |
|---|---|---|
| Шлюз | `gio_apim_gateway` | 8082 |
| Management API | `mgmtapi` | 8083, 8072 |
| Консоль управления | `gio_apim_management_ui` | 8084 |
| Портал разработчика | `gio_apim_portal_ui` | 8085 |
| MongoDB | `gio_apim_mongodb` | — |
| Elasticsearch | `gio_apim_elasticsearch` | — |

Стек создаёт сети `frontend` и `storage`. Шлюз Gravitee подключён к `frontend`
и видит в ней Keycloak и сервисы платформы; nginx консолей обращается к нему
по имени `gateway`. Поэтому стек Gravitee запускается **до** основного
`docker-compose.yml`.

Данные Gravitee хранятся в каталогах `gravitee/mongodb/data` и
`gravitee/elasticsearch/data`, журналы — в `gravitee/*/logs`.

### Лицензия

Шлюз и Management API монтируют файл `gravitee/license.key` — лицензию Gravitee
Enterprise. В репозитории его нет. Если лицензии у вас нет, удалите из
compose-файла строки монтирования `license.key` в сервисах `gateway` и
`management_api`.

## Настройка маршрутов

Скрипт `keycloak/setup_gravitee.py` создаёт всё, что нужно консолям:

- для каждого пути из таблицы выше — API типа v4 PROXY с контекстным путём
  `/eidos/…`;
- для путей к `eidos-stage` и `eidos-gateway` — поток с политикой
  `transform-headers`, которая добавляет `X-Admin-Token`;
- на каждом API — JWT-план: подпись RS256 проверяется по ключам Keycloak
  (`http://keycloak:8180/realms/eidos/protocol/openid-connect/certs`),
  идентификатор приложения берётся из claim `azp`;
- приложение `eidos-console` с `client_id` = `eidos-console` и подписку этого
  приложения на каждый план;
- запуск и развёртывание API на шлюзе.

```bash
ADMIN_TOKEN=<административный токен> python3 keycloak/setup_gravitee.py
```

| Переменная | Значение по умолчанию | Назначение |
|---|---|---|
| `MGMT_URL` | `http://localhost:8083` | Адрес Management API |
| `MGMT_USER`, `MGMT_PASS` | `admin`, `admin` | Учётная запись администратора Gravitee |
| `ADMIN_TOKEN` | `change-me-admin-token` | Значение `X-Admin-Token` для сервисов — должно совпадать с `ADMIN_TOKEN` в `.env` |

Скрипт идемпотентен: существующие API не дублируются, недостающие планы,
подписки, запуск и развёртывание добавляются. Проверка:

```bash
curl -s -H "Authorization: Bearer <токен Keycloak>" http://localhost:8082/eidos/sources
```

!!! warning "Подписка обязательна"
    JWT-план Gravitee пропускает только токены, чей claim `azp` совпадает с
    `client_id` приложения, **подписанного** на план. Без подписки шлюз
    отвечает `401` даже на токен с правильной подписью. Скрипт оформляет
    подписки сам; при ручной настройке не забудьте этот шаг.

### Смена административного токена

Скрипт не обновляет уже созданные API: значение `X-Admin-Token` записано в
политике при создании. Если вы меняете `ADMIN_TOKEN`:

1. Обновите `.env` и пересоздайте сервисы: `docker compose up -d`.
2. В консоли Gravitee измените значение заголовка в политике
   `Inject X-Admin-Token` у API `eidos-sources`, `eidos-golden-record-fields`,
   `eidos-kafka-config` и разверните API заново — или удалите эти API и
   запустите скрипт ещё раз.

## Работа без Gravitee

Без Gravitee работают:

- приём данных по REST и из Kafka, внешний API шлюза, согласия;
- административные API сервисов — с заголовком `X-Admin-Token` из внутренней
  сети, см. [Настройка через API](automation.md);
- API `eidos-cdi-ui-backend`: дашборд, аудит-лог, согласия.

Консоли в текущей версии без Gravitee не работают совсем: nginx консоли при
запуске разрешает имя `gateway`, и без шлюза Gravitee в сети `frontend`
контейнер консоли не стартует.

Чтобы запустить основной `docker-compose.yml` без стека Gravitee, создайте
сеть вручную и поднимите сервисы без консолей:

```bash
docker network create frontend
docker compose up -d postgres mongo kafka keycloak openbao \
  eidos-core eidos-stage eidos-gateway eidos-consents eidos-cdi-ui-backend
```

## Производственная установка

- Закрепите версии образов Gravitee вместо `latest` — одинаковые для всех
  компонентов APIM.
- Смените пароль администратора Gravitee.
- Elasticsearch на стенде получает 512 МБ памяти — на сервере выделите больше
  по рекомендациям Gravitee.
- Шлюз Gravitee публикуется только для nginx консолей: снаружи к нему
  обращаться не нужно.
