# API eidos-consents

| | |
|---|---|
| Базовый адрес | `http://eidos-consents:8082` внутри сети; на стенде `http://localhost:8086` |
| Аутентификация | **Нет.** Держите сервис во внутренней сети; источникам он доступен через шлюз |
| Ошибки | `{"detail": "…"}`; `404` — не найдено, `400` — неверный параметр, `422` — ошибка проверки тела, `500` — прочее |

Через шлюз эти пути доступны как `/api/v1/consents/**` — см.
[Внешний API](external.md#согласия).

## Формат согласия

```json
{
  "id": "0b8f3c1d-6a2e-4f7b-9d61-3c9e2a7b5f40",
  "client_uuid": "5f0c2b1e-8a4d-4c55-9a8e-2b7d4f1c9e10",
  "start_date": "2026-10-01",
  "end_date": "2027-03-30",
  "type": "pd",
  "source": "bank-1"
}
```

`client_uuid` — идентификатор Золотой записи физлица без префикса `GR_`;
`type` — код типа; даты — `yyyy-MM-dd`, окончание включительно.

## POST /internal/api/v1/consents

```json
{"type": "pd", "client_uuid": "<uuid>", "initial_date": "2026-10-01", "end_date": "2027-03-30", "source_name": "bank-1"}
```

| Поле | Обязательно | Описание |
|---|---|---|
| `type` | да | Код типа — [Перечисления](../enums.md#типы-согласий) |
| `client_uuid` | да | UUID без префикса |
| `initial_date` | да | Начало действия |
| `end_date` | нет | Окончание; по умолчанию +180 дней для `pd` и `bio`, +365 для остальных |
| `source_name` | да | Кто зарегистрировал согласие |

Ответ `201`:

```json
{"data": {согласие}, "detail": "Consent created"}
```

После сохранения публикуется событие `new`.

## GET /internal/api/v1/consents/active/{client_uuid}?type={TYPE}

Последнее по дате начала согласие типа `TYPE`, действующее сегодня. `TYPE` —
**имя** типа: `PERSONAL_DATA`, `BIO`, `MARKETING_SMS`, `MARKETING_PUSH`,
`MARKETING_EMAIL`, `PROFILING`, `THIRD_PARTY`.

| Код | Значение |
|---|---|
| `200` | Согласие |
| `404` | `No active consent of type <TYPE> for client <uuid>` |
| `400` | Не передан `type` или `Invalid value for parameter 'type': …` |

## GET /internal/api/v1/consents/client/{client_uuid}

Сводка по всем семи типам. Обратите внимание: имена полей здесь в camelCase.

```json
[
  {"type": "PERSONAL_DATA", "code": "pd", "active": true, "consentId": "0b8f3c1d-…", "startDate": "2026-10-01", "endDate": "2027-03-30"},
  {"type": "MARKETING_SMS", "code": "mk_sms", "active": false, "consentId": null, "startDate": null, "endDate": null}
]
```

Для каждого типа берётся последнее согласие по дате начала; `active` —
действует ли оно сегодня.

## POST /internal/api/v1/consents/{id}/revoke

Переносит дату окончания на вчерашний день. Ответ — согласие. `404` —
`Consent <id> not found`.

## GET /health

`{"status":"ok"}`.

## События

Формат событий в Kafka — [Сообщения и события Kafka](../kafka-messages.md#consent-events).
