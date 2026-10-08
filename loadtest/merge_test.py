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
Проверка merge по trust-уровню (серая зона / перезапись).

Сценарий:
  1. loadtest (trust 8) создаёт Золотую запись по фиксированному человеку
     (ПИНФЛ 61157669271732), телефон/e-mail = «loadtest».
  2. bank-1 (trust 10 > 8) присылает ТОГО ЖЕ человека (те же ФИО+дата+ПИНФЛ,
     иначе точный поиск core не сматчит) с ДРУГИМИ телефоном/e-mail.
  3. core перезаписывает поля значениями bank-1 и переключает провенанс на bank-1.
  4. Поля, которые оба источника прислали одинаковыми, тоже переходят к bank-1:
     более доверенный источник подтвердил значение, и оно закрепляется за ним,
     чтобы менее доверенный не мог позже его переписать. Данные при этом не
     меняются, поэтому версия записи не растёт и снимок в архив не пишется.

Отправка идёт синхронно через gateway REST (POST /api/v1/client-data), поэтому
после ответа 200 запись уже в core — не нужно поллить. Источник gateway
проставляет сам из токена; токены источников берём из admin API stage.

Требует полного контракта у bank-1:
    python3 setup.py --source bank-1 --reset-fields

Запуск:
    python3 merge_test.py
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

from setup import STAGE, GATEWAY, ADMIN_TOKEN, HERE
CORE = "http://localhost:8080"

PINFL = "61157669271732"


def reset_record() -> None:
    """
    Удаляет тестовую Золотую запись по ПИНФЛ (и связанные external-id / field-meta
    / tentative), чтобы прогон был чистым: loadtest создаёт запись заново, bank-1
    перезаписывает. Best-effort через docker psql.
    """
    sql = f"""
    DELETE FROM golden_record_external_id WHERE gr_client_id IN
        (SELECT gr_client_id FROM golden_record WHERE gr_pinfl='{PINFL}');
    DELETE FROM golden_record_field_meta WHERE gr_client_id IN
        (SELECT gr_client_id FROM golden_record WHERE gr_pinfl='{PINFL}');
    DELETE FROM tentative_golden_record WHERE gr_pinfl='{PINFL}';
    DELETE FROM golden_record WHERE gr_pinfl='{PINFL}';
    """
    cmd = ["docker", "compose", "exec", "-T", "postgres",
           "psql", "-U", "postgres", "-d", "eidos_core", "-c", sql]
    try:
        r = subprocess.run(cmd, cwd=os.path.dirname(HERE), capture_output=True, timeout=30)
        if r.returncode == 0:
            print(f"✓ тестовая запись по ПИНФЛ {PINFL} очищена")
        else:
            print("! не удалось очистить запись автоматически:", r.stderr.decode()[:200])
    except Exception as e:
        print("! docker недоступен, очистка пропущена:", e)

# Идентифицирующие поля ОДИНАКОВЫ в обоих пушах — по ним core матчит человека.
IDENTITY = {
    "first_name": "Тест", "last_name": "Мержов", "middle_name": "Проверкович",
    "pinfl": PINFL, "gender": "M", "birth_date": "15.05.1990",
    "citizenship": "Узбекистан", "citizenship_id": "860",
    "passport": "AA1234567", "passport_issued_by": "ГУВД г. Ташкента",
    "passport_issued_by_id": "1234", "passport_issued_date": "20.06.2015",
}

BASELINE = {**IDENTITY, "client_id": "MERGE-BASE",
            "phone": "998900000001", "email": "base.loadtest@example.uz",
            "address": "Базовый адрес (loadtest)"}

UPDATE = {**IDENTITY, "client_id": "MERGE-PAYME",
          "phone": "998900999999", "email": "updated.bank-1@example.uz",
          "address": "Обновлённый адрес (bank-1)"}


def _get(url: str, admin: bool = False):
    req = urllib.request.Request(url)
    if admin:
        req.add_header("X-Admin-Token", ADMIN_TOKEN)
    with urllib.request.urlopen(req, timeout=15) as r:
        return json.loads(r.read())


