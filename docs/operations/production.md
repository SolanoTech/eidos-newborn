# Стенд и продуктив

`docker-compose.yml` из репозитория собирает **стенд** — окружение для
знакомства и разработки на одной машине. Для установки на сервер с реальными
данными его нужно доработать. Эта страница перечисляет отличия; пройдите также
[чек-лист безопасности](../security/hardening.md).

## Что отличается

| Область | Стенд | Сервер |
|---|---|---|
| Секреты | Значения по умолчанию, пароль PostgreSQL `postgres`, `admin/admin` в Keycloak и Gravitee | Уникальные секреты из хранилища секретов |
| Сеть | Порты всех сервисов и баз опубликованы на хосте | Наружу — только обратный прокси с TLS |
| Keycloak | Режим разработки, HTTP, адрес `localhost` | Производственный режим, HTTPS, свой домен |
| Консоли | Собраны с адресом Keycloak `http://localhost:8180` | Пересобраны с адресом Keycloak сервера |
| OpenBao | Режим разработки: ключи в памяти, корневой токен | Постоянное хранилище, распечатывание, токен с ограниченной политикой |
| Версии образов | OpenBao и Gravitee — `latest` | Закреплённые версии |
| Ресурсы | Без ограничений | Лимиты памяти и ЦП, параметры JVM |
| Перезапуск | Не настроен для сервисов модуля | `restart: unless-stopped` |
| Журналы | Драйвер Docker по умолчанию | Ротация и централизованный сбор |
| Резервные копии | Нет | По расписанию, с проверкой восстановления |

## Файл переопределений

Изменения удобно собрать в отдельный файл и подключать его вместе с основным —
так основной `docker-compose.yml` можно обновлять из репозитория без
конфликтов:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

Пример `docker-compose.prod.yml` (нужен Docker Compose 2.24 или новее для
`!reset`):

```yaml
x-restart: &restart
  restart: unless-stopped

services:
  postgres:
    <<: *restart
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?задайте POSTGRES_PASSWORD}
    ports: !reset []

  mongo:
    <<: *restart
    ports: !reset []

  kafka:
    <<: *restart
    ports: !reset []

  openbao:
    <<: *restart
    ports: !reset []

  eidos-core:
    <<: *restart
    environment:
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:?}
      JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=75
    mem_limit: 1g
    ports: !reset []

  eidos-stage:
    <<: *restart
    environment:
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:?}
      JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=75
    mem_limit: 1g
    ports: !reset []

  eidos-consents:
    <<: *restart
    environment:
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:?}
    ports: !reset []

  eidos-gateway:
    <<: *restart
    environment:
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:?}
      EIDOS_REGISTRY_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:?}

  eidos-cdi-ui-backend:
    <<: *restart
    environment:
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:?}
    ports: !reset []

  keycloak:
    <<: *restart
    environment:
      KC_DB_PASSWORD: ${POSTGRES_PASSWORD:?}
```

Порты `eidos-gateway`, консолей и Keycloak остаются: их публикует обратный
прокси. Значения лимитов памяти подберите по результатам
[нагрузочной проверки](performance.md).

!!! warning "Пароль существующей базы"
    `POSTGRES_PASSWORD` действует только при первой инициализации тома
    PostgreSQL. Если том уже создан с паролем `postgres`, смените пароль
    командой `ALTER USER postgres WITH PASSWORD '…';` до перезапуска сервисов с
    новым значением.

Лучше всего завести для каждого сервиса собственного пользователя PostgreSQL с
правами только на его базу. `eidos-gateway` читает реестр — ему достаточно
права на чтение базы `eidos_registry`. См. [Базы данных и миграции](databases.md).

## Публикация наружу

Наружу публикуются только:

| Что | Кому нужно | Порт на стенде |
|---|---|---|
| `eidos-gateway` | Системам-источникам и потребителям | 8090 |
| Консоль администратора и фронт-офис | Пользователям | 8088, 8089 |
| Keycloak | Браузерам пользователей консолей | 8180 |

Поставьте перед ними обратный прокси с TLS. `eidos-core`, `eidos-stage`,
`eidos-consents`, `eidos-cdi-ui-backend`, базы данных, брокер и OpenBao
должны быть доступны только во внутренней сети: у части из них нет
аутентификации. См. [Периметр и угрозы](../security/perimeter.md).

