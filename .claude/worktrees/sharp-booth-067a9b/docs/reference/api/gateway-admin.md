# API eidos-gateway: администрирование

Внешний API шлюза описан в разделе [Внешний API](external.md). Здесь —
административные пути.

| | |
|---|---|
| Базовый адрес | `http://eidos-gateway:8090` внутри сети; на стенде `http://localhost:8090` |
| Аутентификация | Заголовок `X-Admin-Token` на `/internal/**` |
| Ошибки | `{"detail": "…"}`; ошибки проверки тела — `400` |

Без верного токена — `401 {"detail":"Missing or invalid X-Admin-Token"}`.
Закройте путь `/internal/**` на обратном прокси: он не должен быть доступен
снаружи.

## Потребители Kafka

### GET /internal/api/v1/kafka-config?entityType={тип}

Настройка канала одного типа сущности (`PERSON` по умолчанию,
`LEGAL_ENTITY`):

```json
{"id": 1, "entityType": "PERSON", "bootstrapServers": "kafka:9092", "topic": "client-data", "groupId": "eidos-gateway", "enabled": true}
```

`404` — `Kafka config for <тип> is not set`.

### GET /internal/api/v1/kafka-config/all

Все настроенные каналы, включая выключенные.

### PUT /internal/api/v1/kafka-config

Создаёт или заменяет настройку канала для типа сущности и сразу
перезапускает потребителей.

```json
{"entityType": "LEGAL_ENTITY", "bootstrapServers": "kafka:9092", "topic": "legal-data", "groupId": "eidos-gateway", "enabled": true}
```

| Поле | Обязательно | Описание |
|---|---|---|
| `entityType` | нет | `PERSON` (по умолчанию) или `LEGAL_ENTITY` |
| `bootstrapServers` | да | Адреса брокеров через запятую |
| `topic` | да | Топик |
| `groupId` | да | Группа потребителя |
| `enabled` | да | Включён ли потребитель |

Ответ — сохранённая настройка. Ошибки подключения к брокеру видны только в
журнале шлюза.

## Служебные пути

### GET /health

`{"status":"ok"}`.
