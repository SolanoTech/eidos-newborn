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
Подготовка окружения для сквозного/нагрузочного теста.

Делает три вещи (идемпотентно):
  1. Заводит источник `loadtest` в реестре (через admin API stage).
  2. Наполняет его дата-контракт: client_identifier_field = client_id и полный
     набор полей, покрывающий все обязательные поля Золотой записи.
  3. Включает Kafka-консьюмер gateway на брокер `kafka:9092`, топик
     `client-data` (через admin API gateway — консьюмер перезапускается на лету).

По завершении пишет loadtest/config.json (source, token, топик и т.п.),
который читают loadtest.py и e2e-проверки.

Только стандартная библиотека. Требует поднятых stage (8081) и gateway (8090).

    python3 setup.py               # физлица: контракт + топик client-data
    python3 setup.py --legal       # юрлица: контракт LEGAL_ENTITY + топик legal-data
    python3 setup.py --skip-kafka  # без настройки Kafka (только REST-путь)
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import urllib.error
import urllib.request

ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "change-me-admin-token")
STAGE = os.environ.get("STAGE_URL", "http://localhost:8081")
GATEWAY = os.environ.get("GATEWAY_URL", "http://localhost:8090")

SOURCE_CODE = "loadtest"
SOURCE_TOKEN = os.environ.get("LOADTEST_TOKEN", "tk_loadtest_secret_token_0001")
TOPIC = "client-data"
LEGAL_TOPIC = "legal-data"   # у юрлиц свой топик: тип задаёт канал, не payload
GROUP = "eidos-gateway"
KAFKA_INTERNAL = "kafka:9092"   # адрес брокера ВНУТРИ сети compose (для gateway)

HERE = os.path.dirname(os.path.abspath(__file__))
CONFIG_PATH = os.path.join(HERE, "config.json")

# Поля контракта (client_id — это client_identifier_field, отдельно, не поле GR):
# source_field, target_gr_field, type, required, date_format, regex, value_map
CONTRACT_FIELDS = [
    # source_field,           target_gr_field,          type,      required, date_format,   regex,                 value_map
    ("phone",                 "GR_mobilePhoneMain",     "STRING",  True,  None,           r"^998\d{9}$",           None),
    ("first_name",            "GR_FirstName",           "STRING",  True,  None,           None,                    None),
    ("last_name",             "GR_LastName",            "STRING",  True,  None,           None,                    None),
    ("middle_name",           "GR_MiddleName",          "STRING",  False, None,           None,                    None),
    ("pinfl",                 "GR_Pinfl",               "STRING",  True,  None,           r"^\d{14}$",             None),
    ("gender",                "GR_Gender",              "GENDER",  True,  None,           None,                    {"M": "M", "F": "F", "1": "M", "2": "F"}),
    ("birth_date",            "GR_BirthDate",           "DATE",    True,  "dd.MM.yyyy",   None,                    None),
    ("citizenship",           "GR_Citizenship",         "STRING",  True,  None,           None,                    None),
    ("citizenship_id",        "GR_CitizenshipId",       "STRING",  True,  None,           None,                    None),
    ("passport",              "GR_docPassData",         "STRING",  True,  None,           r"^[A-Z]{2}\d{7}$",      None),
    ("passport_issued_by",    "GR_docIssuedBy",         "STRING",  True,  None,           None,                    None),
    ("passport_issued_by_id", "GR_docIssuedById",       "STRING",  True,  None,           None,                    None),
    ("passport_issued_date",  "GR_docIssuedDate",       "DATE",    True,  "dd.MM.yyyy",   None,                    None),
    ("email",                 "GR_contactsEmail",       "STRING",  False, None,           None,                    None),
    ("address",               "GR_addrPermanentAddress","STRING",  False, None,           None,                    None),
]


