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
Нагрузочный тест Eidos: пуш карточек и замер времени от отправки до появления
Золотой записи в eidos-core.

Как меряется задержка (push → Golden Record):
  * фиксируем t0 в момент отправки карточки (Kafka producer.send+flush либо
    HTTP POST на gateway);
  * поллим core `GET /api/v1/external/golden-records/search/source-id?
    source=<code>&source_id=<client_id>` пока не вернётся 200 (запись создана);
  * задержка = t_found − t0.

Каждая карточка — новый человек (уникальный pinfl/client_id), поэтому мерим
чистое создание записи, а не merge.

Два режима транспорта:
  --mode kafka  (по умолчанию) — продюсер в топик, заголовок X-Access-Token;
                 путь: Kafka → gateway-consumer → stage → core.
  --mode rest   — POST на gateway с X-Access-Token;
                 путь: gateway (валидация) → stage → core.

Две вертикали (--entity):
  person (по умолчанию) — клиенты: топик client-data / POST /api/v1/client-data,
                 приземление проверяется по внешнему идентификатору источника.
  legal          — мерчанты: топик legal-data / POST /api/v1/legal-data,
                 приземление проверяется по ИНН.
Тип сущности задаёт канал: в теле сообщения его нет и быть не может.

Зависимости: только для режима kafka нужен пакет `kafka-python`
(`pip install -r requirements.txt`). Режим rest — чистая стандартная библиотека.

Примеры:
    python3 loadtest.py --count 200 --concurrency 8
    python3 loadtest.py --mode rest --count 500 --concurrency 16
    python3 loadtest.py --count 1000 --rate 50        # ~50 карточек/сек
    python3 loadtest.py --entity legal --count 200    # мерчанты через Kafka
    python3 loadtest.py --entity legal --mode rest --count 100
