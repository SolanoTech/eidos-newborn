# API eidos-core

| | |
|---|---|
| Базовый адрес | `http://eidos-core:8080` внутри сети; на стенде `http://localhost:8080` |
| Аутентификация | **Нет.** API доступен любому, кто может обратиться к сервису, — держите его во внутренней сети |
| Ошибки | `{"detail": "…"}`; `404` — не найдено, `400` — неверный запрос, `422` — нарушение правил, `500` — прочее, включая отсутствующие параметры |

Пути `/api/v1/external/**` — те же, что во [внешнем API](external.md): шлюз
передаёт их сюда без изменений. Ниже — внутренние пути.

## Постраничный ответ

Списки возвращаются страницами по 20 элементов. Параметр `page` нумеруется **с
единицы**, а поле `number` в ответе — с нуля:

```json
{
  "content": [ … ],
  "number": 0,
  "size": 20,
  "totalElements": 57,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

## Золотые записи физлиц

### POST /api/v1/internal/golden-records

Приём записи от `eidos-stage`: сопоставление, создание или слияние.

```json
{
  "record": { "GR_FirstName": "Алишер", "GR_LastName": "Каримов", "…": "…" },
  "source": "bank-1",
  "client_source_identificator": "CL-778812"
}
```

`record` — запись в формате контракта (`GR_…`), см.
[Поля физлица](../fields-person.md).

| Код | Значение |
|---|---|
| `200` | `{"status":"SUCCESS"}` — запись создана, слита или конфликт отправлен на разбор |
| `404` | `{"detail":"Source <код> not found"}` — источника нет в таблице ядра; данные записаны в очередь как `UNKNOWN_SOURCE` |
| `422` | Нарушены обязательность или форматы записи: `{"detail":"<свойство>: <сообщение>; …"}` |
| `500` | Например, нарушение уникальности ПИНФЛ |

### GET /api/v1/internal/golden-records/{clientId}

Запись по идентификатору `GR_…`. `404` — `Golden Record Not Found`.

### GET /api/v1/internal/golden-records/{clientId}/field-meta

Происхождение полей, по алфавиту имён:

```json
[{"fieldName": "grFirstName", "sourceName": "crm", "trustLevel": 5, "updatedAt": "2026-10-04T10:15:30.481"}]
```

### GET /api/v1/internal/golden-records/{clientId}/external-ids

Связки с системами-источниками, по алфавиту кодов:

```json
[{"sourceName": "crm", "externalId": "CRM-000123", "active": true, "createdAt": "2026-10-04T10:15:30.481"}]
```

### GET /api/v1/internal/golden-records/search/structured

| Параметр | Сравнение |
|---|---|
| `pinfl` | Целиком, по слепому индексу; нецифровые символы отбрасываются |
| `passport` | Подстрока; без пробелов, без учёта регистра |
| `lastName`, `firstName`, `middleName` | Подстрока без учёта регистра |
| `phone` | Подстрока; только цифры |
| `page` | Номер страницы с 1, по умолчанию 1 |

Пустые параметры не участвуют; без условий — первая страница всех записей.
Сортировка — по времени создания. Ответ — страница записей.

### GET /api/v1/internal/golden-records/search?query={строка}&page={n}

Подстрока, с учётом регистра, в фамилии, имени, паспорте или ПИНФЛ. Ответ —
страница записей.

### POST /api/v1/internal/golden-records/{clientId}/reveal

Раскрывает значения обезличенной записи. Разовый просмотр: форма хранения не
меняется, запись остаётся с токенами.

Тело:

```json
{
  "fields": ["grPinfl", "grMobilePhoneMain"],
  "actor": "operator@company",
  "reason": "обращение клиента по горячей линии, тикет 4471"
}
```

| Поле | Обязательно | Назначение |
|---|---|---|
| `fields` | нет | Какие поля раскрыть. Пусто — все персональные |
| `actor` | да | Кто запрашивает; попадает в журнал |
| `reason` | да | Основание, не короче 10 символов; попадает в журнал |

Ответ — поле и его открытое значение:

```json
{"grPinfl": "61157669271732", "grMobilePhoneMain": "998901234567"}
```

Ошибки:

| Код | Когда |
|---|---|
| `404` | Записи с таким идентификатором нет |
| `422` | Не указан запрашивающий, основание короче 10 символов или запись не обезличена |

Каждое раскрытое поле пишется отдельной строкой в `vault.pd_disclosure`.

## Золотые записи юрлиц

### POST /api/v1/internal/legal-records

Приём записи юрлица от `eidos-stage`. Тело — как у физлиц, `record` — в формате
[полей юрлица](../fields-legal.md). Сопоставление — по внешнему
идентификатору, затем по ИНН.

### GET /api/v1/internal/legal-records/search/inn?inn={ИНН}

| Код | Значение |
|---|---|
| `200` | Запись юрлица |
| `400` | `{"detail":"INN is required"}` |
| `404` | `{"detail":"Legal entity with INN <ИНН> not found"}` |

### GET /api/v1/internal/legal-records/search/name

| Параметр | Описание |
|---|---|
| `query` | Название или его часть |
| `page` | Номер страницы с 1 |
| `threshold` | Порог сходства от 0 до 1, по умолчанию 0,3 |

Ответ — страница записей, отсортированных по убыванию сходства. Если после
нормализации от запроса ничего не осталось, страница пуста.

### GET /api/v1/internal/legal-records/{grLegalEntityId}

Запись юрлица по идентификатору `LE_…`.

### GET /api/v1/internal/legal-records/{grLegalEntityId}/field-meta

### GET /api/v1/internal/legal-records/{grLegalEntityId}/external-ids

Как у физлиц.

## Очередь разбора

Пути для физлиц — `/api/v1/internal/tentative`, для юрлиц —
`/api/v1/internal/tentative-legal`. Методы одинаковые.

### GET /api/v1/internal/tentative?reason={причина}&page={n}

`reason` — `UNKNOWN_SOURCE` или `GREY_ZONE_CONFLICT`, необязателен. Новые
записи первыми. Элемент страницы:

```json
{
  "id": 17,
  "reason": "GREY_ZONE_CONFLICT",
  "grClientId": "GR_5f0c2b1e-8a4d-4c55-9a8e-2b7d4f1c9e10",
  "sourceName": "demo-shop",
  "sourceTrust": 5,
  "personName": "Каримов Алишер Бахтиёрович",
  "createdAt": "2026-10-04T11:40:02.117",
  "conflicts": [
    {"fieldName": "grMiddleName", "currentValue": "Бахтиёрович", "incomingValue": "Бахтиярович", "currentSource": "demo-crm", "currentTrust": 5}
  ],
  "snapshot": {"grFirstName": "Алишер", "grMiddleName": "Бахтиярович", "…": "…"}
}
```

- `conflicts` — поля, где входящее значение непустое и отличается от
  текущего; для `UNKNOWN_SOURCE` пусто.
- `snapshot` — все непустые поля входящей карточки.
- `sourceTrust` — уровень доверия источника в ядре; `null`, если источника нет.

У юрлиц вместо `grClientId` и `personName` — `grLegalEntityId`,
`merchantName` и `inn`.

### GET /api/v1/internal/tentative/count

```json
{"total": 41, "greyZone": 37, "unknownSource": 4}
```

### POST /api/v1/internal/tentative/{id}/resolve

```json
{"action": "ACCEPT_INCOMING"}
```

| `action` | Действие |
|---|---|
| `KEEP_CURRENT` | Закрыть, запись не меняется |
| `ACCEPT_INCOMING` | Перенести отличающиеся непустые значения снимка в запись |
| `REJECT` | Закрыть без изменений |

| Код | Значение |
|---|---|
| `200` | `{"status":"RESOLVED"}` |
| `404` | `Tentative record <id> not found` |
| `422` | `Unknown source records cannot be merged: register the source and resend the data`; `Source '<код>' is not registered in core` |

## Метрики

### GET /api/v1/internal/metrics

Показатели по записям физлиц:

| Поле | Значение |
|---|---|
| `grTotal` | Число Золотых записей |
| `tentativeTotal`, `tentativeGreyZone`, `tentativeUnknownSource` | Размер очереди разбора физлиц по причинам |
| `phoneValidPct` | Доля записей с телефоном вида `^998\d{9}$`, % |
| `pinflValidPct` | Доля записей с ПИНФЛ из 14 цифр, % |
| `middleNamePct` | Доля записей с отчеством, % |
| `fresh30dPct` | Доля записей, изменённых за 30 дней, % |

## Служебные пути

### GET /health

`{"status":"ok"}`.
