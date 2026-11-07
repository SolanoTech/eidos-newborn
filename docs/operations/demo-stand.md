# Публичный демонстрационный стенд

Урезанная сборка для показа интерфейса: `docker-compose.demo.yml` в корне
репозитория и вспомогательные файлы в `deploy/demo/`. Рассчитана на одну
виртуальную машину и **только на выдуманные данные**.

!!! danger "Настоящие персональные данные недопустимы"
    У внешнего API нет разграничения по источникам: любой токен источника даёт
    поиск по всем Золотым записям. Стенд публичный, и для показа интерфейса
    этого достаточно, а для настоящих ПИНФЛ, паспортов и телефонов — нет.

## Чем отличается от полного стенда

| | Полный стенд | Демо-стенд |
|---|---|---|
| Gravitee APIM | 6 контейнеров | 4: монго, Elasticsearch, шлюз, Management API |
| Консоль управления и портал Gravitee | есть | нет — маршруты заводит скрипт |
| Лицензия Gravitee | монтируется | не монтируется |
| OpenSearch | два узла + Dashboards | один узел |
| Порты на хосте | у каждого сервиса | 80 и 443 у Caddy, Management API на 127.0.0.1 |
| TLS | нет | Let's Encrypt |
| Секреты | значения по умолчанию | обязательны, пустой секрет ломает запуск |

Демо-сборка — 18 контейнеров и **примерно 5,5–6 ГБ** памяти (оценка по
настройкам кучи, не замер). Полный стенд — 21 контейнер.

## Почему Gravitee нужен даже на стенде

Семь административных путей консолей (`/api/v1/admin/sources`,
`…/golden-records`, `…/conflicts` и ещё четыре) проверяет Gravitee: подпись
JWT Keycloak на краю и подстановка служебного `X-Admin-Token` для сервисов.
`eidos-cdi-ui-backend` закрыть их не может — он валидирует JWT, но обслуживает
только дашборд, аудит и согласия.

Первая версия демо-сборки обходилась без Gravitee: эти пути проксировал сам
nginx консолей, подставляя токен, а стенд целиком закрывался
basic-аутентификацией на Caddy. Так делать нельзя, и причина не в экономии
памяти: **консоль сама ставит заголовок `Authorization: Bearer`**, браузер
перестаёт подставлять сохранённые `Basic`-данные, Caddy отвечает `401` — и
окно ввода пароля появляется бесконечно. Ломались все экраны, которые ходят в
API, а не только поиск.

Поэтому Gravitee в составе стенда, а своего барьера у Caddy нет. Кому
показываете стенд — тому и заводите пользователя в Keycloak.

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

Затем заведите маршруты в Gravitee — без этого административные экраны
консолей отвечают `404` или `401`:

```bash
ADMIN_TOKEN=$(grep '^ADMIN_TOKEN=' .env | cut -d= -f2-) python3 keycloak/setup_gravitee.py
```

Скрипт идемпотентен: создаёт семь API типа PROXY, JWT-план с проверкой подписи
по ключам Keycloak, приложение `eidos-console` и подписку на каждый план,
после чего разворачивает API на шлюзе. Management API слушает только
`127.0.0.1:8083`, наружу он не смотрит.

!!! warning "Подписка обязательна"
    JWT-план пропускает только токены, чей claim `azp` совпадает с `client_id`
    подписанного приложения. Без подписки шлюз отвечает `401` даже на
    правильный токен. Скрипт оформляет подписки сам.

Наполните стенд выдуманными карточками — иначе экраны будут пустыми.

Засев обращается к `eidos-stage` и `eidos-gateway` по HTTP, а демо-сборка
порты сервисов не публикует. На время засева их открывает наложение
`docker-compose.seed.yml` — **только на петлевом интерфейсе**, снаружи они
по-прежнему недоступны:

```bash
docker compose -f docker-compose.demo.yml -f deploy/demo/docker-compose.seed.yml \
    up -d eidos-stage eidos-gateway

python3 -I deploy/demo/seed.py --dry-run  # посмотреть, что будет сделано
python3 -I deploy/demo/seed.py            # 40 физлиц, 12 юрлиц, два источника

docker compose -f docker-compose.demo.yml up -d eidos-stage eidos-gateway
```

Последняя команда обязательна: она пересоздаёт оба сервиса без публикации
портов. Проверить, что портов не осталось:

```bash
docker compose -f docker-compose.demo.yml ps --format '{{.Service}} {{.Ports}}'
```

Если скрипт запускают из сети compose, адреса переопределяются переменными
`EIDOS_STAGE_URL` и `EIDOS_GATEWAY_URL`, и наложение не нужно.

Засев создаёт два источника с разным уровнем доверия, дата-контракты на оба
типа сущностей и гонит карточки обычным путём приёма через шлюз. Каждая пятая
карточка приходит повторно от второго источника — так на стенде появляются
слияние, происхождение полей и разбор конфликтов. Данные генерируются от
фиксированного зерна: повторный запуск даёт те же карточки.

## Проверка

```bash
curl -o /dev/null -w '%{http_code}\n' https://eidos-admin.example.org/
curl -o /dev/null -w '%{http_code}\n' https://eidos-auth.example.org/realms/eidos
curl -o /dev/null -w '%{http_code}\n' http://localhost:8082/eidos/sources
```

Первые два — `200`. Третий — `401` от Gravitee: шлюз работает и требует токен.
Если там `404`, маршруты не заведены, прогоните `setup_gravitee.py`. Затем войдите в консоль под
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

- **Консоли управления Gravitee и портала разработчика.** Маршруты заводит
  `setup_gravitee.py`; смотреть их в браузере на стенде негде.
- **Лицензии Gravitee** — платные возможности не подключены.
- **Настоящих источников и потока данных.** Приём работает, но карточки
  кладёт только засев.
- **Производственного режима Keycloak:** `start-dev`, как и на локальном
  стенде. Для продуктива нужен `start` — см. [Стенд и продуктив](production.md).
- **Резервного копирования.** Стенд одноразовый: его быстрее развернуть
  заново, чем восстанавливать.