## Адрес Keycloak в консолях

Консоли получают адрес Keycloak, realm и клиента **при сборке** из переменных
`VITE_KC_URL`, `VITE_KC_REALM`, `VITE_KC_CLIENT_ID`. По умолчанию адрес —
`http://localhost:8180`, и с другой машины вход работать не будет. Текущий
`Dockerfile` консолей эти переменные не принимает. Добавьте их в стадию
сборки `eidos-cdi-ui/Dockerfile`:

```dockerfile
FROM node:22-alpine AS build
ARG APP=admin
ARG VITE_KC_URL=http://localhost:8180
ARG VITE_KC_REALM=eidos
ARG VITE_KC_CLIENT_ID=eidos-console
ENV VITE_KC_URL=${VITE_KC_URL} \
    VITE_KC_REALM=${VITE_KC_REALM} \
    VITE_KC_CLIENT_ID=${VITE_KC_CLIENT_ID}
WORKDIR /app
COPY . .
RUN npm ci
RUN npm run build -w @eidos/${APP}
```

и соберите консоли с адресом Keycloak, который видят браузеры пользователей:

```bash
docker compose build --build-arg VITE_KC_URL=https://id.example.uz eidos-cdi-ui eidos-cdp-ui
```

В Keycloak добавьте адреса консолей в разрешённые адреса возврата клиента
`eidos-console` — см. [Keycloak](keycloak.md#адреса-консолей).

## Keycloak

На стенде Keycloak запущен командой `start-dev` и работает по HTTP. На сервере:

- запустите его командой `start` с `KC_HOSTNAME`, настройками TLS или работы
  за прокси (`KC_PROXY_HEADERS=xforwarded`);
- задайте собственные учётные данные администратора вместо `admin/admin`;
- удалите учебного пользователя `admin` из realm `eidos` и смените секрет
  клиента `eidos-m2m`.

Подробности — [Keycloak](keycloak.md).

## OpenBao

Режим разработки хранит ключ шифрования ключей в памяти: перезапуск контейнера
его уничтожает, и ядро перестаёт сопоставлять записи. На сервере OpenBao
работает с постоянным хранилищем и токеном с ограниченной политикой — см.
[OpenBao](openbao.md).

## Gravitee APIM

Закрепите версии образов, смените пароль администратора, выделите
Elasticsearch достаточно памяти. Если административный токен меняется, обновите
его и в настройках Gravitee — см. [Gravitee APIM](gravitee.md).

## Kafka

Шлюз подключается к брокеру без аутентификации и шифрования — настройки TLS и
SASL для его потребителей не предусмотрены. Используйте брокер, доступный
только из внутренней сети платформы. `eidos-core` и `eidos-consents`
подключаются через стандартные свойства Spring Kafka (`SPRING_KAFKA_*`) и
могут работать с защищённым брокером, но приём через шлюз — нет.

## Несколько экземпляров сервисов

Платформа проверялась в конфигурации «один экземпляр каждого сервиса».
Если вы запускаете несколько экземпляров:

- `eidos-gateway` — экземпляры делят партиции топиков в одной группе
  потребителей;
- `eidos-stage` и `eidos-cdi-ui-backend` состояния не хранят;
- `eidos-consents` — каждый экземпляр выполняет ежедневный проход, и события
  `expired` публикуются несколько раз. Потребители к этому готовы, см.
  [События о согласиях](../consumers/consent-events.md);
- `eidos-core` — две карточки одного нового клиента, обработанные
  одновременно, могут привести к отказу одной из них из-за уникальности ПИНФЛ
  или ИНН. Повтор отказавшей карточки сольёт её с созданной записью.

## Журналы

Сервисы пишут журналы в стандартный вывод. Настройте ротацию в Docker:

```yaml
x-logging: &logging
  logging:
    driver: json-file
    options:
      max-size: "50m"
      max-file: "5"
```

Журналы могут содержать фрагменты персональных данных — например, значения,
нарушившие контракт. Храните и передавайте их как конфиденциальные. См.
[Персональные данные](../security/personal-data.md).
