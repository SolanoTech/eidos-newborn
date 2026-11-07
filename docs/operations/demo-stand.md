# Публичный демонстрационный стенд

Урезанная сборка для показа интерфейса: `docker-compose.demo.yml` в корне
репозитория и вспомогательные файлы в `deploy/demo/`. Рассчитана на одну
виртуальную машину и **только на выдуманные данные**.

!!! danger "Настоящие персональные данные недопустимы"
    У внешнего API нет разграничения по источникам: любой токен источника даёт
    поиск по всем Золотым записям. В демо-сборке вместо Gravitee стоит более
    простой барьер (см. ниже). Этого достаточно для показа интерфейса и
    недостаточно для настоящих ПИНФЛ, паспортов и телефонов.

## Чем отличается от полного стенда

| | Полный стенд | Демо-стенд |
|---|---|---|
| Gravitee APIM (6 контейнеров) | есть | **нет** |
| OpenSearch | два узла + Dashboards | один узел |
| Порты на хосте | у каждого сервиса | только 80 и 443 у Caddy |
| TLS | нет | Let's Encrypt |
| Секреты | значения по умолчанию | обязательны, пустой секрет ломает запуск |

Отказ от Gravitee экономит около 2,5–3 ГБ памяти. Полный состав — 15
контейнеров и примерно 6,5–7 ГБ; демо-сборка — 14 контейнеров и
**примерно 3,5–4 ГБ** (оценка по настройкам кучи, не замер).

## Чем заменён Gravitee и почему стенд закрыт паролем

Административные пути консолей (`/api/v1/admin/sources`, `…/golden-records`,
`…/conflicts` и ещё четыре) в штатной схеме идут через Gravitee: он проверяет
JWT Keycloak и подставляет служебный `X-Admin-Token`. В демо-сборке их
проксирует сам nginx консолей (`deploy/demo/nginx-console.conf`), подставляя
токен из переменной окружения.

**Следствие: JWT пользователя на этих путях не проверяется.** `eidos-cdi-ui-backend`
проверяет его сам, но закрывает только дашборд, аудит и согласия — остальные
семь путей не покрывает. Поэтому весь стенд закрыт basic-аутентификацией на
Caddy: без неё админские пути вызвал бы любой желающий.

Снимать basicauth можно **только** вместе с возвратом Gravitee: поднять
`gravitee/docker-compose-apim.yml`, вернуть консолям штатный `nginx.conf` и
прогнать `keycloak/setup_gravitee.py`.

## Что нужно подготовить

**Виртуальная машина.** 4 vCPU и 8 ГБ памяти, 40 ГБ диска, Docker и Docker
Compose. В 4 ГБ стенд тоже уложится, но кучи JVM придётся ограничивать через
`JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=50`.

**Три имени DNS на один IP.** Две консоли и Keycloak живут каждый на своём
хосте — так SPA не надо пересобирать под базовый путь. Бесплатно это
закрывается, например, пятью записями DuckDNS; для показа заказчикам лучше
обычный домен.

```
eidos-admin.example.org   →  IP машины
eidos-cdp.example.org     →  IP машины
eidos-auth.example.org    →  IP машины
```

**Открытые порты 80 и 443.** Порт 80 нужен Let's Encrypt для проверки
владения именем.

## Подготовка машины

Проверено на Ubuntu 24.04 LTS. Команды — от пользователя с `sudo`.

**Ядро под OpenSearch.** Без этого OpenSearch не стартует вовсе: ему нужно
не меньше 262144 областей отображения памяти, а по умолчанию в Linux их
65530.

```bash
echo 'vm.max_map_count=262144' | sudo tee /etc/sysctl.d/99-opensearch.conf
sudo sysctl --system
```

**Подкачка.** На дешёвых виртуальных машинах диск медленный, но подкачка
дешевле, чем падение контейнера по нехватке памяти: пики при сборке образов
гасятся именно ею.

```bash
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

**Брандмауэр.** Наружу нужны только 22, 80 и 443: порты сервисов демо-сборка
не публикует, но межсетевой экран — второй рубеж на случай ошибки в compose.

```bash
sudo ufw default deny incoming && sudo ufw default allow outgoing
sudo ufw allow 22/tcp && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp
sudo ufw enable
```

**Docker.**

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"   # перелогиниться, чтобы группа применилась
```

## Развёртывание

```bash
git clone <репозиторий> && cd eidos-newborn
cp deploy/demo/.env.demo.example .env
cp deploy/demo/caddy.env.example deploy/demo/caddy.env
```

Заполните `.env`: три имени, адрес для Let's Encrypt, пароли и токены.
Значений по умолчанию нет намеренно — пустой секрет должен ломать запуск.
Сгенерировать разом:

