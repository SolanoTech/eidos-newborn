# Сборка и тесты

## Что нужно

| Инструмент | Версия |
|---|---|
| JDK | 26 |
| Maven | 3.9 — в модулях с Maven Wrapper (`./mvnw`) не нужен |
| Node.js | 22, с npm |
| Docker | Для инфраструктуры стенда |

## Библиотека контракта

`shared-library` подключается к `eidos-core` и `eidos-stage` как зависимость
Maven. Перед их сборкой установите библиотеку в локальный репозиторий:

```bash
mvn -f shared-library/pom.xml clean install
```

Повторяйте после каждого изменения библиотеки. В Docker библиотека собирается
автоматически: образы ядра и `eidos-stage` собираются из корня репозитория.

## Сервисы

| Модуль | Сборка | Тесты |
|---|---|---|
| `shared-library` | `mvn -f shared-library/pom.xml install` | `mvn -f shared-library/pom.xml test` |
| `eidos-core` | `cd eidos-core && ./mvnw package` | `./mvnw test` |
| `eidos-stage` | `cd eidos-stage && mvn package` | `mvn test` |
| `eidos-gateway` | `cd eidos-gateway && ./mvnw package` | `./mvnw test` |
| `eidos-consents` | `cd eidos-consents && ./mvnw package` | `./mvnw test` |
| `eidos-cdi-ui-backend` | `cd eidos-cdi-ui-backend && ./mvnw package` | `./mvnw test` |

У `shared-library` и `eidos-stage` нет Maven Wrapper — нужен установленный
Maven.

Тесты — модульные, внешние сервисы в них подменяются. Исключение —
`TokenVaultStandTest` в ядре: он проверяет криптоконтур на поднятом стенде и по
умолчанию пропускается:

```bash
cd eidos-core
./mvnw test -Dtest=TokenVaultStandTest -Deidos.stand=true \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5433/eidos_core \
  -Dspring.kafka.bootstrap-servers=localhost:19092 \
  -Deidos.consents.base-url=http://localhost:8086
```

## Запуск сервиса из исходников

Удобный режим разработки — инфраструктура и остальные сервисы в Docker,
изменяемый сервис — локально:

```bash
docker compose up -d postgres mongo kafka openbao keycloak
docker compose stop eidos-core          # если сервис был запущен в Docker
cd eidos-core && ./mvnw spring-boot:run
```

Настройки по умолчанию в `application.properties` рассчитаны на локальный
запуск, но порты стенда на хосте отличаются от стандартных. Передайте их
переменными окружения:

| Сервис стенда | Адрес с хоста | Переменная |
|---|---|---|
| PostgreSQL | `localhost:5433` | `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/<база>` |
| MongoDB | `localhost:27018` | `SPRING_MONGODB_URI=mongodb://localhost:27018/eidos_stage` |
| Kafka | `localhost:19092` | Совпадает с настройкой по умолчанию |
| OpenBao | `localhost:8200` | Совпадает; токен — `EIDOS_CRYPTO_OPENBAO_TOKEN` |
| `eidos-consents` | `localhost:8086` | `EIDOS_CONSENTS_BASE_URL=http://localhost:8086` |
| Keycloak | `localhost:8180` | Совпадает |

Если локальный сервис должен вызываться другими сервисами из Docker, им нужно
указать адрес хоста, например `http://host.docker.internal:8080`.

## Консоли

```bash
cd eidos-cdi-ui
npm install
npm run dev --workspace=apps/admin          # http://localhost:5173
npm run dev --workspace=apps/front-office
```

Проверка типов и сборка:

```bash
npx tsc --noEmit -p apps/admin/tsconfig.json
npm run build --workspace=apps/admin
```

!!! note "Маршруты в режиме разработки"
    Сервер разработки Vite отправляет все запросы `/api` в
    `eidos-cdi-ui-backend` (`http://localhost:8093`). Пути, которые в сборке
    идут через Gravitee, — источники, контракты, Kafka, поиск, конфликты, — в
    этом режиме не работают, пока в `vite.config.ts` не добавлены маршруты на
    Gravitee (`http://localhost:8082/eidos/…`).

Автоматических тестов у консолей пока нет.

## Образы

```bash
docker compose build eidos-core                    # один сервис
docker compose build                               # все
docker compose build --build-arg JAVA_IMAGE=<образ JDK 26>
```

Образ консоли собирается с аргументом `APP` (`admin` или `front-office`).

## Документация

```bash
python3 -m venv .venv && . .venv/bin/activate
pip install -r docs/requirements.txt
mkdocs serve           # http://127.0.0.1:8000, пересборка при изменениях
mkdocs build --strict  # проверка ссылок, как в CI
```

После изменения описаний Золотых записей в `shared-library` обновите
справочник полей:

```bash
python3 tools/docs/generate_field_reference.py
```

После изменения API сервисов обновите спецификации OpenAPI. Их выгружают тесты
`OpenApiSpecTest` в самих сервисах, стенд не нужен:

```bash
tools/docs/export_openapi.sh
```

Файлы ложатся в `docs/reference/openapi/`: `<сервис>.yaml` — спецификация
целиком; у `eidos-core` и `eidos-gateway` ещё `<сервис>-external.yaml` и
`<сервис>-internal.yaml` — внешний и внутренний API по отдельности.
