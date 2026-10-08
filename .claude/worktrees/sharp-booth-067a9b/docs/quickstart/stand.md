# Поднять стенд

За этот шаг вы соберёте и запустите всю платформу на своей машине: сервисы
модуля, базы данных, брокер, Keycloak, OpenBao и Gravitee APIM. Стенд
предназначен для знакомства и разработки — для серверной установки см.
[Стенд и продуктив](../operations/production.md).

## Что понадобится

- Docker с Docker Compose v2 и не меньше 8 ГБ памяти, выделенной Docker
  (с Gravitee и Elasticsearch комфортнее 12 ГБ);
- Git;
- Python 3.10 или новее — для скрипта настройки Gravitee;
- `curl` — для проверок.

Свободные порты на хосте: 5433, 8072, 8080–8090, 8093, 8180, 8200, 9644, 19092,
27018. Полный список — [Порты](../reference/ports.md).

## 1. Получите исходный код

Платформа собрана из нескольких репозиториев. Корневой репозиторий
`eidos-infrastructure` содержит compose-файлы и документацию; модули
клонируются внутрь него, каждый в свой каталог:

```bash
git clone https://github.com/SolanoTech/eidos-infrastructure.git eidos
cd eidos
git clone https://github.com/SolanoTech/eidos-cdi-shared-library.git shared-library
git clone https://github.com/SolanoTech/eidos-cdi-core.git eidos-core
git clone https://github.com/SolanoTech/eidos-cdi-stage.git eidos-stage
git clone https://github.com/SolanoTech/eidos-cdi-gateway.git eidos-gateway
git clone https://github.com/SolanoTech/eidos-cdi-consents.git eidos-consents
git clone https://github.com/SolanoTech/eidos-cdi-ui-backend.git eidos-cdi-ui-backend
git clone https://github.com/SolanoTech/eidos-cdi-console.git eidos-cdi-ui
```

!!! note "Имена каталогов важны"
    `docker-compose.yml` ищет модули по этим именам. Консоли лежат в
    репозитории `eidos-cdi-console`, но клонируются в каталог `eidos-cdi-ui`.

## 2. Задайте секреты

```bash
cp .env.example .env
```

Откройте `.env` и задайте значения:

| Переменная | Что указать |
|---|---|
| `ADMIN_TOKEN` | Длинная случайная строка, например результат `openssl rand -hex 24`. Общий токен административных API `eidos-stage` и `eidos-gateway` |
| `OPENBAO_TOKEN` | Случайная строка — корневой токен OpenBao на стенде |
| `POSTGRES_PASSWORD` | Можно оставить пустым: в текущей версии compose-файл его не использует, пароль PostgreSQL на стенде — `postgres` |

Остальные переменные можно оставить как есть. Подробно —
[Конфигурация и секреты](../operations/configuration.md).

Тот же административный токен понадобится скриптам и командам `curl` в
следующих шагах — экспортируйте его в текущую сессию терминала:

```bash
export ADMIN_TOKEN=<значение из .env>
```

## 3. Запустите Gravitee APIM

Консоли отправляют административные запросы через Gravitee, а
`docker-compose.yml` подключается к сети `frontend`, которую создаёт стек
Gravitee. Поэтому Gravitee запускается первым.

Стек рассчитан на файл лицензии `gravitee/license.key`, которого нет в
репозитории. Если лицензии Gravitee Enterprise у вас нет, удалите из
`gravitee/docker-compose-apim.yml` две строки, монтирующие `license.key`
(в сервисах `gateway` и `management_api`). Затем:

```bash
docker compose -f gravitee/docker-compose-apim.yml up -d
```

Первый запуск занимает несколько минут. Консоль Gravitee откроется по адресу
<http://localhost:8084> (логин `admin`, пароль `admin`).

??? question "Можно ли обойтись без Gravitee?"
    Приём и выдача данных работают без Gravitee, а административные экраны
    консолей — нет. Чтобы поднять стенд без Gravitee, создайте сеть вручную:
    `docker network create frontend` — и не запускайте консоли
    (`eidos-cdi-ui`, `eidos-cdp-ui`). Настраивать платформу в этом случае
    придётся через API — см. [Настройка через API](../operations/automation.md).

## 4. Соберите и запустите платформу

```bash
docker compose up -d --build
```

Первая сборка занимает 10–20 минут: Maven и npm скачивают зависимости. Образы
Java-сервисов собираются из исходников внутри контейнера, локальные JDK и Node.js
не нужны.

Следите за состоянием:

```bash
docker compose ps
```

Инфраструктура (`postgres`, `mongo`, `kafka`, `keycloak`, `openbao`) должна
перейти в состояние `healthy`, сервисы — в `running`.

## 5. Настройте маршруты Gravitee

Скрипт создаёт в Gravitee API для административных путей консолей, JWT-план с
проверкой токена Keycloak и подписку приложения консоли. Он идемпотентен, его
можно запускать повторно.

```bash
python3 keycloak/setup_gravitee.py
```

Скрипт берёт `ADMIN_TOKEN` из окружения: значение должно совпадать с указанным
в `.env`, иначе Gravitee будет подставлять в запросы к сервисам неверный токен.
Подробнее — [Gravitee APIM](../operations/gravitee.md).

## 6. Проверьте, что всё работает

Каждый сервис отвечает на `GET /health`:

```bash
for port in 8080 8081 8090 8086 8093; do
  printf '%s ' "$port"; curl -s "http://localhost:$port/health"; echo
done
```

Ожидаемый результат — пять строк вида `8080 {"status":"ok"}`.

Откройте консоли:

| Что | Адрес | Вход |
|---|---|---|
| Консоль администратора | <http://localhost:8088> | `admin` / `admin` |
| Фронт-офис | <http://localhost:8089> | `admin` / `admin` |
| Keycloak (администрирование) | <http://localhost:8180/admin> | `admin` / `admin` |
| Gravitee (консоль) | <http://localhost:8084> | `admin` / `admin` |

Вход в консоли идёт через страницу Keycloak. Если консоль открылась, но
разделы «Источники» или «Конструктор контракта» показывают ошибку, проверьте
шаг 5.

!!! warning "Стенд не для реальных данных"
    Пароли по умолчанию, режим разработки Keycloak и OpenBao, открытые порты
    баз данных — всё это допустимо только на локальной машине. Перед загрузкой
    реальных данных пройдите [чек-лист безопасности](../security/hardening.md).

## Остановить и очистить

```bash
docker compose down                                   # остановить, данные сохраняются
docker compose -f gravitee/docker-compose-apim.yml down
```

Чтобы удалить данные стенда, добавьте `-v` к первой команде: удалятся тома
PostgreSQL и MongoDB. Данные Gravitee хранятся в каталогах
`gravitee/mongodb/data` и `gravitee/elasticsearch/data`.

## Дальше

[Первая Золотая запись](first-golden-record.md) — зарегистрируйте источник,
опишите дата-контракт и отправьте первую карточку клиента.