# Поля контракта юрлиц: source_field, target, type, required, date_format, regex
LEGAL_CONTRACT_FIELDS = [
    ("tin",             "GR_Inn",              "STRING",  True,  None,         r"^\d{9}$"),
    ("name",            "GR_FullName",         "STRING",  True,  None,         None),
    ("brand",           "GR_ShortName",        "STRING",  False, None,         None),
    ("opf_code",        "GR_OpfCode",          "STRING",  False, None,         None),
    ("opf_name",        "GR_OpfName",          "STRING",  False, None,         None),
    ("active",          "GR_IsActive",         "BOOLEAN", True,  None,         None),
    ("bankrupt",        "GR_IsBankrupt",       "BOOLEAN", True,  None,         None),
    ("oked",            "GR_OkedCode",         "STRING",  True,  None,         None),
    ("oked_name",       "GR_OkedName",         "STRING",  True,  None,         None),
    ("reg_date",        "GR_RegistrationDate", "DATE",    False, "dd.MM.yyyy", None),
    ("reg_number",      "GR_RegistrationNumber","STRING", False, None,         None),
    ("capital",         "GR_StatutoryFund",    "STRING",  False, None,         None),
    ("address",         "GR_AddressFull",      "STRING",  False, None,         None),
    ("region",          "GR_RegionName",       "STRING",  False, None,         None),
    ("email",           "GR_Email",            "STRING",  False, None,         None),
    ("director",        "GR_DirectorName",     "STRING",  False, None,         None),
    ("contacts.phones", "GR_Phones",           "ARRAY",   False, None,         None),
    ("owners",          "GR_Founders",         "STRUCT",  False, None,         None),
]


def _req(method: str, url: str, body: dict | None = None, admin: bool = True) -> tuple[int, dict | None]:
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if admin:
        req.add_header("X-Admin-Token", ADMIN_TOKEN)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            raw = resp.read()
            return resp.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw.decode(errors="replace")}


def ensure_source(code: str = SOURCE_CODE, name: str = "Load Test",
                  token: str = SOURCE_TOKEN, trust: int = 8) -> int:
    """Создаёт источник (или возвращает id существующего по коду)."""
    status, existing = _req("GET", f"{STAGE}/internal/api/v1/sources")
    if status == 200 and existing:
        for s in existing:
            if s.get("code") == code:
                print(f"✓ источник '{code}' уже есть (id={s['id']}, trust={s.get('trustLevel')})")
                return s["id"]

    status, created = _req("POST", f"{STAGE}/internal/api/v1/sources", {
        "code": code, "name": name, "token": token,
        "trustLevel": trust, "enabled": True,
    })
    if status not in (200, 201):
        raise SystemExit(f"✗ не удалось создать источник: HTTP {status} {created}")
    print(f"✓ источник '{code}' создан (id={created['id']})")
    return created["id"]


def ensure_contract(source_id: int, reset: bool = False, entity_type: str = "PERSON") -> None:
    """Контракт источника для одного типа сущности; у источника их может быть два."""
    fields = CONTRACT_FIELDS if entity_type == "PERSON" else LEGAL_CONTRACT_FIELDS
    identifier = "client_id" if entity_type == "PERSON" else "merchant_id"
    q = f"?entityType={entity_type}"

    _req("PUT", f"{STAGE}/internal/api/v1/sources/{source_id}/contract{q}",
         {"clientIdentifierField": identifier})
    print(f"✓ контракт {entity_type}: client_identifier_field = {identifier}")

    status, contract = _req("GET", f"{STAGE}/internal/api/v1/sources/{source_id}/contract{q}")
    if reset and status == 200 and contract and contract.get("fields"):
        for f in contract["fields"]:
            _req("DELETE", f"{STAGE}/internal/api/v1/sources/{source_id}/contract/fields/{f['id']}{q}")
        print(f"✓ старые поля контракта удалены ({len(contract['fields'])})")
        contract = {"fields": []}

    existing_names = set()
    if contract and contract.get("fields"):
        existing_names = {f.get("sourceFieldName") for f in contract["fields"]}

    added = skipped = 0
    for i, spec in enumerate(fields):
        src, tgt, typ, req, fmt, rgx = spec[:6]
        vmap = spec[6] if len(spec) > 6 else None
        if src in existing_names:
            skipped += 1
            continue
        payload = {
            "sourceFieldName": src, "targetGrField": tgt, "dataType": typ,
            "required": bool(req), "ordering": i,
        }
        if fmt:  payload["sourceDateFormat"] = fmt
        if rgx:  payload["validationRegex"] = rgx
        if vmap: payload["valueMap"] = vmap
        status, res = _req("POST", f"{STAGE}/internal/api/v1/sources/{source_id}/contract/fields{q}", payload)
        if status not in (200, 201):
            raise SystemExit(f"✗ поле {src}→{tgt}: HTTP {status} {res}")
        added += 1
    print(f"✓ поля контракта: добавлено {added}, уже было {skipped}")