"""
from __future__ import annotations

import argparse
import json
import os
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed

import mocks

HERE = os.path.dirname(os.path.abspath(__file__))

# Уникальный префикс прогона. Без него идентификаторы источника (client_id /
# merchant_id) повторяются между запусками, core находит запись прошлого
# прогона по external id и делает merge вместо создания — тест мерил бы
# повторный поиск, а не приём.
RUN_ID = uuid.uuid4().hex[:6]
CONFIG_PATH = os.path.join(HERE, "config.json")


def load_config() -> dict:
    if not os.path.exists(CONFIG_PATH):
        sys.exit("Нет config.json — сначала запусти: python3 setup.py")
    with open(CONFIG_PATH, encoding="utf-8") as f:
        return json.load(f)


# --- Транспорты отправки ----------------------------------------------------

class KafkaSender:
    """Общий на все потоки продюсер (KafkaProducer потокобезопасен)."""

    def __init__(self, bootstrap: str, topic: str, token: str):  # noqa: D401
        try:
            from kafka import KafkaProducer  # noqa: PLC0415
        except ImportError:
            sys.exit("Режиму kafka нужен пакет kafka-python: pip install -r requirements.txt")
        self.topic = topic
        self.headers = [("X-Access-Token", token.encode())]
        self.producer = KafkaProducer(
            bootstrap_servers=bootstrap,
            value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode(),
            acks=1, linger_ms=5, retries=3,
        )

    def send(self, envelope: dict) -> None:
        future = self.producer.send(self.topic, value=envelope, headers=self.headers)
        future.get(timeout=30)  # ждём подтверждения брокера — это и есть t0-барьер

    def close(self) -> None:
        self.producer.flush()
        self.producer.close()


class RestSender:
    def __init__(self, gateway: str, token: str, path: str = "/api/v1/client-data"):
        self.url = gateway.rstrip("/") + path
        self.token = token

    def send(self, envelope: dict) -> None:
        data = json.dumps(envelope, ensure_ascii=False).encode()
        req = urllib.request.Request(self.url, data=data, method="POST")
        req.add_header("Content-Type", "application/json")
        req.add_header("X-Access-Token", self.token)
        with urllib.request.urlopen(req, timeout=30) as resp:
            resp.read()

    def close(self) -> None:
        pass


# --- Поллинг появления Золотой записи ---------------------------------------

def landing_url(core: str, source: str, entity: str, key: str) -> str:
    """
    Где искать приземлившуюся запись. У физлиц — по внешнему идентификатору
    источника, у юрлиц — по ИНН: это их самодостаточный ключ.
    """
    base = core.rstrip("/")
    if entity == "legal":
        return f"{base}/api/v1/internal/legal-records/search/inn?inn={key}"
    return f"{base}/api/v1/external/golden-records/search/source-id?source={source}&source_id={key}"


def poll_landed(url: str, deadline: float, interval: float) -> float | None:
    """Возвращает t_found (монотонное), либо None если не появилась к deadline."""
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=10) as resp:
                if resp.status == 200 and resp.read():
                    return time.monotonic()
        except urllib.error.HTTPError as e:
            if e.code not in (404, 204):
                # 4xx/5xx кроме "ещё нет" — считаем фатальным для этой карточки
                return None
        except Exception:
            pass
        time.sleep(interval)
    return None


# --- Один прогон карточки: отправка + ожидание записи -----------------------

class Result:
    __slots__ = ("client_id", "latency", "sent_ok", "landed")

    def __init__(self, client_id: str):
        self.client_id = client_id
        self.latency: float | None = None
        self.sent_ok = False
        self.landed = False


def run_one(sender, cfg: dict, seq: int, timeout: float, interval: float,
            rate_gate: "RateLimiter | None", entity: str = "person") -> Result:
    tag = f"{RUN_ID}-{seq}"
    if entity == "legal":
        card = mocks.make_merchant(tag)
        key = card["tin"]          # ИНН — ключ поиска приземления
    else:
        card = mocks.make_card(tag)
        key = card["client_id"]
    r = Result(key)
    if rate_gate:
        rate_gate.acquire()
    t0 = time.monotonic()
    try:
        sender.send(mocks.make_envelope(card, cfg["source"]))
        r.sent_ok = True
    except Exception as e:  # noqa: BLE001
        r.latency = None
        print(f"  ! отправка {key} не удалась: {e}", file=sys.stderr)
        return r
    t_found = poll_landed(landing_url(cfg["core"], cfg["source"], entity, key),
                          deadline=t0 + timeout, interval=interval)
    if t_found is not None:
        r.landed = True
        r.latency = t_found - t0
    return r


class RateLimiter:
    """Простой ограничитель: не более `rate` разрешений в секунду."""

    def __init__(self, rate: float):
        self.min_interval = 1.0 / rate
        self.lock = threading.Lock()
        self.next_at = time.monotonic()

    def acquire(self) -> None:
        with self.lock:
            now = time.monotonic()
            wait = self.next_at - now
            if wait > 0:
                time.sleep(wait)
                self.next_at += self.min_interval
            else:
                self.next_at = now + self.min_interval


# --- Отчёт ------------------------------------------------------------------

def pct(sorted_vals: list[float], p: float) -> float:
    if not sorted_vals:
        return float("nan")
    k = (len(sorted_vals) - 1) * p
    lo = int(k)
    hi = min(lo + 1, len(sorted_vals) - 1)
    return sorted_vals[lo] + (sorted_vals[hi] - sorted_vals[lo]) * (k - lo)


def report(results: list[Result], wall: float, mode: str) -> None:
    total = len(results)
    landed = [r for r in results if r.landed]
    lats = sorted(r.latency for r in landed)
    sent_fail = sum(1 for r in results if not r.sent_ok)
    not_landed = sum(1 for r in results if r.sent_ok and not r.landed)

    print("\n" + "=" * 56)
    print(f"  НАГРУЗОЧНЫЙ ТЕСТ — режим {mode}")
    print("=" * 56)
    print(f"  Карточек отправлено:     {total}")
    print(f"  Приземлилось в Golden:   {len(landed)}")
    print(f"  Ошибок отправки:         {sent_fail}")
    print(f"  Не дождались записи:     {not_landed}")
    print(f"  Общее время (wall):      {wall:.2f} c")
    if wall > 0:
        print(f"  Пропускная способность:  {len(landed) / wall:.1f} карточек/с")
    if lats:
        print("  --- Задержка push → Golden Record (сек) ---")
        print(f"    min    {lats[0]:.3f}")
        print(f"    avg    {statistics.fmean(lats):.3f}")
        print(f"    p50    {pct(lats, 0.50):.3f}")
        print(f"    p90    {pct(lats, 0.90):.3f}")
        print(f"    p95    {pct(lats, 0.95):.3f}")
        print(f"    p99    {pct(lats, 0.99):.3f}")
        print(f"    max    {lats[-1]:.3f}")
    print("=" * 56)


# --- main -------------------------------------------------------------------

def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--mode", choices=["kafka", "rest"], default="kafka")
    ap.add_argument("--entity", choices=["person", "legal"], default="person",
                    help="кого шлём: клиентов или мерчантов — у них разные каналы")
    ap.add_argument("--count", type=int, default=100, help="сколько карточек")
    ap.add_argument("--concurrency", type=int, default=8, help="параллельных воркеров")
    ap.add_argument("--rate", type=float, default=0, help="ограничение карточек/сек (0 = без лимита)")
    ap.add_argument("--timeout", type=float, default=60, help="макс. ожидание приземления одной карточки, c")
    ap.add_argument("--interval", type=float, default=0.25, help="период поллинга core, c")
    ap.add_argument("--bootstrap", default=None, help="override Kafka bootstrap (по умолч. из config.json)")
    args = ap.parse_args()

    cfg = load_config()
    bootstrap = args.bootstrap or cfg.get("bootstrap_host", "localhost:19092")

    # Канал определяет тип сущности: свой топик и свой эндпоинт на каждую
    # вертикаль, в теле сообщения типа нет.
    if args.entity == "legal":
        topic = cfg.get("legal_topic", "legal-data")
        rest_path = "/api/v1/legal-data"
    else:
        topic = cfg["topic"]
        rest_path = "/api/v1/client-data"

    if args.mode == "kafka":
        sender = KafkaSender(bootstrap, topic, cfg["token"])
    else:
        sender = RestSender(cfg["gateway"], cfg["token"], rest_path)

    rate_gate = RateLimiter(args.rate) if args.rate > 0 else None

    subject = "мерчантов" if args.entity == "legal" else "карточек"
    channel = f"топик {topic}" if args.mode == "kafka" else rest_path
    print(f"Старт: {args.count} {subject}, режим {args.mode} ({channel}), воркеров {args.concurrency}"
          + (f", лимит {args.rate}/с" if rate_gate else ""))
    results: list[Result] = []
    t_start = time.monotonic()
    try:
        with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
            futures = [pool.submit(run_one, sender, cfg, i, args.timeout, args.interval,
                                   rate_gate, args.entity)
                       for i in range(args.count)]
            done = 0
            for fut in as_completed(futures):
                results.append(fut.result())
                done += 1
                if done % max(1, args.count // 20) == 0 or done == args.count:
                    print(f"  ... {done}/{args.count}", end="\r", flush=True)
    finally:
        sender.close()
    wall = time.monotonic() - t_start
    report(results, wall, f"{args.mode} · {args.entity}")


if __name__ == "__main__":
    main()
