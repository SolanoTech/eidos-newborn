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
Генератор моков клиентских карточек для источника `loadtest`.

Формат карточки — «сырой», в терминах полей источника (см. контракт в
setup.py). Envelope, который уходит в Kafka / REST:

    {"data": { ...поля источника... }, "source": "loadtest"}

Значения подобраны так, чтобы проходить и валидацию контракта на gateway
(regex/дата/обязательность), и bean-validation Золотой записи на stage:
  * phone     — 998XXXXXXXXX (12 цифр);
  * pinfl     — ровно 14 цифр;
  * passport  — AA1234567 (2 заглавные буквы + 7 цифр);
  * gender    — "M"/"F" (в контракте value-map → M/F);
  * даты      — формат dd.MM.yyyy (в контракте DATE, source_date_format).

Каждая карточка — НОВЫЙ человек (уникальный pinfl, паспорт и client_id),
чтобы нагрузочный тест мерил чистое *создание* Золотой записи, а не merge.
"""
from __future__ import annotations

import argparse
import json
import random
import string
import time
import uuid
from datetime import date, timedelta

# --- Справочники для правдоподобных значений -------------------------------

FIRST_NAMES = ["Азиз", "Дилшод", "Шерзод", "Бекзод", "Фаррух", "Отабек",
               "Нодира", "Малика", "Гульнора", "Дилдора", "Севара", "Зарина"]
LAST_NAMES = ["Каримов", "Юсупов", "Рахимов", "Абдуллаев", "Тошматов",
              "Эргашев", "Саидова", "Исмоилова", "Назарова", "Холматова"]
MIDDLE_NAMES = ["Азизович", "Бекзодович", "Шерзодовна", "Фаррухжоновна", ""]
REGIONS = ["г. Ташкент, Юнусабадский р-н", "Самаркандская обл.",
           "Бухарская обл.", "Ферганская обл., г. Коканд", "Андижанская обл."]
ISSUERS = ["ГУВД г. Ташкента", "ИИБ Самаркандской обл.",
           "ИИБ Бухарской обл.", "ГУВД Ферганской обл."]


def _pinfl() -> str:
    """14 цифр, первая ненулевая."""
    return str(random.randint(1, 9)) + "".join(random.choices(string.digits, k=13))


def _phone() -> str:
    """998 + 9 цифр (мобильные коды 90/91/93/94/97/98/99/33/88)."""
    code = random.choice(["90", "91", "93", "94", "97", "98", "99", "33", "88"])
    return "998" + code + "".join(random.choices(string.digits, k=7))


def _passport() -> str:
    """AA1234567 — серия из 2 заглавных латинских + 7 цифр."""
    series = "".join(random.choices(string.ascii_uppercase, k=2))
    return series + "".join(random.choices(string.digits, k=7))


def _dob() -> date:
    start = date(1960, 1, 1)
    return start + timedelta(days=random.randint(0, 365 * 45))


def make_card(seq: object | None = None) -> dict:
    """
    Одна валидная карточка. `seq` (если задан) уходит в client_id для удобной
    трассировки в логах/БД; иначе — случайный UUID-суффикс.
    """
    tag = str(seq) if seq is not None else uuid.uuid4().hex[:10]
    dob = _dob()
    issued = dob.replace(year=dob.year + 18) + timedelta(days=random.randint(0, 4000))
    gender = random.choice(["M", "F"])
    first = random.choice(FIRST_NAMES)
    last = random.choice(LAST_NAMES)

    return {
        "client_id": f"LT-{tag}",
        "phone": _phone(),
        "first_name": first,
        "last_name": last,
        "middle_name": random.choice(MIDDLE_NAMES),
        "pinfl": _pinfl(),
        "gender": gender,
        "birth_date": dob.strftime("%d.%m.%Y"),
        "citizenship": "Узбекистан",
        "citizenship_id": "860",
        "passport": _passport(),
        "passport_issued_by": random.choice(ISSUERS),
        "passport_issued_by_id": str(random.randint(1000, 9999)),
        "passport_issued_date": issued.strftime("%d.%m.%Y"),
        "email": f"{first.lower()}.{last.lower()}{tag}@example.uz",
        "address": random.choice(REGIONS) + f", ул. Тестовая, д. {random.randint(1, 200)}",
    }


def make_envelope(card: dict, source: str = "loadtest") -> dict:
    """
    Оборачивает карточку в формат сообщения stage/gateway.

    Поле source здесь — для прямых вызовов stage; через gateway оно всё равно
    перезаписывается по токену, а тип сущности определяется каналом, а не телом.
    """
    return {"data": card, "source": source}


# --- Юридические лица (мерчанты) -------------------------------------------

MERCHANT_WORDS = ["Оазис", "Самарканд", "Бухоро", "Нур", "Барака", "Зафар",
                  "Ситора", "Мехр", "Олтин", "Гулистон", "Чинор", "Дилором"]
MERCHANT_KINDS = ["Маркет", "Савдо", "Групп", "Трейд", "Логистик", "Импэкс",
                  "Сервис", "Строй", "Фуд", "Текстиль"]
OPF = [("ООО", "Общество с ограниченной ответственностью"),
       ("МЧЖ", "Масъулияти чекланган жамият"),
       ("ЧП", "Частное предприятие"),
       ("АО", "Акционерное общество")]
OKED = [("47110", "Розничная торговля в неспециализированных магазинах"),
        ("46900", "Неспециализированная оптовая торговля"),
        ("56101", "Деятельность ресторанов"),
        ("49410", "Деятельность автомобильного грузового транспорта"),
        ("62010", "Разработка программного обеспечения")]
REGIONS_UZ = ["г. Ташкент", "Самаркандская обл.", "Бухарская обл.",
              "Ферганская обл.", "Андижанская обл.", "Наманганская обл."]


def _inn() -> str:
    """ИНН юрлица в Узбекистане — ровно 9 цифр, первая ненулевая."""
    return str(random.randint(1, 9)) + "".join(random.choices(string.digits, k=8))


def make_merchant(seq: object | None = None) -> dict:
    """
    Одна валидная карточка юрлица в терминах полей источника (см. контракт в
    setup.py --legal). Каждый вызов — новый мерчант: уникальный ИНН и
    merchant_id, чтобы нагрузочный тест мерил создание, а не merge.
    """
    tag = str(seq) if seq is not None else uuid.uuid4().hex[:10]
    opf_short, opf_full = random.choice(OPF)
    oked_code, oked_name = random.choice(OKED)
    brand = f"{random.choice(MERCHANT_WORDS)} {random.choice(MERCHANT_KINDS)}"
    reg = date(2005, 1, 1) + timedelta(days=random.randint(0, 7000))

    return {
        "merchant_id": f"LT-M-{tag}",
        "tin": _inn(),
        "name": f'{opf_short} "{brand}"',
        "brand": brand,
        "opf_code": str(random.randint(100, 999)),
        "opf_name": opf_full,
        "active": "true",
        "bankrupt": "false",
        "oked": oked_code,
        "oked_name": oked_name,
        "reg_date": reg.strftime("%d.%m.%Y"),
        "reg_number": str(random.randint(100000, 999999)),
        "capital": f"{random.randint(1, 500) * 1_000_000}.00",
        "address": f"{random.choice(REGIONS_UZ)}, ул. Мерчантская, д. {random.randint(1, 200)}",
        "region": random.choice(REGIONS_UZ),
        "email": f"info@{brand.split()[0].lower()}{tag}.uz",
        "director": f"{random.choice(LAST_NAMES)} {random.choice(FIRST_NAMES)[0]}.",
        "contacts": {"phones": [_phone() for _ in range(random.randint(1, 3))]},
        "owners": [
            {"name": f"{random.choice(LAST_NAMES)} {random.choice(FIRST_NAMES)[0]}.",
             "pinfl": _pinfl(),
             "sharePercent": 100.00}
        ],
    }


def make_invalid_merchants() -> list[tuple[str, dict]]:
    """(описание, карточка) — каждая нарушает ровно одно правило контракта."""
    base = make_merchant("BAD")
    out: list[tuple[str, dict]] = []

    c = dict(base); c["tin"] = "12345"                      # не 9 цифр
    out.append(("ИНН не 9 цифр", c))

    c = dict(base); c.pop("name")                           # нет обязательного
    out.append(("отсутствует name (обязательное)", c))

    c = dict(base); c.pop("oked")
    out.append(("отсутствует ОКЭД (обязательное)", c))

    c = dict(base); c["reg_date"] = "2019-13-45"            # не парсится dd.MM.yyyy
    out.append(("reg_date не парсится", c))

    c = dict(base); c["contacts"] = {"phones": "998901234567"}  # строка вместо массива
    out.append(("phones не массив", c))

    return out


# --- Заведомо НЕвалидные карточки для негативных проверок -------------------

def make_invalid_cards() -> list[tuple[str, dict]]:
    """(описание, карточка) — каждая нарушает ровно одно правило контракта."""
    base = make_card("BAD")
    out: list[tuple[str, dict]] = []

    c = dict(base); c["pinfl"] = "12345"                       # не 14 цифр
    out.append(("pinfl не 14 цифр", c))

    c = dict(base); c["phone"] = "998123"                      # короткий телефон
    out.append(("phone не 998XXXXXXXXX", c))

    c = dict(base); c["passport"] = "1234567AA"                # неверный формат
    out.append(("passport не AA1234567", c))

    c = dict(base); c.pop("last_name")                         # нет обязательного
    out.append(("отсутствует last_name (обязательное)", c))

    c = dict(base); c["birth_date"] = "1990-13-45"             # не парсится dd.MM.yyyy
    out.append(("birth_date не парсится", c))

    return out


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description="Печатает моки карточек (JSON).")
    ap.add_argument("-n", "--count", type=int, default=1, help="сколько карточек")
    ap.add_argument("--envelope", action="store_true", help="обернуть в {data, source}")
    ap.add_argument("--invalid", action="store_true", help="печатать невалидные примеры")
    args = ap.parse_args()

    if args.invalid:
        for desc, card in make_invalid_cards():
            print(f"# {desc}")
            print(json.dumps(make_envelope(card) if args.envelope else card, ensure_ascii=False))
        raise SystemExit

    for i in range(args.count):
        card = make_card(int(time.time_ns()) % 1_000_000 + i)
        obj = make_envelope(card) if args.envelope else card
        print(json.dumps(obj, ensure_ascii=False))