```bash
{
  echo "POSTGRES_PASSWORD=$(openssl rand -hex 16)"
  echo "ADMIN_TOKEN=$(openssl rand -hex 24)"
  echo "OPENBAO_TOKEN=$(openssl rand -hex 16)"
  echo "DEMO_SOURCE_TOKEN=tk_$(openssl rand -hex 12)"
  echo "KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -hex 16)"
  echo "DEMO_CONSOLE_PASSWORD=$(openssl rand -hex 12)"
  echo "OPENSEARCH_INITIAL_ADMIN_PASSWORD=Eidos@$(openssl rand -hex 8)Aa1"
}
```

Пароль OpenSearch должен содержать заглавную, строчную, цифру и спецсимвол —
шаблон это обеспечивает. Спецсимвол взят `@`, а не `!`: в интерактивном bash
`!` запускает подстановку из истории команд, и строка искажается до запуска.

Хеш пароля для входа на стенд положите в `deploy/demo/caddy.env` — отдельным
файлом потому, что bcrypt содержит символы `$`, а значения из `environment`
Compose раскрывает.

```bash
docker run --rm caddy:2-alpine caddy hash-password --plaintext 'пароль-для-показа'
```

Сборка и запуск:

```bash
docker compose -f docker-compose.demo.yml up -d --build
```

Сборка консолей впекает в бандл адрес Keycloak из `DEMO_AUTH_HOST`. Менять имя
потом — значит пересобирать образы консолей.

Затем настройте realm под публичные имена: в `keycloak/realm-eidos.json`
прописан только `localhost`, и вход извне без этого шага даёт
`Invalid parameter: redirect_uri`.

```bash
./deploy/demo/setup-keycloak.sh
```

Скрипт добавляет публичные адреса в `redirectUris` и `webOrigins` и заменяет
пароль пользователя консолей на `DEMO_CONSOLE_PASSWORD`. **Это обязательно:**
пароль из `realm-eidos.json` лежит в открытом репозитории.

Наполните стенд выдуманными карточками — иначе экраны будут пустыми:

```bash
python3 -I deploy/demo/seed.py            # 40 физлиц, 12 юрлиц, два источника
python3 -I deploy/demo/seed.py --dry-run  # посмотреть, что будет сделано
```

Засев создаёт два источника с разным уровнем доверия, дата-контракты на оба
типа сущностей и гонит карточки обычным путём приёма через шлюз. Каждая пятая
карточка приходит повторно от второго источника — так на стенде появляются
слияние, происхождение полей и разбор конфликтов. Данные генерируются от
фиксированного зерна: повторный запуск даёт те же карточки.

## Проверка

```bash
curl -u demo:<пароль> -o /dev/null -w '%{http_code}\n' https://eidos-admin.example.org/
curl -o /dev/null -w '%{http_code}\n' https://eidos-auth.example.org/realms/eidos
```

Ожидается `200` в обоих случаях. Затем войдите в консоль под
`DEMO_CONSOLE_USER` и проверьте дашборд, поиск, источники и очередь конфликтов.

Экран «Доступ» в административной консоли ссылается на консоль администратора
Keycloak — снаружи эта ссылка не работает намеренно: Caddy отдаёт `404` на
`/admin`, доступ к ней только из внутренней сети машины.

## После перезапуска машины

OpenBao на стенде работает в режиме разработки и держит KEK в памяти. После
перезапуска контейнера ключ слепого индекса в базе больше не разворачивается,
и приём с точным поиском отвечают `Key store operation failed`. Восстановление
— по [процедуре OpenBao](openbao.md#стенд-режим-разработки):

```bash
docker compose -f docker-compose.demo.yml exec -T postgres psql -U postgres -d eidos_core -c \
  "DELETE FROM vault.pd_blind_index_key; UPDATE golden_record SET pd_bi_key_version = NULL;"
docker compose -f docker-compose.demo.yml restart eidos-core
```

Если поисковый индекс отстал от базы, поднимите ядро один раз с
`EIDOS_SEARCH_REINDEX_ON_STARTUP=true`.

## Чего на стенде нет

- **Gravitee** и проверки JWT на краю — вместо неё пароль на входе.
- **Настоящих источников и потока данных.** Приём работает, но карточки
  кладёт только засев.
- **Производственного режима Keycloak:** `start-dev`, как и на локальном
  стенде. Для продуктива нужен `start` — см. [Стенд и продуктив](production.md).
- **Резервного копирования.** Стенд одноразовый: его быстрее развернуть
  заново, чем восстанавливать.
