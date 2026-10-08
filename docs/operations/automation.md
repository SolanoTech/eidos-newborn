# Настройка через API

Всё, что администратор делает в консоли, можно сделать через административные
API сервисов. Это нужно, когда:

- платформу разворачивают у нескольких заказчиков и настройки должны
  совпадать;
- источники и контракты хотят хранить в Git и применять так же, как код;
- консоли недоступны, например, если не установлен Gravitee.

## Что доступно

| Настройка | API | Справочник |
|---|---|---|
| Источники | `eidos-stage` `/internal/api/v1/sources` | [API eidos-stage](../reference/api/stage.md#источники) |
| Дата-контракты | `eidos-stage` `/internal/api/v1/sources/{id}/contract` | [API eidos-stage](../reference/api/stage.md#дата-контракт) |
| Каталог полей Золотой записи | `eidos-stage` `/internal/api/v1/golden-record-fields` | [API eidos-stage](../reference/api/stage.md#каталог-полей) |
| Потребители Kafka | `eidos-gateway` `/internal/api/v1/kafka-config` | [API eidos-gateway](../reference/api/gateway-admin.md) |
| Источники ядра | SQL в базе `eidos_core` | [Регистрация в ядре](../sources/registration.md#регистрация-в-ядре) |

Административные API закрыты заголовком `X-Admin-Token` и доступны только из
внутренней сети. На стенде `eidos-stage` и `eidos-gateway` опубликованы на
портах 8081 и 8090 хоста.

## Источники и контракты как код

Опишите источники в JSON-файле и храните его в репозитории конфигурации:

```json
{
  "sources": [
    {
      "code": "bank-1",
      "name": "Банк — АБС",
      "trustLevel": 8,
      "tokenEnv": "BANK1_TOKEN",
      "contracts": {
        "PERSON": {
          "clientIdentifierField": "client_id",
          "fields": [
            {"sourceFieldName": "pinfl", "targetGrField": "GR_Pinfl", "dataType": "STRING", "required": true, "validationRegex": "^\\d{14}$"},
            {"sourceFieldName": "birth_date", "targetGrField": "GR_BirthDate", "dataType": "DATE", "required": true, "sourceDateFormat": "dd.MM.yyyy"},
            {"sourceFieldName": "gender", "targetGrField": "GR_Gender", "dataType": "GENDER", "required": true, "valueMap": {"1": "M", "2": "F"}}
          ]
        }
      }
    }
  ],
  "kafka": [
    {"entityType": "PERSON", "bootstrapServers": "kafka:9092", "topic": "client-data", "groupId": "eidos-gateway", "enabled": true}
  ]
}
```

Токены в файл не записываются: поле `tokenEnv` называет переменную окружения,
из которой скрипт возьмёт токен.

Скрипт ниже приводит платформу к описанию: создаёт или обновляет источники,
добавляет недостающие правила контракта, обновляет изменённые и удаляет
лишние, настраивает потребителей Kafka. Его можно запускать повторно.

```python
#!/usr/bin/env python3
"""Приводит источники, контракты и потребителей Kafka Eidos к описанию из JSON."""
import json
import os
import sys
import urllib.error
import urllib.request

STAGE = os.environ.get("STAGE_URL", "http://localhost:8081")
GATEWAY = os.environ.get("GATEWAY_URL", "http://localhost:8090")
ADMIN_TOKEN = os.environ["ADMIN_TOKEN"]


def call(base, method, path, body=None):
    request = urllib.request.Request(
        base + path, method=method,
        data=None if body is None else json.dumps(body).encode("utf-8"),
        headers={"X-Admin-Token": ADMIN_TOKEN, "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            raw = response.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        sys.exit(f"{method} {path}: HTTP {error.code} {error.read().decode()}")


def normalized(field):
    return {
        "sourceFieldName": field["sourceFieldName"],
        "targetGrField": field["targetGrField"],
        "dataType": field["dataType"],
        "required": bool(field.get("required")),
        "sourceDateFormat": field.get("sourceDateFormat"),
        "validationRegex": field.get("validationRegex"),
        "defaultValue": field.get("defaultValue"),
        "valueMap": field.get("valueMap") or {},
    }


def key(field):
    return field["sourceFieldName"], field["targetGrField"]


def apply_contract(source_id, entity_type, spec):
    base = f"/internal/api/v1/sources/{source_id}/contract"
    query = f"?entityType={entity_type}"
    current = call(STAGE, "PUT", base + query,
                   {"clientIdentifierField": spec["clientIdentifierField"]})
    have = {key(f): f for f in current["fields"]}
    want = {key(f): normalized(f) for f in spec["fields"]}
    for k, field in have.items():
        if k not in want:
            call(STAGE, "DELETE", f"{base}/fields/{field['id']}")
    for order, (k, field) in enumerate(want.items()):
        body = dict(field, ordering=order)
        if k not in have:
            call(STAGE, "POST", f"{base}/fields{query}", body)
        elif normalized(have[k]) != field or have[k].get("ordering") != order:
            call(STAGE, "PUT", f"{base}/fields/{have[k]['id']}", body)


def apply_source(spec):
    existing = {s["code"]: s for s in call(STAGE, "GET", "/internal/api/v1/sources")}
    body = {"code": spec["code"], "name": spec["name"], "trustLevel": spec["trustLevel"],
            "token": os.environ[spec["tokenEnv"]], "enabled": True}
    if spec["code"] in existing:
        source = call(STAGE, "PUT", f"/internal/api/v1/sources/{existing[spec['code']]['id']}", body)
    else:
        source = call(STAGE, "POST", "/internal/api/v1/sources", body)
    for entity_type, contract in spec.get("contracts", {}).items():
        apply_contract(source["id"], entity_type, contract)


if __name__ == "__main__":
    with open(sys.argv[1], encoding="utf-8") as file:
        spec = json.load(file)
    for source_spec in spec.get("sources", []):
        apply_source(source_spec)
        print(f"✓ источник {source_spec['code']}")
    for channel in spec.get("kafka", []):
        call(GATEWAY, "PUT", "/internal/api/v1/kafka-config", channel)
        print(f"✓ Kafka {channel['entityType']}: {channel['topic']}")
    print("\nДобавьте источники в ядро (база eidos_core):")
    for s in spec.get("sources", []):
        print(f"INSERT INTO source (source_name, trust_level) VALUES ('{s['code']}', {s['trustLevel']}) "
              "ON CONFLICT (source_name) DO UPDATE SET trust_level = EXCLUDED.trust_level;")
```

Запуск:

```bash
export ADMIN_TOKEN=… BANK1_TOKEN=…
python3 apply_sources.py sources.json
```

Скрипт печатает SQL для таблицы источников ядра — выполните его в базе
`eidos_core` (на стенде — через `docker compose exec -T postgres psql -U postgres -d eidos_core`).

!!! warning "Изменения применяются сразу"
    Удалённое из описания правило контракта перестаёт действовать сразу после
    запуска скрипта. Проверяйте изменения на стенде, прежде чем применять их к
    рабочей платформе.

## Выгрузить текущие настройки

```bash
curl -s -H "X-Admin-Token: $ADMIN_TOKEN" http://localhost:8081/internal/api/v1/sources
curl -s -H "X-Admin-Token: $ADMIN_TOKEN" "http://localhost:8081/internal/api/v1/sources/1/contract?entityType=PERSON"
curl -s -H "X-Admin-Token: $ADMIN_TOKEN" http://localhost:8090/internal/api/v1/kafka-config/all
```

Ответ контракта содержит поля `id` и `ordering`; остальные атрибуты правил
совпадают с форматом описания выше.

Ещё один пример автоматизации — `loadtest/setup.py` в репозитории
`eidos-infrastructure`: он заводит тестовый источник, контракт и потребителя
Kafka для нагрузочного теста.
