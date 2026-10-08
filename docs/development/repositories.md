# Репозитории

Платформа разбита на несколько репозиториев. Корневой репозиторий
`eidos-infrastructure` собирает их в стенд и хранит документацию; модули
клонируются внутрь него, каждый в свой каталог, и в корневой репозиторий не
попадают (`.gitignore`).

| Репозиторий | Каталог | Содержимое | Сборка |
|---|---|---|---|
| [`eidos-infrastructure`](https://github.com/SolanoTech/eidos-infrastructure) | корень | Compose-файлы, инициализация баз, realm Keycloak, настройка Gravitee, нагрузочный тест, документация | — |
| [`eidos-cdi-shared-library`](https://github.com/SolanoTech/eidos-cdi-shared-library) | `shared-library` | Описание Золотых записей и каталог полей | Maven, библиотека |
| [`eidos-cdi-core`](https://github.com/SolanoTech/eidos-cdi-core) | `eidos-core` | Ядро | Maven, Spring Boot |
| [`eidos-cdi-stage`](https://github.com/SolanoTech/eidos-cdi-stage) | `eidos-stage` | Приём, трансформация, реестр | Maven, Spring Boot |
| [`eidos-cdi-gateway`](https://github.com/SolanoTech/eidos-cdi-gateway) | `eidos-gateway` | Внешний интерфейс | Maven, Spring Boot |
| [`eidos-cdi-consents`](https://github.com/SolanoTech/eidos-cdi-consents) | `eidos-consents` | Согласия | Maven, Spring Boot |
| [`eidos-cdi-ui-backend`](https://github.com/SolanoTech/eidos-cdi-ui-backend) | `eidos-cdi-ui-backend` | Backend-for-frontend консолей | Maven, Spring Boot |
| [`eidos-cdi-console`](https://github.com/SolanoTech/eidos-cdi-console) | `eidos-cdi-ui` | Консоли и общие пакеты интерфейса | npm workspaces, Vite |

## Устройство корневого репозитория

```text
eidos/
├── docker-compose.yml          стенд: сервисы и инфраструктура
├── .env.example                шаблон секретов
├── docker/postgres-init/       создание баз данных
├── keycloak/                   realm eidos, настройка Gravitee
├── gravitee/                   стек Gravitee APIM
├── loadtest/                   сквозная проверка и нагрузочный тест
├── mkdocs.yml, docs/           документация
├── tools/docs/                 генерация справочника полей
└── shared-library/, eidos-*/   модули — отдельные репозитории
```

## Устройство Java-модулей

Сервисы устроены однотипно:

| Пакет | Назначение |
|---|---|
| `web` | REST-контроллеры, обработчик ошибок, DTO запросов и ответов |
| `service` | Прикладная логика |
| `entity`, `repository` | Сущности JPA и репозитории Spring Data |
| `config` | Конфигурация Spring, стартовые скрипты схемы |
| `security` | Фильтры токенов (`eidos-stage`, `eidos-gateway`) |

В `eidos-core` логика сопоставления и слияния вынесена в пакет `engine` —
обобщённые классы, общие для физлиц и юрлиц: `RecordSaveFlow`, `RecordCreator`,
`RecordMerger`. Специфика типа записи — тонкие фасады в `service` и
`service/legal`. Криптография — пакет `crypto`, согласия — `consent`.

## Устройство консолей

```text
eidos-cdi-ui/
├── apps/
│   ├── admin/          консоль администратора
│   ├── front-office/   фронт-офис
│   └── marketing/      заготовка, вне модуля CDI
└── packages/
    ├── ui-kit/         компоненты и дизайн-система
    ├── auth/           вход через Keycloak
    └── api-client/     HTTP-клиент с токеном
```

У каждого приложения свой `nginx.conf` с маршрутами API и свой
`vite.config.ts`.

## Общие файлы

Каждый репозиторий содержит `LICENSE`, `NOTICE`, `CONTRIBUTING.md`,
`SECURITY.md` и `TRADEMARK.md` — см. [Лицензия и товарный знак](../legal.md) и
[Как внести вклад](contributing.md).
