#!/usr/bin/env python3
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
"""Наполняет демонстрационный стенд выдуманными карточками.

Без этого консоли показывают пустые экраны: на стенде нет ни источников, ни
потока данных.

Данные генерируются от фиксированного зерна, поэтому повторный запуск даёт те
же карточки, а не новую порцию. Все значения выдуманы: ПИНФЛ, паспорта,
телефоны и ИНН собраны из случайных цифр и ничьим настоящим реквизитам не
соответствуют. Настоящие персональные данные на этот стенд класть нельзя.

Карточки идут обычным путём приёма — через шлюз, с токеном источника, — чтобы
на стенде работали и происхождение полей, и слияние, и разбор конфликтов.

Запуск из корня репозитория:

    python3 -I deploy/demo/seed.py            # два источника, 40 физлиц, 12 юрлиц
    python3 -I deploy/demo/seed.py --persons 100
    python3 -I deploy/demo/seed.py --dry-run  # показать, что будет сделано
"""

import argparse
import json
import os
import random
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

# Демо-сборка не публикует порты сервисов, поэтому засев работает поверх
# наложения docker-compose.seed.yml: оно открывает их на 127.0.0.1. Адреса
# можно переопределить, если скрипт запускают из сети compose.
STAGE = os.environ.get("EIDOS_STAGE_URL", "http://localhost:8081")
GATEWAY = os.environ.get("EIDOS_GATEWAY_URL", "http://localhost:8090")
COMPOSE = ["docker", "compose", "-f", "docker-compose.demo.yml"]

# Источники стенда: разный уровень доверия, чтобы на экранах были видны и
# слияние по доверию, и серая зона при равных уровнях.
SOURCES = [
    {"code": "demo-bank", "name": "Демобанк — АБС", "trustLevel": 8},
    {"code": "demo-crm", "name": "Демо-CRM отделений", "trustLevel": 5},
]

LAST_NAMES = ["Каримов", "Саидова", "Исмоилов", "Юлдашева", "Рахимов", "Абдуллаева",
              "Тошматов", "Назарова", "Хакимов", "Мирзаева", "Эргашев", "Султонова"]
FIRST_M = ["Алишер", "Бобур", "Тимур", "Жасур", "Рустам", "Азиз", "Шохрух", "Фаррух"]
FIRST_F = ["Гульнора", "Нодира", "Зухра", "Малика", "Дилором", "Севара", "Нигора"]
MIDDLE_M = ["Бахтиёрович", "Рустамович", "Азизович", "Шавкатович"]
MIDDLE_F = ["Бахтиёровна", "Рустамовна", "Азизовна", "Шавкатовна"]
REGIONS = ["ОВД Мирзо-Улугбекского района", "ОВД Чиланзарского района",
           "ОВД Юнусабадского района", "ОВД Самаркандского района"]
MERCHANTS = ["Оазис Маркет", "Барака Строй", "Зафар Групп", "Олтин Трейд",
             "Дилором Фуд", "Тошкент Савдо", "Нурли Йул", "Авесто Текстиль",
             "Силк Роуд Логистик", "Чинор Фарм", "Ипак Йули", "Самарканд Агро"]
OPF = [("ООО", "1210"), ("МЧЖ", "1210"), ("АО", "1150"), ("ЧП", "1130")]
OKED = [("4120", "Строительство зданий"), ("4711", "Розничная торговля"),
        ("5610", "Деятельность ресторанов"), ("4941", "Грузовые перевозки"),
        ("1310", "Подготовка и прядение текстильных волокон")]

# Правила контракта: путь в данных источника → поле Золотой записи.
# Двенадцать полей физлица обязательны на уровне DTO, иначе ядро отклонит
# карточку; отчество и почта добавлены, чтобы экраны не выглядели голыми.
PERSON_FIELDS = [
    ("last_name", "GR_LastName", "STRING", True, None),
    ("first_name", "GR_FirstName", "STRING", True, None),
    ("middle_name", "GR_MiddleName", "STRING", False, None),
    ("pinfl", "GR_Pinfl", "STRING", True, None),
    ("gender", "GR_Gender", "GENDER", True, None),
    ("birth_date", "GR_BirthDate", "DATE", True, "yyyy-MM-dd"),
    ("phone", "GR_mobilePhoneMain", "STRING", True, None),
    ("email", "GR_contactsEmail", "STRING", False, None),
    ("citizenship", "GR_Citizenship", "STRING", True, None),
    ("citizenship_id", "GR_CitizenshipId", "STRING", True, None),
    ("passport", "GR_docPassData", "STRING", True, None),
    ("passport_issued_by", "GR_docIssuedBy", "STRING", True, None),
    ("passport_issued_by_id", "GR_docIssuedById", "STRING", True, None),
    ("passport_issued_date", "GR_docIssuedDate", "DATE", True, "yyyy-MM-dd"),
]

