# Передача по REST

REST — основной способ передачи: каждая карточка обрабатывается синхронно, и
источник сразу узнаёт результат.

## Запрос

```http
POST /api/v1/client-data HTTP/1.1
Host: <адрес eidos-gateway>
X-Access-Token: <токен источника>
Content-Type: application/json

{"data": { … карточка в формате источника … }}
```

| Тип сущности | Метод и путь |
|---|---|
| Физическое лицо | `POST /api/v1/client-data` |
| Юридическое лицо | `POST /api/v1/legal-data` |

- Одна карточка — один запрос. Пакетной передачи нет.
- Тело — JSON-объект с полем `data`; внутри `data` — данные в формате
  источника, как они описаны в дата-контракте.
- Поле `source` в теле передавать не нужно: шлюз всё равно подставит код
  источника из токена.
- Кодировка — UTF-8.

## Ответы

| Код | Тело | Что означает | Что делать |
|---|---|---|---|
| `200` | `{"status":"SUCCESS"}` | Карточка сохранена: создана новая Золотая запись, данные слиты с существующей или отправлены на ручной разбор | Ничего |
| `401` | `{"detail":"Missing X-Access-Token"}` или `{"detail":"Invalid access token"}` | Нет токена или он неизвестен | Проверьте токен; не повторяйте без исправления |
| `422` | `{"detail":"Malformed client-data body: …"}` | Тело не является JSON-объектом | Исправьте формирование запроса |
| `422` | `{"detail":"…"}` | Карточка отклонена: нет контракта, нарушение контракта, ошибка преобразования, отказ ядра | Исправьте данные или настройки; см. ниже |
| `5xx` | — | Сбой платформы или сети | Повторите позже |

Ответ `200` приходит и тогда, когда данные ушли на ручной разбор как конфликт
при равном доверии: для источника карточка принята. Подробный разбор сообщений
`422` и стратегия повторов — [Ошибки, повторы, идемпотентность](errors.md).

## Примеры

=== "curl"

    ```bash
    curl -s -X POST "https://eidos.example.uz/api/v1/client-data" \
      -H "X-Access-Token: $EIDOS_TOKEN" \
      -H "Content-Type: application/json" \
      --data-binary @card.json
    ```

=== "Python"

    ```python
    import requests

    def send_card(card: dict) -> None:
        response = requests.post(
            "https://eidos.example.uz/api/v1/client-data",
            json={"data": card},
            headers={"X-Access-Token": EIDOS_TOKEN},
            timeout=30,
        )
        if response.status_code == 200:
            return
        if response.status_code in (401, 422):
            # Повтор без исправления ничего не даст — фиксируем причину.
            raise ValueError(response.json().get("detail"))
        response.raise_for_status()
    ```

=== "Java"

    ```java
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://eidos.example.uz/api/v1/client-data"))
            .header("X-Access-Token", eidosToken)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"data\":" + cardJson + "}", StandardCharsets.UTF_8))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    ```

## Производительность

- Запрос завершается после записи в ядро. Время ответа складывается из
  проверки на шлюзе, записи сырья в MongoDB, преобразования и транзакции ядра.
- Для больших объёмов отправляйте карточки параллельно, в несколько потоков,
  но изменения **одного клиента** — последовательно: применяется последняя
  пришедшая карточка, см. [Поток изменений](initial-load.md#поток-изменений).
- Таймауты на стороне шлюза не настроены. Задавайте таймаут на стороне клиента
  (30 секунд с запасом) и повторяйте запрос при его срабатывании — повтор
  безопасен.
- Оценить пропускную способность стенда можно скриптом из каталога `loadtest`
  — см. [Производительность](../operations/performance.md).

## Чтение и привязка через тот же шлюз

Тем же токеном источник может искать Золотые записи и работать с согласиями:

- [Поиск Золотой записи](../consumers/search.md);
- [Внешние идентификаторы](../consumers/external-ids.md);
- [Передача согласий](consents.md).
