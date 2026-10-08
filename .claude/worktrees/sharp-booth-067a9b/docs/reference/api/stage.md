# API eidos-stage

| | |
|---|---|
| Базовый адрес | `http://eidos-stage:8081` внутри сети; на стенде `http://localhost:8081` |
| Аутентификация | `/internal/**` — заголовок `X-Admin-Token`; приём — без аутентификации |
| Ошибки | `{"detail": "…"}`; ошибки проверки тела административных запросов — `400` с перечнем `поле: сообщение` |

Без верного `X-Admin-Token` пути `/internal/**` отвечают
`401 {"detail":"Missing or invalid X-Admin-Token"}`.

## Приём

Вызывается шлюзом. Напрямую источникам не публикуется: путь не проверяет,
кто прислал данные.

### POST /api/v1/client-data, POST /api/v1/legal-data

```json
{"data": { "…": "…" }, "source": "<код источника>"}
```

| Код | Значение |
|---|---|
| `200` | `{"status":"SUCCESS"}` |
| `400` | `{"detail":"Malformed request body"}` |
| `422` | Ошибка обработки — [сообщения eidos-stage](../../sources/errors.md#сообщения-eidos-stage) |
| `500` | `{"detail":"Internal error"}` |

## Источники

### GET /internal/api/v1/sources

Список источников:

```json
[{"id": 1, "code": "bank-1", "name": "Банк — АБС", "token": "tk_…", "trustLevel": 8, "enabled": true}]
```

### GET /internal/api/v1/sources/{id}

Источник. `404` — `Source <id> not found`.

### POST /internal/api/v1/sources

```json
{"code": "bank-1", "name": "Банк — АБС", "token": "tk_…", "trustLevel": 8, "enabled": true}
```

| Поле | Обязательно | Описание |
|---|---|---|
| `code` | да | Уникальный код |
| `name` | да | Название |
| `token` | да | Уникальный токен доступа |
| `trustLevel` | да | Целое число |
| `enabled` | нет | По умолчанию `true` |

`201` — созданный источник. `400` — `Source code '<код>' already exists`,
`Token already in use` или незаполненные поля.

### PUT /internal/api/v1/sources/{id}

Тело — как при создании, все обязательные поля передаются заново. Если
`enabled` не передан, признак не меняется.

### DELETE /internal/api/v1/sources/{id}

`204`. Источник с дата-контрактом удалить нельзя — ответ `500`.

## Дата-контракт

Пути контракта содержат `id` источника. Параметр `entityType` — `PERSON`
(по умолчанию) или `LEGAL_ENTITY`.

### GET /internal/api/v1/sources/{sourceId}/contract?entityType={тип}

```json
{
  "id": 3,
  "sourceId": 1,
  "sourceCode": "bank-1",
  "clientIdentifierField": "client_id",
  "version": 16,
  "fields": [
    {
      "id": 41,
      "sourceFieldName": "person.birth_date",
      "targetGrField": "GR_BirthDate",
      "dataType": "DATE",
      "required": true,
      "sourceDateFormat": "dd.MM.yyyy",
      "validationRegex": null,
      "defaultValue": null,
      "ordering": 3,
      "valueMap": {}
    }
  ]
}
```

`404` — `No PERSON contract for source <id>`.

### PUT /internal/api/v1/sources/{sourceId}/contract?entityType={тип}

Создаёт контракт, если его нет, и задаёт поле-идентификатор. Правила полей не
меняет.

```json
{"clientIdentifierField": "client_id"}
```

Ответ — контракт целиком.

### POST /internal/api/v1/sources/{sourceId}/contract/fields?entityType={тип}

Добавляет правило; создаёт контракт, если его нет. Ответ `201` — правило с `id`.

| Поле | Обязательно | Описание |
|---|---|---|
| `sourceFieldName` | да | Путь в данных источника |
| `targetGrField` | да | Поле Золотой записи (`GR_…`) |
| `dataType` | да | `STRING`, `DATE`, `INTEGER`, `BOOLEAN`, `GENDER`, `ARRAY`, `STRUCT` |
| `required` | нет | По умолчанию `false` |
| `sourceDateFormat` | нет | Шаблон даты |
| `validationRegex` | нет | Регулярное выражение |
| `defaultValue` | нет | Значение по умолчанию |
| `ordering` | нет | Порядок отображения, по умолчанию 0 |
| `valueMap` | нет | Таблица значений `{"источник": "запись"}` |

`400` — `Unknown Golden Record field for <тип>: <поле>`.

### PUT /internal/api/v1/sources/{sourceId}/contract/fields/{fieldId}

Заменяет правило целиком; тело — как при добавлении.

### DELETE /internal/api/v1/sources/{sourceId}/contract/fields/{fieldId}

`204`.

Формат контракта — [Формат дата-контракта](../contract-format.md).

## Каталог полей

### GET /internal/api/v1/golden-record-fields?entityType={тип}

Поля Золотой записи для выбранного типа:

```json
[{"jsonName": "GR_Pinfl", "javaName": "grPinfl", "type": "STRING", "required": true, "pattern": "^\\d{14}$"}]
```

`type` — логический тип поля: `STRING`, `DATE`, `GENDER`, `BOOLEAN`,
`INTEGER`, `DECIMAL`, `STRING_ARRAY`, `STRUCT_ARRAY`.

## Статистика приёма

### GET /internal/api/v1/ingest-stats?days={n}

Принято и отклонено по источникам за последние `n` суток, включая текущие
(по умолчанию `n` = 1 — только текущие сутки):

```json
[{"sourceCode": "bank-1", "accepted": 1520, "rejected": 3}]
```

## Служебные пути

### GET /health

`{"status":"ok"}`.
