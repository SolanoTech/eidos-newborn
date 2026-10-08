# Мониторинг и диагностика

## Проверки живости

Каждый сервис отвечает на `GET /health` ответом `{"status":"ok"}`:

| Сервис | Адрес внутри сети compose |
|---|---|
| `eidos-core` | `http://eidos-core:8080/health` |
| `eidos-stage` | `http://eidos-stage:8081/health` |
| `eidos-gateway` | `http://eidos-gateway:8090/health` |
| `eidos-consents` | `http://eidos-consents:8082/health` |
| `eidos-cdi-ui-backend` | `http://eidos-cdi-ui-backend:8083/health` |

Проверка подтверждает, что процесс отвечает, но не проверяет базы, брокер и
OpenBao. В `docker-compose.yml` проверки здоровья настроены только для
инфраструктуры; для сервисов модуля их можно добавить в файле
переопределений.

Метрик в формате Prometheus сервисы не публикуют.

## Показатели платформы

| Что | Где | На что смотреть |
|---|---|---|
| Приём по источникам за сутки | `GET /internal/api/v1/ingest-stats?days=1` в `eidos-stage`, дашборд консоли | Доля отклонённых; источник, который перестал присылать данные |
| Размер очереди разбора | `GET /api/v1/internal/tentative/count` и `/tentative-legal/count` в `eidos-core` | Рост очереди, особенно `unknownSource` |
| Качество Золотых записей | `GET /api/v1/internal/metrics` в `eidos-core` | Число записей, свежесть |
| Отставание чтения Kafka | `rpk group describe eidos-gateway` (на стенде — `docker compose exec kafka rpk group describe eidos-gateway`) | Растущее отставание: шлюз не успевает или остановлен |

Пример ответа метрик ядра:

```json
{
  "grTotal": 15230,
  "tentativeTotal": 41,
  "tentativeGreyZone": 37,
  "tentativeUnknownSource": 4,
  "phoneValidPct": 100.0,
  "pinflValidPct": 100.0,
  "middleNamePct": 93.4,
  "fresh30dPct": 18.2
}
```

## Что стоит отслеживать

| Сигнал | Возможная причина |
|---|---|
| `/health` не отвечает | Сервис упал или перегружен |
| Рост «Отклонено» у источника | Источник сменил формат; изменили контракт; истёк или сменился токен |
| Появились записи `unknownSource` | Источник не зарегистрирован в ядре |
| Рост отставания Kafka | Шлюз выключен, ошибка подключения к брокеру, медленная обработка |
| Ошибки `Key store operation failed` в журнале ядра | OpenBao недоступен или запечатан |
| Ошибки `Consents service unreachable` в журнале ядра | Сервис согласий недоступен; события будут обработаны повторно |
| Место на диске | Рост сырья в MongoDB и архива в PostgreSQL |

## Журналы

Сервисы пишут журналы в стандартный вывод контейнера:

```bash
docker compose logs -f --since 15m eidos-gateway eidos-stage eidos-core
```

Полезные строки:

| Сервис | Строка | Что означает |
|---|---|---|
| `eidos-gateway` | `Gateway Kafka consumer started: entityType=…` | Потребитель запущен |
| `eidos-gateway` | `No active Kafka config in DB` | Ни один потребитель не включён |
| `eidos-gateway` | `Skipping Kafka message <топик>-<партиция>@<смещение>: …` | Сообщение не обработано, причина в конце строки |
| `eidos-stage` | `Processed PERSON message source=… clientSourceIdentificator=…` | Карточка принята |
| `eidos-stage` | `Processing failed: …` | Карточка отклонена, причина — в сообщении и трассировке |
| `eidos-core` | `Unknown source '…' — writing tentative and rejecting request` | Источник не зарегистрирован в ядре |
| `eidos-core` | `Grey-zone conflict on field …` | Конфликт при равном доверии |
| `eidos-core` | `Blind index backfill finished` / `failed` | Итог досчёта слепого индекса при старте |
| `eidos-core` | `Key store ready` / `Key store is not ready` | Состояние OpenBao при старте |
| `eidos-consents` | `Published N expired consent events for <дата>` | Ежедневный проход нашёл истёкшие согласия и опубликовал события |

Журналы могут содержать идентификаторы клиентов и значения, нарушившие
контракт. Храните их как конфиденциальные данные.
