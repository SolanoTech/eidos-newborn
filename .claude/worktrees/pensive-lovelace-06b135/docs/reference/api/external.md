# Внешний API

Внешний API предоставляет `eidos-gateway` — единственный сервис, к которому
обращаются системы-источники и потребители.

| | |
|---|---|
| Базовый адрес | Адрес `eidos-gateway`; на стенде `http://localhost:8090` |
| Аутентификация | Заголовок `X-Access-Token: <токен источника>` на всех путях, кроме `/health` |
| Формат | JSON в UTF-8 |
| Ошибки | `{"detail": "…"}` — см. [Коды ответов и ошибки](../errors.md) |

Ответы на отсутствующий или неизвестный токен:

| Код | Тело |
|---|---|
| `401` | `{"detail":"Missing X-Access-Token"}` |
| `401` | `{"detail":"Invalid access token"}` |

Запросы к Золотым записям и согласиям шлюз передаёт сервисам `eidos-core` и
`eidos-consents` и возвращает их ответы без изменений. Заголовки
`X-Access-Token` и `X-Admin-Token` дальше шлюза не передаются.

## Приём данных

### POST /api/v1/client-data

Приём карточки физлица.

```json
{"data": { "…": "поля в формате источника" }}
```

Поле `source` в теле не нужно: шлюз записывает в него код источника из
токена.

| Код | Тело | Значение |
|---|---|---|
| `200` | `{"status":"SUCCESS"}` | Карточка обработана ядром |
| `422` | `{"detail":"No PERSON data contract configured for source <код>"}` | Нет контракта |
| `422` | `{"detail":"Malformed client-data body: …"}` | Тело не является JSON-объектом |
| `422` | `{"detail":"Client data failed contract validation: …"}` | Нарушения контракта через `; ` |
| `422` | `{"detail":"…for source=<код>…"}` | Отказ `eidos-stage` или ядра |
| `500` | `{"detail":"Internal error"}` | Сбой шлюза или недоступность `eidos-stage` |

Все сообщения — [Ошибки, повторы, идемпотентность](../../sources/errors.md).

### POST /api/v1/legal-data

Приём карточки юрлица. Тело и ответы — как у `/api/v1/client-data`; в
сообщениях тип сущности `LEGAL_ENTITY`.

## Золотые записи

Пути `/api/v1/external/golden-records/**` передаются в `eidos-core` без
изменений, с любым методом.

### GET /api/v1/external/golden-records/search/accurate

Точный поиск физлица.

| Параметр | Обязателен | Описание |
|---|---|---|
| `lastName` | да | Фамилия |
| `firstName` | да | Имя |
| `birthDate` | да | Дата рождения, `yyyy-MM-dd` |
| `pinfl` | одно из двух | ПИНФЛ; если задан, паспорт не используется |
| `passport` | одно из двух | Серия и номер паспорта |

| Код | Значение |
|---|---|
| `200` | Золотая запись — [формат](../../consumers/search.md#формат-ответа) |
| `404` | `{"detail":"Golden Record Not Found"}` |
| `422` | `{"detail":"Passport or PINFL required for accurate search"}` |
| `500` | Не передан обязательный параметр или неверный формат даты |

### GET /api/v1/external/golden-records/search/source-id

Поиск по идентификатору клиента в системе-источнике.

| Параметр | Описание |
|---|---|
| `source` | Код источника |
| `source_id` | Идентификатор клиента в источнике |

| Код | Значение |
|---|---|
| `200` | Золотая запись |
| `404` | `{"detail":"Source Not Found"}` или `{"detail":"Golden Record Not Found"}` |

### POST /api/v1/external/golden-records/{uuid}/external-id

Привязка внешнего идентификатора к записи `{uuid}` (`GR_…`).

```json
{"source": "bank-1", "externalId": "CL-778812"}
```

| Код | Значение |
|---|---|
| `201` | Связка создана; в теле — связка с вложенными записью и источником |
| `400` | `External ID for source <код> and client <uuid> already exists` |
| `404` | `Source Not Found`; `Golden Record <uuid> not found` |
| `422` | Не заполнено `source` или `externalId` |

### PUT /api/v1/external/golden-records/{uuid}/external-id

Изменение внешнего идентификатора в связке записи `{uuid}` с источником.
Тело — как при привязке. `404`, если связки нет.

### PUT /api/v1/external/golden-records/external-id

Изменение внешнего идентификатора по прежнему значению.

```json
{"source": "bank-1", "oldExternalId": "CL-778812", "newExternalId": "CL-990001"}
```

`404`, если связки с прежним значением нет.

!!! warning
    Запросы изменения в текущей версии могут вернуть `500` после того, как
    изменение сохранено. Проверяйте результат поиском по новому значению. См.
    [Внешние идентификаторы](../../consumers/external-ids.md).

## Согласия

Пути `/api/v1/consents/**` передаются в `eidos-consents` с заменой префикса на
`/internal/api/v1/consents`. Регистрация и чтение проходят через шлюз с двумя
правками запроса:

- `source_name` заменяется кодом источника из токена;
- идентификатор клиента принимается как `GR_<uuid>` и как `<uuid>`; шлюз
  снимает префикс.

### POST /api/v1/consents

```json
{"type": "pd", "client_uuid": "GR_<uuid>", "initial_date": "2026-10-01", "end_date": "2027-03-30"}
```

| Поле | Обязательно | Описание |
|---|---|---|
| `type` | да | Код типа — [Перечисления](../enums.md#типы-согласий) |
| `client_uuid` | да | Идентификатор Золотой записи физлица |
| `initial_date` | да | Начало действия, `yyyy-MM-dd` |
| `end_date` | нет | Окончание включительно; по умолчанию — по сроку типа |

| Код | Значение |
|---|---|
| `201` | `{"data": {согласие}, "detail": "Consent created"}` |
| `422` | `Consent has no client_uuid`; `client_uuid is not a golden record identifier: …`; `Malformed consent body: …`; `<поле>: must not be null` |

Формат согласия — [API eidos-consents](consents.md#формат-согласия).

### GET /api/v1/consents/client/{clientId}

Сводка по всем типам согласий клиента: по каждому типу — последнее согласие и
признак действия на сегодня.

### GET /api/v1/consents/active/{clientId}?type={TYPE}

Действующее согласие одного типа. `type` — **имя** типа (`PERSONAL_DATA`, …).
`404`, если действующего нет.

### POST /api/v1/consents/{id}/revoke

Отзыв согласия: дата окончания переносится на вчерашний день. `404`, если
согласия нет.

## Служебные пути

### GET /health

Без аутентификации. Ответ `{"status":"ok"}`.
