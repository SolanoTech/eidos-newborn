# Тестирование Eidos: end-to-end и нагрузка

Полная проверка потока: **пуш карточки → Kafka → gateway (авторизация + валидация контракта) → stage (сырьё в Mongo + маппинг) → core (Golden Record) → отображение в UI**.

Здесь три вещи:
1. **Шаги ручного тестирования** (ниже) — от инфраструктуры до каждого экрана.
2. **Моки данных** — [`mocks.py`](mocks.py) (валидные и невалидные карточки).
3. **Нагрузочный скрипт** — [`loadtest.py`](loadtest.py) с замером `push → Golden Record`.

Вспомогательное: [`setup.py`](setup.py) заводит источник `loadtest` + контракт + включает Kafka-консьюмер.

---

## 0. Что нужно один раз

- Docker Desktop запущен.
- Python 3.10+ на хосте. Для режима Kafka: `pip install -r requirements.txt`.
- **Важно:** в `docker-compose.yml` добавлен брокер **Redpanda** (Kafka-совместимый, сервис `kafka`). Он слушает `kafka:9092` внутри сети и `localhost:19092` с хоста. Если у вас уже есть свой кластер — можно не поднимать наш, а в `setup.py` указать свой `KAFKA_INTERNAL`, и в `loadtest.py` передать `--bootstrap <хост:порт>`.

## 1. Поднять систему

```bash
cd /Users/skaydi/Projects/Solano/eidos
docker compose up -d
docker compose ps          # дождаться, что kafka/postgres/mongo — healthy, сервисы — Up
```

## 2. Подготовить источник, контракт и Kafka

```bash
cd loadtest
python3 setup.py
```

Скрипт (идемпотентно):
- заведёт источник `loadtest` (токен `tk_loadtest_secret_token_0001`, trust 8) в реестре;
- наполнит дата-контракт: `client_id` как идентификатор клиента + 15 полей, покрывающих все обязательные поля Золотой записи (телефон, ФИО, ПИНФЛ, пол, даты, паспорт, гражданство, e-mail, адрес);
- зарегистрирует источники в **core.source** (trust-уровни) — core держит собственную таблицу источников и отклоняет неизвестные; сидинг через `seed_core_sources.sql`;
- включит Kafka-консьюмер gateway на топик `client-data` (перезапуск на лету);
- запишет `config.json` для остальных скриптов.

Если docker недоступен из Python, core-источники засеваются вручную:
```bash
docker compose exec -T postgres psql -U postgres -d eidos_core -f - < seed_core_sources.sql
```

Проверить, что консьюмер поднялся:
```bash
docker compose logs eidos-gateway --since 1m | grep -i "Kafka consumer started"
```

---

## 3. Сквозной smoke-тест (одна карточка)

### 3a. Через Kafka
```bash
python3 loadtest.py --mode kafka --count 1 --concurrency 1
```
Ожидаемо: `Приземлилось в Golden: 1`, задержка в пределах секунды-двух.

Проверить руками цепочку:
```bash
# сырьё осело в Mongo
docker compose exec -T mongo mongosh eidos_stage --quiet \
  --eval 'db.raw_client_data.countDocuments({source:"loadtest"})'

# статистика приёма stage (accepted инкрементнулся)
curl -s http://localhost:8081/internal/api/v1/ingest-stats -H "X-Admin-Token: $ADMIN_TOKEN"
```

### 3b. Через REST (gateway напрямую, без Kafka)
```bash
python3 loadtest.py --mode rest --count 1 --concurrency 1
```

### 3c. Ручной curl (посмотреть тело)
```bash
python3 mocks.py -n 1 --envelope > /tmp/card.json
curl -s -X POST http://localhost:8090/api/v1/client-data \
  -H 'Content-Type: application/json' \
  -H 'X-Access-Token: tk_loadtest_secret_token_0001' \
  --data-binary @/tmp/card.json
# client_id из карточки → ищем в core:
CID=$(python3 -c 'import json;print(json.load(open("/tmp/card.json"))["data"]["client_id"])')
curl -s "http://localhost:8080/api/v1/external/golden-records/search/source-id?source=loadtest&source_id=$CID" | head -c 400
```

---

## 4. Негативные проверки (валидация контракта на gateway)