LEGAL_FIELDS = [
    ("inn", "GR_Inn", "STRING", True, None),
    ("full_name", "GR_FullName", "STRING", True, None),
    ("short_name", "GR_ShortName", "STRING", False, None),
    ("opf_name", "GR_OpfName", "STRING", False, None),
    ("opf_code", "GR_OpfCode", "STRING", False, None),
    ("oked_code", "GR_OkedCode", "STRING", True, None),
    ("oked_name", "GR_OkedName", "STRING", True, None),
    ("is_active", "GR_IsActive", "BOOLEAN", True, None),
    ("is_bankrupt", "GR_IsBankrupt", "BOOLEAN", True, None),
]


def request(method, url, body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            raw = response.read().decode()
            return response.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, raw
    except urllib.error.URLError as e:
        sys.exit(f"Не отвечает {url}: {e.reason}. Стенд поднят?")


def admin(token):
    return {"X-Admin-Token": token}


def ensure_source(spec, token, source_token, dry):
    """Создаёт источник в реестре; при повторном запуске находит существующий."""
    if dry:
        print(f"  создать источник {spec['code']} (доверие {spec['trustLevel']})")
        return None
    body = dict(spec, token=source_token, enabled=True)
    status, payload = request("POST", f"{STAGE}/internal/api/v1/sources", body, admin(token))
    if status == 201:
        print(f"  источник {spec['code']} создан, id={payload['id']}")
        return payload["id"]
    # Код или токен уже заняты — источник из прошлого запуска.
    status, payload = request("GET", f"{STAGE}/internal/api/v1/sources", headers=admin(token))
    for src in payload or []:
        if src.get("code") == spec["code"]:
            print(f"  источник {spec['code']} уже есть, id={src['id']}")
            return src["id"]
    sys.exit(f"Не удалось ни создать, ни найти источник {spec['code']}")


def register_in_core(code, trust, dry):
    """Ядро принимает данные только от источников из собственной таблицы.

    API для неё нет — только SQL, см. docs/sources/registration.md.
    """
    sql = (f"INSERT INTO source (source_name, trust_level) VALUES ('{code}', {trust}) "
           f"ON CONFLICT (source_name) DO UPDATE SET trust_level = EXCLUDED.trust_level;")
    if dry:
        print(f"  зарегистрировать {code} в ядре: {sql}")
        return
    done = subprocess.run(COMPOSE + ["exec", "-T", "postgres", "psql", "-U", "postgres",
                                     "-d", "eidos_core", "-c", sql],
                          capture_output=True, text=True)
    if done.returncode != 0:
        sys.exit(f"Не удалось зарегистрировать {code} в ядре: {done.stderr.strip()}")
    print(f"  источник {code} зарегистрирован в ядре, доверие {trust}")


def ensure_contract(source_id, fields, entity_type, token, dry):
    if dry:
        print(f"  контракт {entity_type}: {len(fields)} правил")
        return
    created = 0
    for name, target, dtype, required, fmt in fields:
        body = {"sourceFieldName": name, "targetGrField": target,
                "dataType": dtype, "required": required}
        if fmt:
            body["sourceDateFormat"] = fmt
        status, payload = request(
            "POST",
            f"{STAGE}/internal/api/v1/sources/{source_id}/contract/fields?entityType={entity_type}",
            body, admin(token))
        if status == 201:
            created += 1
        elif status == 400 and "Unknown Golden Record field" in str(payload):
            sys.exit(f"Каталог полей не знает {target}: {payload}")
    print(f"  контракт {entity_type}: добавлено правил — {created} из {len(fields)}")


def person(rng, idx):
    female = rng.random() < 0.5
    last = rng.choice(LAST_NAMES)
    if female and not last.endswith(("а", "я")):
        last += "а"
    birth = date(1960, 1, 1) + timedelta(days=rng.randrange(0, 365 * 45))
    issued = date(2015, 1, 1) + timedelta(days=rng.randrange(0, 365 * 9))
    first = rng.choice(FIRST_F if female else FIRST_M)
    return {
        "last_name": last,
        "first_name": first,
        "middle_name": rng.choice(MIDDLE_F if female else MIDDLE_M),
        # Выдуманные реквизиты: случайные цифры, ничьим настоящим не соответствуют.
        "pinfl": f"{rng.randrange(3, 7)}{rng.randrange(10**12, 10**13 - 1)}",
        "gender": "F" if female else "M",
        "birth_date": birth.isoformat(),
        "phone": f"9989{rng.randrange(10**8, 10**9 - 1)}",
        "email": f"demo.user{idx}@example.invalid",
        "citizenship": "Узбекистан",
        "citizenship_id": "860",
        "passport": f"{rng.choice('ABCK')}{rng.choice('ABCK')}{rng.randrange(1000000, 9999999)}",
        "passport_issued_by": rng.choice(REGIONS),
        "passport_issued_by_id": str(rng.randrange(10000, 99999)),
        "passport_issued_date": issued.isoformat(),
    }


def merchant(rng, name):
    opf, opf_code = rng.choice(OPF)
    oked_code, oked_name = rng.choice(OKED)
    return {
        "inn": str(rng.randrange(100000000, 999999999)),
        "full_name": f'{opf} "{name}"',
        "short_name": name,
        "opf_name": opf,
        "opf_code": opf_code,
        "oked_code": oked_code,
        "oked_name": oked_name,
        "is_active": rng.random() > 0.15,
        "is_bankrupt": rng.random() < 0.05,
    }


def push(path, source_token, data, dry):
    if dry:
        return True
    status, payload = request("POST", f"{GATEWAY}{path}", {"data": data},
                              {"X-Access-Token": source_token})
    if status != 200:
        print(f"    отклонено ({status}): {payload}")
        return False
    return True


def main():
    ap = argparse.ArgumentParser(description="Засев демонстрационного стенда")
    ap.add_argument("--persons", type=int, default=40)
    ap.add_argument("--merchants", type=int, default=12)
    ap.add_argument("--seed", type=int, default=20261108)
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    env = {}
    try:
        for line in open(".env"):
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                v = v.strip()
                # Compose снимает окружающие кавычки, здесь тоже — иначе токен
                # уедет в заголовок вместе с ними.
                if len(v) > 1 and v[0] == v[-1] and v[0] in "\"'":
                    v = v[1:-1]
                env[k.strip()] = v
    except FileNotFoundError:
        sys.exit("Нет .env рядом с docker-compose.demo.yml")

    token = env.get("ADMIN_TOKEN")
    source_token = env.get("DEMO_SOURCE_TOKEN")
    if not token or not source_token:
        sys.exit("В .env нужны ADMIN_TOKEN и DEMO_SOURCE_TOKEN")

    rng = random.Random(args.seed)
    print("Выдуманные данные, фиксированное зерно:", args.seed)

    source_ids = {}
    for i, spec in enumerate(SOURCES):
        print(f"Источник {spec['code']}:")
        tok = source_token if i == 0 else f"{source_token}-{spec['code']}"
        sid = ensure_source(spec, token, tok, args.dry_run)
        source_ids[spec["code"]] = (sid, tok)
        register_in_core(spec["code"], spec["trustLevel"], args.dry_run)
        if sid:
            ensure_contract(sid, PERSON_FIELDS, "PERSON", token, args.dry_run)
            ensure_contract(sid, LEGAL_FIELDS, "LEGAL_ENTITY", token, args.dry_run)

    primary = SOURCES[0]["code"]
    secondary = SOURCES[1]["code"]
    _, primary_token = source_ids[primary]
    _, secondary_token = source_ids[secondary]

    print(f"Физические лица: {args.persons}")
    ok = 0
    people = [person(rng, i) for i in range(args.persons)]
    for p in people:
        if push("/api/v1/client-data", primary_token, p, args.dry_run):
            ok += 1
    print(f"  принято: {ok} из {args.persons}")

    # Каждая пятая карточка приходит вторым источником с другой почтой и
    # телефоном: так на стенде появляются и слияние, и провенанс полей.
    overlap = people[::5]
    print(f"Повторный приём вторым источником: {len(overlap)}")
    ok = 0
    for i, p in enumerate(overlap):
        twin = dict(p, email=f"crm.user{i}@example.invalid",
                    phone=f"9989{rng.randrange(10**8, 10**9 - 1)}")
        if push("/api/v1/client-data", secondary_token, twin, args.dry_run):
            ok += 1
    print(f"  принято: {ok} из {len(overlap)}")

    print(f"Юридические лица: {args.merchants}")
    ok = 0
    for name in MERCHANTS[:args.merchants]:
        if push("/api/v1/legal-data", primary_token,
                merchant(rng, name), args.dry_run):
            ok += 1
    print(f"  принято: {ok} из {min(args.merchants, len(MERCHANTS))}")

    print("Готово." if not args.dry_run else "Это был разбор без записи.")


if __name__ == "__main__":
    main()
