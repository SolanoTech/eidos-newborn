# API eidos-cdi-ui-backend

Backend-for-frontend консолей: сводки, журнал действий и согласия из карточки
клиента. Остальные данные консоли получают через Gravitee — см.
[Gravitee APIM](../../operations/gravitee.md#что-идёт-через-gravitee).

| | |
|---|---|
| Базовый адрес | `http://eidos-cdi-ui-backend:8083` внутри сети; на стенде `http://localhost:8093`; консоли обращаются через свой nginx по пути `/api/` |
| Аутентификация | `Authorization: Bearer <токен Keycloak>`; пути `/api/v1/admin/**` требуют роль `ADMIN` |
| Ошибки | `401` без тела — нет или неверный токен; `403 {"detail":"Access denied"}` — нет роли; ошибки сервисов передаются со своим кодом и телом |

## GET /api/v1/admin/dashboard

Сводка для дашборда:

```json
{
  "metrics": { "grTotal": 15230, "tentativeTotal": 41, "…": "…" },
  "sources": [{"id": 1, "code": "bank-1", "name": "Банк — АБС", "token": "tk_…", "trustLevel": 8, "enabled": true}],
  "activity": [{"sourceCode": "bank-1", "accepted": 1520, "rejected": 3}]
}
```

- `metrics` — [метрики ядра](core.md#метрики);
- `sources` — реестр источников, **включая токены**;
- `activity` — статистика приёма за текущие сутки.

## GET /api/v1/admin/audit?limit={n}

Последние записи журнала, новые первыми. `limit` — от 1 до 500, по умолчанию
100.

```json
[{"id": "…", "actor": "admin", "action": "Выдано согласие pd", "details": "Клиент 5f0c2b1e-…", "category": "ok", "createdAt": "2026-10-04T12:01:33.120Z"}]
```

`category` — оформление в консоли: `gold`, `ok`, `warn`, `bad`.

## GET /api/v1/admin/consents/{clientUuid}

Сводка согласий клиента — как [в сервисе согласий](consents.md#get-internalapiv1consentsclientclient_uuid).
`clientUuid` — UUID без префикса `GR_`.

## POST /api/v1/admin/consents/grant

```json
{"clientUuid": "5f0c2b1e-8a4d-4c55-9a8e-2b7d4f1c9e10", "type": "mk_sms"}
```

Выдаёт согласие с сегодняшней даты на срок по умолчанию; источник —
`cdp-console`. Ответ `{"status":"GRANTED"}`. Записывается в журнал.

## POST /api/v1/admin/consents/{consentId}/revoke

Отзывает согласие. Ответ `{"status":"REVOKED"}`. Записывается в журнал.

## GET /health

Без аутентификации. `{"status":"ok"}`.