```bash
python3 mocks.py --invalid --envelope
```
Отправить каждую невалидную карточку на gateway — все должны вернуть **422** и **не** долететь до core:
```bash
python3 - <<'PY'
import json, urllib.request, urllib.error, mocks
for desc, card in mocks.make_invalid_cards():
    body = json.dumps(mocks.make_envelope(card)).encode()
    req = urllib.request.Request("http://localhost:8090/api/v1/client-data",
        data=body, method="POST",
        headers={"Content-Type":"application/json","X-Access-Token":"tk_loadtest_secret_token_0001"})
    try:
        urllib.request.urlopen(req); print("НЕОЖИДАННО ПРИНЯТО:", desc)
    except urllib.error.HTTPError as e:
        print(f"{e.code}  ← {desc}")
PY
```
Ожидаемо: `422 ← ...` на каждой строке. Отклонённые уходят в `rejected` статистику gateway.

---

## 5. Проверка интерфейсов

### 5a. Админ-консоль — http://localhost:8088  (логин `admin` / `admin`)
- **Дашборд** — метрики (число записей, конфликты, приём) обновились после пушей.
- **Источники** — виден `loadtest` и остальные; можно создать/выключить источник.
- **Конструктор** — открыть контракт `loadtest`, увидеть 15 полей и маппинг на GR.
- **Kafka-шлюз** — конфигурация топика `client-data`, брокер `kafka:9092`, статус.
- **Поиск** — найти клиента (по ПИНФЛ/паспорту/ФИО из отправленных карточек) → открыть карточку: данные, источники полей (провенанс), внешние ID.
- **Конфликты (серые зоны)** — см. п.6.
- **Аудит-лог** — записи входа и действий.
- **Пользователи и роли**, **API-ключи** — CRUD.
- Демо-экраны (с пометкой «демо»): Схема событий, Инциденты качества, Интеграции, Категории согласий, GDPR.

### 5b. CDP-консоль (front office) — http://localhost:8089  (логин `admin` / `admin`)
- **Поиск** → выбрать клиента → **Карточка 360**: детали Золотой записи, провенанс полей, внешние ID.
- **Согласия** — тогглы 7 типов: выдать/отозвать (реальный eidos-consents, пишется в аудит).
- **Экспорт** — карточка/JSON.
- Демо-блоки (с пометкой): ML-сегменты и скоры, Nexus-граф, Офферы, Коммуникации, AI-чат, история ID.

## 6. Конфликт данных (серая зона) — ручной сценарий

Одного и того же человека шлём из **двух источников с разным доверием**, чтобы менее доверенный источник ушёл в tentative-конфликт:

```bash
python3 - <<'PY'
import json, urllib.request, mocks
card = mocks.make_card("CONFLICT-1")        # опорная карточка
def push(src, token, c):
    body=json.dumps(mocks.make_envelope(c, src)).encode()
    req=urllib.request.Request("http://localhost:8090/api/v1/client-data", data=body, method="POST",
        headers={"Content-Type":"application/json","X-Access-Token":token})
    urllib.request.urlopen(req)
# 1) высокодоверенный источник loadtest (trust 8)
push("loadtest","tk_loadtest_secret_token_0001", card)
# 2) тот же ПИНФЛ, но другой e-mail/адрес из менее доверенного источника bank-2 (trust 1)
c2=dict(card); c2["email"]="changed@example.uz"; c2["address"]="ИЗМЕНЁННЫЙ АДРЕС"
push("bank-2","tk_E8Aa6qMm9QN3MvPadFASFf6BAs_6geYh", c2)
print("отправлено; смотри 'Конфликты' в админ-консоли")
PY
```
> Примечание: у источника `bank-2` в реестре свой контракт. Если он не покрывает все поля — используйте вместо второго пуша тот же `loadtest`, изменив значения: конфликт возникнет при расхождении входящего снимка с текущим. Разбор — на экране **Конфликты**: принять/отклонить.

### 6b. Автоматический merge-тест по trust-уровню

Готовый скрипт [`merge_test.py`](merge_test.py) проверяет перезапись поля источником с более высоким trust (bank1=10 > loadtest=8) по ПИНФЛ `61157669271732`:

```bash
python3 setup.py --source bank-1 --reset-fields   # один раз: дать bank-1 полный контракт
python3 merge_test.py
```

Скрипт: чистит тестовую запись → loadtest создаёт её (все поля «loadtest») → bank-1 присылает того же человека (те же ФИО+дата+ПИНФЛ, иначе точный поиск core не сматчит), изменив только телефон/e-mail → проверяет, что **только изменённые** поля переключились на bank-1, а неизменные (имя, фамилия, паспорт) остались за loadtest.

