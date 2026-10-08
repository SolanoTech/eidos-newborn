# Keycloak + Gravitee: фазы 1–2 перехода с ui-backend

## Что где

| Компонент | URL | Доступ |
|---|---|---|
| Keycloak | http://localhost:8180 (админка `/admin`) | admin / admin |
| Realm | `eidos` (импорт из [realm-eidos.json](realm-eidos.json) при старте) | пользователь консолей: admin / admin, роль ADMIN |
| Gravitee Console | http://localhost:8084 | admin / admin |
| Gravitee Gateway | http://localhost:8082 (пути `/eidos/*`) | JWT Keycloak |

## Фаза 1 (auth)

- Консоли (8088/8089) входят через **OIDC Authorization Code + PKCE** (keycloak-js, клиент `eidos-console`). Своей формы логина больше нет.
- **ui-backend** — OAuth2 resource server: проверяет подпись по JWKS realm'а, роль из `realm_access.roles`, актор аудита — `preferred_username`. Собственный выпуск JWT, таблицы пользователей/refresh-токенов/API-ключей удалены.
- Пользователи → Keycloak Users; API-ключи → клиенты с `client_credentials` (пример: `eidos-m2m`).

## Фаза 2 (routing)

nginx консолей роутит admin-пути через Gravitee (JWT валидируется на краю, для stage/gateway инъектируется `X-Admin-Token`):

| Путь консоли | Gravitee | Upstream |
|---|---|---|
| `/api/v1/admin/sources/**` | `/eidos/sources` | stage `/internal/api/v1/sources` (+конструктор) |
| `/api/v1/admin/golden-record-fields` | `/eidos/golden-record-fields` | stage |
| `/api/v1/admin/kafka-config` | `/eidos/kafka-config` | gateway |
| `/api/v1/admin/golden-records/**` | `/eidos/golden-records` | core `/api/v1/internal/golden-records` |
| `/api/v1/admin/conflicts/**` | `/eidos/conflicts` | core `/api/v1/internal/tentative` |

Остальное (`/admin/audit`, `/admin/dashboard`, `/admin/consents`) — по-прежнему ui-backend.

## Настройка Gravitee

[setup_gravitee.py](setup_gravitee.py) — идемпотентно создаёт 5 v4 PROXY API с JWT-планами, приложение `eidos-console` и подписки:

```bash
python3 keycloak/setup_gravitee.py
```

**Важно:** JWT-план Gravitee пускает только токены, чей claim `azp` совпадает
с clientId **приложения, подписанного на план** — без подписки будет 401 даже
с валидной подписью. Скрипт оформляет подписку сам.

## Сети

Стек APIM (`gravitee/docker-compose-apim.yml`) и наш compose связаны внешней
сетью `frontend`: в ней gio_apim_gateway видит `keycloak` (JWKS),
`eidos-stage`, `eidos-core`, `eidos-gateway`, а nginx консолей — `gateway:8082`.
**Поднимать стек APIM нужно до нашего** (иначе сети frontend не будет).

## Фаза 3 (не сделана, осознанно)

- Аудит-семантика перенесённых операций (создание источника, разрешение конфликта) больше не пишется — уехали мимо ui-backend. План: доменные события → audit-service.
- Дашборд-агрегация и согласия остаются в ui-backend.
- Замена `X-Admin-Token` на token relay + scope-проверки в самих сервисах.
