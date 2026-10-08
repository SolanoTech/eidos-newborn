# Copyright 2026 LLC SOLANOTECH
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Настройка Gravitee APIM под фазу 2: admin-маршруты консолей идут через
gio_apim_gateway (8082) напрямую в stage/gateway/core, минуя ui-backend.

Для каждого маршрута создаётся v4 PROXY API:
  * контекст-путь /eidos/<name> на гейтвее;
  * JWT-план: подпись проверяется по JWKS Keycloak (внутренний URL
    http://keycloak:8180 — гейтвей и Keycloak в одной docker-сети frontend);
  * для stage/gateway — flow c transform-headers: инъекция X-Admin-Token
    (внутренняя модель доверия пока прежняя, trust-the-edge);
  * план публикуется, API стартует и деплоится.

Идемпотентно: существующие API (по имени) обновляются только недостающими
частями (план/старт/деплой), дубликаты не создаются.

Запуск:  python3 setup_gravitee.py
Окружение: MGMT_URL (default http://localhost:8083), MGMT_USER/MGMT_PASS
(admin/admin), ADMIN_TOKEN (default change-me-admin-token).
"""
from __future__ import annotations

import base64
import json
import os
import sys
import urllib.error
import urllib.request

MGMT = os.environ.get("MGMT_URL", "http://localhost:8083")
ENV_BASE = f"{MGMT}/management/v2/environments/DEFAULT"
AUTH = base64.b64encode(
    f"{os.environ.get('MGMT_USER', 'admin')}:{os.environ.get('MGMT_PASS', 'admin')}".encode()
).decode()
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "change-me-admin-token")

JWKS_INTERNAL = "http://keycloak:8180/realms/eidos/protocol/openid-connect/certs"

# name, context-path, upstream target, нужен ли X-Admin-Token
#
# Юрлица ходят своими маршрутами, а не параметром типа: у них другая Золотая
# запись, другой поиск и своя очередь конфликтов — разделение на уровне API
# позволяет независимо давать права и считать статистику по вертикалям.
ROUTES = [
    ("eidos-sources",             "/eidos/sources",             "http://eidos-stage:8081/internal/api/v1/sources",             True),
    ("eidos-golden-record-fields","/eidos/golden-record-fields","http://eidos-stage:8081/internal/api/v1/golden-record-fields", True),
    ("eidos-kafka-config",        "/eidos/kafka-config",        "http://eidos-gateway:8090/internal/api/v1/kafka-config",       True),
    ("eidos-golden-records",      "/eidos/golden-records",      "http://eidos-core:8080/api/v1/internal/golden-records",        False),
    ("eidos-conflicts",           "/eidos/conflicts",           "http://eidos-core:8080/api/v1/internal/tentative",             False),
    # --- вертикаль юридических лиц ---
    ("eidos-legal-records",       "/eidos/legal-records",       "http://eidos-core:8080/api/v1/internal/legal-records",         False),
    ("eidos-conflicts-legal",     "/eidos/conflicts-legal",     "http://eidos-core:8080/api/v1/internal/tentative-legal",       False),
]


def req(method: str, path: str, body: dict | None = None) -> tuple[int, dict | list | None]:
    url = path if path.startswith("http") else ENV_BASE + path
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header("Authorization", f"Basic {AUTH}")
    r.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            raw = resp.read()
            return resp.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw.decode(errors="replace")[:500]}


def list_apis() -> list[dict]:
    apis, page = [], 1
    while True:
        status, d = req("GET", f"/apis?page={page}&perPage=50")
        if status != 200:
            sys.exit(f"✗ список API: HTTP {status} {d}")
        apis += d.get("data", [])
        if page >= d.get("pagination", {}).get("pageCount", 1):
            return apis
        page += 1


def api_definition(name: str, path: str, target: str, inject_token: bool) -> dict:
    flows = []
    if inject_token:
        flows.append({
            "name": "inject-admin-token",
            "enabled": True,
            "selectors": [{"type": "HTTP", "path": "/", "pathOperator": "STARTS_WITH"}],
            "request": [{
                "name": "Inject X-Admin-Token",
                "enabled": True,
                "policy": "transform-headers",
                "configuration": {
                    "addHeaders": [{"name": "X-Admin-Token", "value": ADMIN_TOKEN}],
                },
            }],
            "response": [],
        })
    return {
        "name": name,
        "apiVersion": "1.0",
        "definitionVersion": "V4",
        "type": "PROXY",
        "description": f"EIDOS фаза 2: {path} → {target}",
        "listeners": [{
            "type": "HTTP",
            "paths": [{"path": path}],
            "entrypoints": [{"type": "http-proxy"}],
        }],
        "endpointGroups": [{
            "name": "default-group",
            "type": "http-proxy",
            "endpoints": [{
                "name": "default",
                "type": "http-proxy",
                "inheritConfiguration": False,
                "configuration": {"target": target},
            }],
        }],
        "flowExecution": {"mode": "DEFAULT", "matchRequired": False},
        "flows": flows,
    }


def jwt_plan() -> dict:
    return {
        "definitionVersion": "V4",
        "name": "JWT Keycloak (realm eidos)",
        "description": "Подпись проверяется по JWKS Keycloak",
        "mode": "STANDARD",
        "validation": "AUTO",
        "security": {
            "type": "JWT",
            "configuration": {
                "signature": "RSA_RS256",
                "publicKeyResolver": "JWKS_URL",
                "resolverParameter": JWKS_INTERNAL,
                "extractClaims": False,
                "propagateAuthHeader": True,
                "userClaim": "sub",
                "clientIdClaim": "azp",
            },
        },
    }


def ensure_api(existing: dict[str, dict], name: str, path: str, target: str, inject: bool) -> None:
    api = existing.get(name)
    if api is None:
        status, api = req("POST", "/apis", api_definition(name, path, target, inject))
        if status not in (200, 201):
            sys.exit(f"✗ создание {name}: HTTP {status} {json.dumps(api, ensure_ascii=False)[:400]}")
        print(f"✓ API {name} создан ({path} → {target})")
    else:
        print(f"✓ API {name} уже есть")
    api_id = api["id"]

    # --- план JWT ---
    status, plans = req("GET", f"/apis/{api_id}/plans?statuses=PUBLISHED,STAGING,DEPRECATED")
    plan_list = plans.get("data", []) if status == 200 else []
    plan = next((p for p in plan_list if p.get("security", {}).get("type") == "JWT"), None)
    if plan is None:
        status, plan = req("POST", f"/apis/{api_id}/plans", jwt_plan())
        if status not in (200, 201):
            sys.exit(f"✗ план {name}: HTTP {status} {json.dumps(plan, ensure_ascii=False)[:400]}")
        print("  ✓ JWT-план создан")
    if plan.get("status") == "STAGING":
        status, _ = req("POST", f"/apis/{api_id}/plans/{plan['id']}/_publish")
        if status not in (200, 201, 204):
            sys.exit(f"✗ публикация плана {name}: HTTP {status}")
        print("  ✓ план опубликован")

    # --- старт и деплой ---
    if api.get("state") != "STARTED":
        status, res = req("POST", f"/apis/{api_id}/_start")
        if status not in (200, 201, 204):
            sys.exit(f"✗ старт {name}: HTTP {status} {res}")
        print("  ✓ API запущен")
    status, res = req("POST", f"/apis/{api_id}/deployments",
                      {"deploymentLabel": "eidos-phase2"})
    if status in (200, 201, 202, 204):
        print("  ✓ задеплоен на gateway")
    else:
        print(f"  ! деплой: HTTP {status} {json.dumps(res, ensure_ascii=False)[:300]}")


def ensure_application() -> str:
    """
    Приложение APIM с clientId=eidos-console: JWT-план пускает только токены,
    чей claim azp совпадает с clientId приложения, подписанного на план.
    """
    status, apps = req("GET", f"{MGMT}/management/organizations/DEFAULT/environments/DEFAULT/applications?size=100")
    if status == 200:
        data = apps if isinstance(apps, list) else apps.get("data", [])
        for a in data:
            if a.get("name") == "eidos-console":
                print(f"✓ приложение eidos-console уже есть (id={a['id']})")
                return a["id"]
    status, app = req("POST", f"{MGMT}/management/organizations/DEFAULT/environments/DEFAULT/applications", {
        "name": "eidos-console",
        "description": "Консоли EIDOS (Keycloak public client)",
        "settings": {"app": {"client_id": "eidos-console"}},
    })
    if status not in (200, 201):
        sys.exit(f"✗ приложение: HTTP {status} {json.dumps(app, ensure_ascii=False)[:400]}")
    print(f"✓ приложение eidos-console создано (id={app['id']})")
    return app["id"]


def ensure_subscription(api_id: str, app_id: str) -> None:
    status, subs = req("GET", f"/apis/{api_id}/subscriptions?statuses=ACCEPTED,PENDING&perPage=50")
    if status == 200:
        for sub in subs.get("data", []):
            # Подписка отдаётся вложенным объектом application: {id: ...},
            # плоского applicationId в ответе нет.
            if (sub.get("application") or {}).get("id") == app_id:
                return
    status, plans = req("GET", f"/apis/{api_id}/plans?statuses=PUBLISHED")
    plan = next((p for p in plans.get("data", []) if p.get("security", {}).get("type") == "JWT"), None)
    if plan is None:
        print("  ! нет опубликованного JWT-плана — подписка пропущена")
        return
    status, res = req("POST", f"/apis/{api_id}/subscriptions",
                      {"applicationId": app_id, "planId": plan["id"]})
    if status in (200, 201):
        print("  ✓ подписка eidos-console оформлена")
    else:
        sys.exit(f"✗ подписка: HTTP {status} {json.dumps(res, ensure_ascii=False)[:400]}")


def main() -> None:
    app_id = ensure_application()
    existing = {a["name"]: a for a in list_apis()}
    for name, path, target, inject in ROUTES:
        ensure_api(existing, name, path, target, inject)
        api_id = existing.get(name, {}).get("id") or next(
            a["id"] for a in list_apis() if a["name"] == name)
        ensure_subscription(api_id, app_id)
    print("\nГотово. Проверка (нужен Bearer-токен Keycloak):")
    print("  curl -H \"Authorization: Bearer $T\" http://localhost:8082/eidos/sources")


if __name__ == "__main__":
    main()
