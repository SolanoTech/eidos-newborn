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
Ручная отправка ОДНОЙ карточки в Kafka (для отладки).

Формат сообщения: {"data": { ...поля источника... }} — БЕЗ поля source:
источник gateway проставляет сам из токена (заголовок X-Access-Token).
Поля внутри data должны совпадать с sourceFieldName контракта (см. setup.py):
phone, first_name, last_name, pinfl, gender, birth_date, passport, ...

    python3 produce.py            # случайная валидная карточка
    python3 produce.py --raw      # минимальный ручной набор полей (для проверки валидации)
"""
import argparse
import json

import mocks
from loadtest import KafkaSender, load_config

ap = argparse.ArgumentParser()
ap.add_argument("--raw", action="store_true", help="послать заведомо неполный набор полей")
args = ap.parse_args()

cfg = load_config()
bootstrap = cfg.get("bootstrap_host", "localhost:19092")
sender = KafkaSender(bootstrap, cfg["topic"], cfg["token"])

if args.raw:
    # Неполная карточка — gateway отклонит по контракту (missing required fields),
    # но НЕ упадёт: сообщение просто пропустится (см. лог "Skipping Kafka message").
    data = {"pinfl": "61157669271732", "phone": "998943118881"}
else:
    data = mocks.make_card()   # полная валидная карточка

sender.send({"data": data})    # source добавит gateway
sender.close()
print("Отправлено:", json.dumps({"data": data}, ensure_ascii=False)[:200])
