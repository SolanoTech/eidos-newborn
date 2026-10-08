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
Проверка частичного merge по trust-уровню для юридических лиц.

Сценарий зеркалит merge_test.py, но у юрлиц другой ключ идентификации:
  1. loadtest (trust 8) создаёт запись мерчанта по фиксированному ИНН.
  2. bank-1 (trust 10 > 8) присылает ТОТ ЖЕ ИНН с другим названием и e-mail.
  3. core перезаписывает только изменённые поля и переключает их провенанс на
     bank-1; неизменные остаются за loadtest.

Ключевое отличие от физлиц: ИНН самодостаточен, поэтому смена названия
мерчанта не порождает новую запись — она приводит к merge существующей.

Требует контрактов LEGAL_ENTITY у обоих источников:
    python3 setup.py --legal                  # для loadtest
    python3 setup.py --source bank-1 --legal   # для bank-1

Запуск:
    python3 merge_test_legal.py
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
INN = "309876543"


def reset_record() -> None:
    """
    Чистит тестового мерчанта (и связанные external-id / field-meta / архив /
    tentative), чтобы прогон был воспроизводимым. Best-effort через docker psql.
    """
    sql = f"""
    DELETE FROM legal_entity_external_id WHERE gr_legal_entity_id IN
        (SELECT gr_legal_entity_id FROM legal_entity_record WHERE gr_inn='{INN}');
    DELETE FROM legal_entity_field_meta WHERE gr_legal_entity_id IN
        (SELECT gr_legal_entity_id FROM legal_entity_record WHERE gr_inn='{INN}');
    DELETE FROM legal_entity_archive WHERE gr_legal_entity_id IN
        (SELECT gr_legal_entity_id FROM legal_entity_record WHERE gr_inn='{INN}');
    DELETE FROM tentative_legal_entity WHERE gr_inn='{INN}';
    DELETE FROM legal_entity_record WHERE gr_inn='{INN}';
    """
    cmd = ["docker", "compose", "exec", "-T", "postgres",
           "psql", "-U", "postgres", "-d", "eidos_core", "-c", sql]
    try:
        r = subprocess.run(cmd, cwd=os.path.dirname(HERE), capture_output=True, timeout=30)
        if r.returncode == 0:
            print(f"✓ тестовый мерчант с ИНН {INN} очищен")
        else:
            print("! не удалось очистить запись автоматически:", r.stderr.decode()[:200])
    except Exception as e:
        print("! docker недоступен, очистка пропущена:", e)


# Поля, одинаковые в обоих пушах: по ИНН core находит существующую запись,
# остальное показывает, что неизменные поля не меняют владельца.
COMMON = {
    "merchant_id": "MERGE-LEGAL",
    "tin": INN,
    "active": "true",
    "bankrupt": "false",
    "oked": "47110",
    "oked_name": "Розничная торговля",
    "reg_date": "05.05.2016",
    "region": "г. Ташкент",
}

BASELINE = {**COMMON,
            "name": 'ООО "Мерж Тест"',
            "brand": "МержТест",
            "email": "base.loadtest@example.uz"}

UPDATE = {**COMMON,
          "merchant_id": "MERGE-LEGAL-PM",
          "name": 'ООО "Мерж Тест Плюс"',
          "brand": "МержТест",
          "email": "updated.bank-1@example.uz"}


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
    """Канал юрлиц: отдельный эндпоинт, тип в теле не передаётся."""
    body = json.dumps({"data": card}).encode()
    req = urllib.request.Request(f"{GATEWAY}/api/v1/legal-data", data=body, method="POST",
                                 headers={"Content-Type": "application/json",
                                          "X-Access-Token": token})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            r.read()
    except urllib.error.HTTPError as e:
        sys.exit(f"✗ push отклонён: HTTP {e.code} {e.read().decode(errors='replace')[:300]}")


def fetch_record() -> dict:
    return _get(f"{CORE}/api/v1/internal/legal-records/search/inn?inn={INN}")


def provenance(record_id: str) -> dict:
    meta = _get(f"{CORE}/api/v1/internal/legal-records/{record_id}/field-meta")
    return {m["fieldName"]: m["sourceName"] for m in meta}


def show(title: str, rec: dict) -> None:
    prov = provenance(rec["grLegalEntityId"])
    def owner(field: str) -> str:
        return prov.get(field, "?")
    print(f"\n── {title} ──  {rec['grLegalEntityId']} (version {rec.get('version')})")
    print("  ИЗМЕНЯЕМЫЕ bank-1:")
    print(f"    название   : {str(rec.get('grFullName')):28} [источник: {owner('grFullName')}]")
    print(f"    e-mail     : {str(rec.get('grEmail')):28} [источник: {owner('grEmail')}]")
    print("  НЕИЗМЕННЫЕ (те же значения в обеих карточках):")
    print(f"    ИНН        : {str(rec.get('grInn')):28} [источник: {owner('grInn')}]")
    print(f"    ОКЭД       : {str(rec.get('grOkedName')):28} [источник: {owner('grOkedName')}]")
    print(f"    регион     : {str(rec.get('grRegionName')):28} [источник: {owner('grRegionName')}]")


def main() -> None:
    lt = source_token("loadtest")
    pm = source_token("bank-1")

    reset_record()

    print("1) loadtest (trust 8) создаёт запись мерчанта…")
    push(BASELINE, lt)
    show("ПОСЛЕ loadtest", fetch_record())

    print("\n2) bank-1 (trust 10) присылает тот же ИНН с другим названием и e-mail…")
    push(UPDATE, pm)
    after = fetch_record()
    show("ПОСЛЕ bank-1", after)

    prov = provenance(after["grLegalEntityId"])
    changed_ok = (after.get("grFullName") == UPDATE["name"]
                  and prov.get("grFullName") == "bank-1"
                  and prov.get("grEmail") == "bank-1")
    unchanged_ok = (prov.get("grInn") == "loadtest"
                    and prov.get("grOkedName") == "loadtest"
                    and prov.get("grRegionName") == "loadtest")

    print("\n" + "=" * 60)
    if changed_ok and unchanged_ok:
        print("  ✓ ЧАСТИЧНЫЙ MERGE ЮРЛИЦА: изменённые поля (название, e-mail) → bank-1,")
        print("    неизменные (ИНН, ОКЭД, регион) остались за loadtest.")
        print("  ✓ Смена названия при том же ИНН дала merge, а не дубликат.")
    else:
        print("  ✗ Не то, что ожидалось:")
        print(f"    изменённые→bank-1: {changed_ok}, неизменные→loadtest: {unchanged_ok}")
    print("=" * 60)


if __name__ == "__main__":
    main()