def ensure_core_sources() -> None:
    """
    Регистрирует источники в core.source (trust_level). Core отклоняет неизвестные
    источники при приёме. Таблицу создаёт Hibernate, поэтому сидим через psql уже
    после подъёма сервисов. Best-effort: если docker недоступен — подсказываем SQL.
    """
    sql = os.path.join(HERE, "seed_core_sources.sql")
    cmd = ["docker", "compose", "exec", "-T", "postgres",
           "psql", "-U", "postgres", "-d", "eidos_core", "-f", "-"]
    try:
        with open(sql, "rb") as f:
            r = subprocess.run(cmd, stdin=f, cwd=os.path.dirname(HERE),
                               capture_output=True, timeout=30)
        if r.returncode == 0:
            print("✓ источники core засеяны (loadtest + реестр)")
        else:
            print("! не удалось засеять core автоматически — выполните вручную:")
            print(f"    docker compose exec -T postgres psql -U postgres -d eidos_core -f - < {sql}")
    except Exception:
        print("! docker недоступен из setup.py — засейте core вручную:")
        print(f"    docker compose exec -T postgres psql -U postgres -d eidos_core -f - < {sql}")


def ensure_kafka(entity_type: str = "PERSON") -> None:
    """Канал Kafka на тип сущности: физлица и юрлица идут разными топиками."""
    topic = TOPIC if entity_type == "PERSON" else LEGAL_TOPIC
    group = GROUP if entity_type == "PERSON" else GROUP + "-legal"
    status, res = _req("PUT", f"{GATEWAY}/internal/api/v1/kafka-config", {
        "entityType": entity_type,
        "bootstrapServers": KAFKA_INTERNAL, "topic": topic,
        "groupId": group, "enabled": True,
    })
    if status not in (200, 201):
        raise SystemExit(f"✗ настройка Kafka ({entity_type}): HTTP {status} {res}")
    print(f"✓ Kafka-консьюмер gateway [{entity_type}]: {KAFKA_INTERNAL} topic={topic} (перезапущен)")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--skip-kafka", action="store_true", help="не трогать конфигурацию Kafka")
    ap.add_argument("--source", help="настроить полный контракт для этого источника "
                                     "(напр. payme) вместо основного loadtest")
    ap.add_argument("--reset-fields", action="store_true",
                    help="перед добавлением удалить существующие поля контракта")
    ap.add_argument("--legal", action="store_true",
                    help="настроить вертикаль юрлиц: контракт LEGAL_ENTITY и топик legal-data")
    args = ap.parse_args()

    # Режим настройки произвольного источника (например payme для merge-теста):
    # даём ему тот же полный контракт и те же имена полей, что у loadtest.
    if args.source and args.source != SOURCE_CODE:
        entity_type = "LEGAL_ENTITY" if args.legal else "PERSON"
        sid = ensure_source(code=args.source, name=args.source, token="tk_" + args.source, trust=5)
        ensure_contract(sid, reset=args.reset_fields, entity_type=entity_type)
        print(f"\nГотово: источник '{args.source}' получил контракт {entity_type} "
              f"(поля как у loadtest). Токен и trust берутся из реестра, не меняются.")
        return

    entity_type = "LEGAL_ENTITY" if args.legal else "PERSON"

    source_id = ensure_source()
    ensure_contract(source_id, reset=args.reset_fields, entity_type=entity_type)
    ensure_core_sources()
    if not args.skip_kafka:
        ensure_kafka(entity_type)

    cfg = {
        "source": SOURCE_CODE, "source_id": source_id, "token": SOURCE_TOKEN,
        "topic": TOPIC, "legal_topic": LEGAL_TOPIC, "group": GROUP,
        "bootstrap_host": "localhost:19092", "bootstrap_internal": KAFKA_INTERNAL,
        "gateway": GATEWAY, "stage": STAGE, "core": os.environ.get("CORE_URL", "http://localhost:8080"),
    }
    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(cfg, f, ensure_ascii=False, indent=2)
    print(f"✓ конфигурация записана: {CONFIG_PATH}")
    if args.legal:
        print("\nГотово. Дальше: python3 loadtest.py --entity legal --count 200 --concurrency 8")
    else:
        print("\nГотово. Дальше: python3 loadtest.py --count 200 --concurrency 8")


if __name__ == "__main__":
    main()