> **По-полевой merge**: при более высоком trust core перезаписывает поле и переключает провенанс **только если значение реально изменилось** (непустое и отличается от текущего). Неизменные и непришедшие поля остаются за прежним источником — так видно смешанное происхождение по полям.
>
> Ключевой нюанс: идентифицирующие поля (**фамилия, имя, дата рождения, ПИНФЛ**) должны совпадать — по ним core находит существующую запись. Равный trust → не перезапись, а серая зона (tentative).

Настроить полный контракт можно для любого источника: `python3 setup.py --source <code> --reset-fields`.

---

## 6b. Юридические лица (мерчанты)

Вторая вертикаль ходит **своими каналами** — тип сущности определяется каналом,
а не полем в payload: источник `entity_type` не присылает и прислать не может.

| Канал | Физлица | Юрлица |
|---|---|---|
| REST (gateway) | `/api/v1/client-data` | `/api/v1/legal-data` |
| Kafka | топик `client-data` | топик `legal-data` |

Подготовка (идемпотентно): контракт `LEGAL_ENTITY` и топик юрлиц

```bash
python3 setup.py --legal                          # для источника loadtest
python3 setup.py --source bank-1 --legal --reset-fields   # для merge-теста
```

Нагрузка мерчантами — тот же скрипт с `--entity legal`:

```bash
python3 loadtest.py --entity legal --count 200 --concurrency 8   # Kafka
python3 loadtest.py --entity legal --mode rest --count 100       # REST
```

Приземление юрлица проверяется **по ИНН** (`search/inn`), а не по внешнему
идентификатору источника: ИНН — самодостаточный ключ.

Частичный merge по trust для юрлиц:

```bash
python3 merge_test_legal.py
```

Скрипт чистит тестового мерчанта, создаёт его от `loadtest` (trust 8), затем
шлёт **тот же ИНН** от `bank-1` (trust 10) с другим названием и e-mail и
проверяет, что изменённые поля перешли к bank-1, неизменные остались за
loadtest, а смена названия дала merge, а не дубликат.

> **Идентификаторы уникальны на прогон.** `client_id` / `merchant_id` содержат
> случайный префикс запуска. Без него повторный прогон попадал бы в записи
> предыдущего: core находит их по external id и делает merge — тест мерил бы
> повторный поиск вместо приёма.

---

## 7. Нагрузочный тест и замер времени

```bash
# 200 карточек, 8 воркеров, через Kafka
python3 loadtest.py --count 200 --concurrency 8

# 500 через REST, 16 воркеров
python3 loadtest.py --mode rest --count 500 --concurrency 16

# держать ~50 карточек/сек, всего 1000
python3 loadtest.py --count 1000 --rate 50 --concurrency 16
```

Скрипт печатает: сколько приземлилось, пропускную способность (карточек/с) и распределение задержки **push → Golden Record** (min/avg/p50/p90/p95/p99/max).

Параметры: `--timeout` (макс. ожидание одной карточки, по умолч. 60 c), `--interval` (период поллинга core, 0.25 c), `--bootstrap` (свой брокер).

---

## Траблшутинг

| Симптом | Причина / решение |
|---|---|
| `Нет config.json` | Сначала `python3 setup.py`. |
| Kafka: карточки не приземляются | Консьюмер не поднят: `docker compose logs eidos-gateway \| grep -i kafka`; проверить, что `kafka` healthy и `setup.py` включил конфиг. |
| Режим kafka: ImportError | `pip install -r requirements.txt`. |
| 401 при пуше | Неверный `X-Access-Token`; источник выключен/удалён — перезапустить `setup.py`. |
| 422 на валидной карточке | Контракт не покрывает все обязательные поля — перезапустить `setup.py`; проверить формат дат `dd.MM.yyyy`. |
| Запись не находится в core | Увеличить `--timeout`; проверить `docker compose logs eidos-stage --since 2m`. |

## Очистка тестовых данных

```bash
# сырьё в Mongo
docker compose exec -T mongo mongosh eidos_stage --quiet --eval 'db.raw_client_data.deleteMany({source:"loadtest"})'
# Золотые записи и external-id по источнику loadtest — по вашему усмотрению в БД eidos_core
```