def source_token(code: str) -> str:
    for s in _get(f"{STAGE}/internal/api/v1/sources", admin=True):
        if s["code"] == code:
            return s["token"]
    sys.exit(f"источник {code} не найден в реестре — запусти setup.py")


def push(card: dict, token: str) -> None:
    body = json.dumps({"data": card}).encode()      # source проставит gateway
    req = urllib.request.Request(f"{GATEWAY}/api/v1/client-data", data=body, method="POST",
                                 headers={"Content-Type": "application/json", "X-Access-Token": token})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            r.read()
    except urllib.error.HTTPError as e:
        sys.exit(f"✗ push отклонён: HTTP {e.code} {e.read().decode(errors='replace')[:300]}")


def fetch_record() -> dict | None:
    page = _get(f"{CORE}/api/v1/internal/golden-records/search/structured?pinfl={PINFL}")
    items = page.get("content", []) if isinstance(page, dict) else page
    return items[0] if items else None


def provenance(client_id: str) -> dict:
    meta = _get(f"{CORE}/api/v1/internal/golden-records/{client_id}/field-meta")
    return {m["fieldName"]: m["sourceName"] for m in meta}


def show(title: str, rec: dict) -> None:
    prov = provenance(rec["grClientId"])
    def owner(sub):
        return next((v for k, v in prov.items() if sub.lower() in k.lower().replace("_","")), "?")
    print(f"\n── {title} ──  grClientId={rec['grClientId']} (version {rec.get('version')})")
    print("  ИЗМЕНЯЕМЫЕ bank-1:")
    print(f"    телефон    : {rec.get('grMobilePhoneMain')!s:24} [источник: {owner('phone')}]")
    print(f"    e-mail     : {rec.get('grContactsEmail')!s:24} [источник: {owner('email')}]")
    print("  ПОДТВЕРЖДЁННЫЕ (те же значения в обеих карточках):")
    print(f"    имя        : {rec.get('grFirstName')!s:24} [источник: {owner('firstname')}]")
    print(f"    фамилия    : {rec.get('grLastName')!s:24} [источник: {owner('lastname')}]")
    print(f"    паспорт    : {rec.get('grDocPassData')!s:24} [источник: {owner('passdata')}]")


def main() -> None:
    lt = source_token("loadtest")
    pm = source_token("bank-1")

    reset_record()
    print("1) loadtest (trust 8) создаёт запись…")
    push(BASELINE, lt)
    base = fetch_record()
    if not base:
        sys.exit("✗ базовая запись не появилась в core")
    show("ПОСЛЕ loadtest", base)

    print("\n2) bank-1 (trust 10) присылает того же человека с другими телефоном/e-mail…")
    push(UPDATE, pm)
    after = fetch_record()
    show("ПОСЛЕ bank-1", after)

    prov = provenance(after["grClientId"])
    def owner(sub): return next((v for k, v in prov.items() if sub.lower() in k.lower().replace("_","")), "?")

    changed_ok = (after.get("grMobilePhoneMain") == UPDATE["phone"]
                  and owner("phone") == "bank-1"
                  and owner("email") == "bank-1")
    confirmed_ok = (owner("firstname") == "bank-1"
                    and owner("lastname") == "bank-1"
                    and owner("passdata") == "bank-1")
    print("\n" + ("=" * 56))
    if changed_ok and confirmed_ok:
        print("  ✓ MERGE ПО TRUST: изменённые поля (телефон, e-mail) → bank-1,")
        print("    подтверждённые (имя, фамилия, паспорт) закреплены за bank-1 —")
        print("    менее доверенный источник больше не сможет их переписать.")
    else:
        print("  ✗ Не то, что ожидалось:")
        print(f"    изменённые→bank-1: {changed_ok}, подтверждённые→bank-1: {confirmed_ok}")
    print("=" * 56)


if __name__ == "__main__":
    main()
