# Установка через Docker Compose

Установка на сервер повторяет [быстрый старт](../quickstart/stand.md), но с
закреплёнными версиями, собственными секретами и файлом переопределений для
сервера. Прочтите [Стенд и продуктив](production.md) до начала установки.

## Что входит в поставку

| Файл в `eidos-infrastructure` | Назначение |
|---|---|
| `docker-compose.yml` | Сервисы модуля и инфраструктура: PostgreSQL, MongoDB, Redpanda, Keycloak, OpenBao |
| `.env.example` | Шаблон переменных окружения |
| `docker/postgres-init/init-databases.sql` | Создание баз данных при первой инициализации тома PostgreSQL |
| `keycloak/realm-eidos.json` | Realm `eidos`, импортируется при первом запуске Keycloak |
| `keycloak/setup_gravitee.py` | Настройка маршрутов, плана и подписки в Gravitee |
| `gravitee/docker-compose-apim.yml` | Стек Gravitee APIM |
| `loadtest/` | Сквозная проверка и нагрузочный тест |

Образы сервисов собираются из исходников модулей при `docker compose build`:

| Сервис compose | Модуль | Контекст сборки |
|---|---|---|
| `eidos-core` | `eidos-core` + `shared-library` | Корень репозитория |
| `eidos-stage` | `eidos-stage` + `shared-library` | Корень репозитория |
| `eidos-gateway` | `eidos-gateway` | `./eidos-gateway` |
| `eidos-consents` | `eidos-consents` | `./eidos-consents` |
| `eidos-cdi-ui-backend` | `eidos-cdi-ui-backend` | `./eidos-cdi-ui-backend` |
| `eidos-cdi-ui` | Консоль администратора | `./eidos-cdi-ui`, аргумент `APP=admin` |
| `eidos-cdp-ui` | Фронт-офис | `./eidos-cdi-ui`, аргумент `APP=front-office` |

## 1. Подготовьте код

Клонируйте корневой репозиторий и модули, как в
[быстром старте](../quickstart/stand.md#1-получите-исходный-код). Для сервера
переключите каждый репозиторий на согласованную версию — тег или коммит, — а
не используйте `main`:

```bash
for dir in . shared-library eidos-core eidos-stage eidos-gateway eidos-consents eidos-cdi-ui-backend eidos-cdi-ui; do
  git -C "$dir" checkout <тег-или-коммит-для-этого-репозитория>
done
```

Набор совместимых версий модулей — [Совместимость версий](../reference/compatibility.md).

## 2. Задайте секреты и переопределения

```bash
cp .env.example .env
chmod 600 .env
```

Заполните `.env` — см. [Конфигурация и секреты](configuration.md#переменные-env).
Создайте `docker-compose.prod.yml` по образцу из раздела
[Стенд и продуктив](production.md#файл-переопределений) и, если консоли будут
открываться не с `localhost`, доработайте `Dockerfile` консолей — см.
[Адрес Keycloak в консолях](production.md#адрес-keycloak-в-консолях).

## 3. Подготовьте Keycloak и OpenBao

- Keycloak: отредактируйте `keycloak/realm-eidos.json` до первого запуска —
  адреса консолей, секрет клиента `eidos-m2m`, учебный пользователь. Realm
  импортируется только при первом запуске. См. [Keycloak](keycloak.md).
- OpenBao: для сервера замените сервис `openbao` на установку с постоянным
  хранилищем или подключите внешний OpenBao. См. [OpenBao](openbao.md).

## 4. Запустите Gravitee

```bash
docker compose -f gravitee/docker-compose-apim.yml up -d
```

Gravitee создаёт сеть `frontend`, к которой подключаются сервисы платформы.
См. [Gravitee APIM](gravitee.md).

## 5. Соберите образы

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml build
```

Базовый образ JDK задаётся аргументом `JAVA_IMAGE` (по умолчанию
`eclipse-temurin:26-jdk`):

```bash
docker compose build --build-arg JAVA_IMAGE=bellsoft/liberica-openjdk-debian:26
```

!!! warning "Проверяйте результат сборки"
    Если сборка образа упала, `docker compose up -d --build` может оставить
    работать прежний контейнер со старой версией, и ошибка легко
    теряется в выводе. Собирайте отдельной командой, проверяйте код выхода и
    пересоздавайте контейнеры явно: `docker compose up -d --force-recreate <сервис>`.

## 6. Запустите платформу

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
docker compose ps
```

Порядок запуска задают зависимости: сначала PostgreSQL, MongoDB, брокер и
OpenBao, затем сервисы. При первом запуске:

- PostgreSQL создаёт базы `eidos_core`, `eidos_registry`, `consents`,
  `gateway`, `eidos_ui_backend`, `keycloak`;
- Keycloak импортирует realm `eidos`;
- `eidos-core` применяет миграции Flyway, создаёт в OpenBao движок transit и
  ключ шифрования ключей, генерирует ключ слепого индекса;
- остальные сервисы создают свои таблицы.

## 7. Настройте Gravitee

```bash
ADMIN_TOKEN=<значение из .env> MGMT_PASS=<пароль администратора Gravitee> \
  python3 keycloak/setup_gravitee.py
```

## 8. Проверьте

Сервисы отвечают на `GET /health`. Если их порты не опубликованы на хосте,
проверьте их изнутри сети compose (проект называется `eidos`, сеть —
`eidos_eidos-net`):

```bash
for target in eidos-core:8080 eidos-stage:8081 eidos-gateway:8090 eidos-consents:8082 eidos-cdi-ui-backend:8083; do
  printf '%s ' "$target"
  docker run --rm --network eidos_eidos-net curlimages/curl -s "http://$target/health"; echo
done
curl -s https://<адрес шлюза>/health
```

Каждая строка должна заканчиваться на `{"status":"ok"}`.

Откройте консоль администратора и войдите. Затем пройдите сквозную проверку:
зарегистрируйте тестовый источник и отправьте карточку, как в упражнении
[Первая Золотая запись](../quickstart/first-golden-record.md), или
воспользуйтесь скриптами каталога `loadtest` — см.
[Производительность](performance.md).

## Повседневные команды

| Задача | Команда |
|---|---|
| Состояние | `docker compose ps` |
| Журнал сервиса | `docker compose logs -f --since 10m eidos-core` |
| Перезапуск сервиса | `docker compose restart eidos-gateway` |
| Пересборка одного сервиса | `docker compose build eidos-stage && docker compose up -d --force-recreate eidos-stage` |
| Остановка без удаления данных | `docker compose down` |
| Остановка с удалением томов | `docker compose down -v` — **удаляет все данные** |

Обновление на новую версию — [Обновление версии](upgrade.md).
