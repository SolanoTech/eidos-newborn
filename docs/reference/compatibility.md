# Совместимость версий

## Модули

Пронумерованных выпусков платформы пока нет: у репозиториев нет тегов, а
версии в файлах сборки — рабочие (`SNAPSHOT`). Модули развиваются вместе, и
совместимым набором считается **состояние основных веток всех репозиториев на
одну дату**.

Эта документация описывает следующий набор:

| Репозиторий | Каталог | Версия сборки | Коммит | Дата |
|---|---|---|---|---|
| `eidos-infrastructure` | `.` | — | `5c6c6d2` | 2026-09-28 |
| `eidos-cdi-shared-library` | `shared-library` | `0.0.1` | `4189ffc` | 2026-09-28 |
| `eidos-cdi-core` | `eidos-core` | `0.0.1-SNAPSHOT` | `9f131ce` | 2026-09-28 |
| `eidos-cdi-stage` | `eidos-stage` | `1.0-SNAPSHOT` | `9f232b6` | 2026-09-28 |
| `eidos-cdi-gateway` | `eidos-gateway` | `0.0.1-SNAPSHOT` | `4370ba0` | 2026-09-28 |
| `eidos-cdi-consents` | `eidos-consents` | `0.0.1-SNAPSHOT` | `38637f3` | 2026-09-28 |
| `eidos-cdi-ui-backend` | `eidos-cdi-ui-backend` | `0.0.1-SNAPSHOT` | `2e3c1a3` | 2026-09-28 |
| `eidos-cdi-console` | `eidos-cdi-ui` | `0.1.0` | `0bc9169` | 2026-09-28 |

Для установки на сервер закрепите коммиты всех репозиториев одного набора — см.
[Установка](../operations/install.md#1-подготовьте-код). С появлением выпусков
здесь будет таблица соответствия версий.

### Что связывает модули

| Связь | Что должно совпадать |
|---|---|
| `shared-library` → `eidos-core`, `eidos-stage` | Описание Золотых записей: обе стороны собираются с одной версией библиотеки |
| `eidos-stage` ↔ `eidos-gateway` | Схема реестра `eidos_registry`: шлюз читает таблицы, которые создаёт `eidos-stage` |
| `eidos-stage` → `eidos-core` | Формат запроса приёма (`record`, `source`, `client_source_identificator`) |
| `eidos-consents` → `eidos-core` | Формат событий `consent-events` и ответа о действующем согласии |
| Консоли → сервисы | Пути и форматы API, маршруты `nginx.conf` и Gravitee |

## Компоненты окружения

| Компонент | Проверенная версия | Требование |
|---|---|---|
| Java | 26 (Eclipse Temurin) | 26 |
| Spring Boot | 4.0.6 (`eidos-core`, `eidos-stage`, `eidos-consents`), 4.1.0 (`eidos-gateway`, `eidos-cdi-ui-backend`) | Задаётся в модулях |
| Maven | 3.9.9 | 3.9 |
| Node.js | 22 | 22 |
| PostgreSQL | 16 | 16; расширение `pg_trgm` |
| MongoDB | 7 | 7 |
| Kafka API | Redpanda 24.2.7 | Любая реализация Kafka API без обязательной аутентификации |
| Keycloak | 26.0 | 26 |
| OpenBao | `latest` на дату набора | Движок transit |
| Gravitee APIM | `latest` на дату набора | 4.x: v4 PROXY API, JWT-план, политика `transform-headers` |
| nginx | 1.27 | — |

## Внешние API

Пути внешнего API содержат номер версии (`/api/v1/…`). Правила совместимости
API будут зафиксированы с первым выпуском; пока изменения описываются в
[примечаниях к выпускам](../releases.md). Перед обновлением проверяйте их и
прогоняйте интеграционные проверки источников и потребителей на стенде.
