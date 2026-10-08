/*
 * Copyright 2026 LLC SOLANOTECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Chip, Icon, St, ViewBar, toast } from "@eidos/ui-kit";

// ДЕМО-раздел: направления доставки данных — витрина макета.
const ROWS = [
  { nm: "Webhook · CRM «Единое окно»", sub: "POST https://crm.humo.uz/hooks/eidos · события profile_update, consent_change", ok: true, stTxt: "активен", extra: "99.98% успешных за 7 дн" },
  { nm: "Kafka · топик gr-events", sub: "Исходящий поток изменений Golden Records · avro, compaction", ok: true, stTxt: "активен", extra: "2.1 тыс msg/мин" },
  { nm: "S3 · ежесуточная выгрузка витрин", sub: "s3://dwh-humo/eidos/daily · parquet · 03:00 UTC+5", ok: true, stTxt: "активен", extra: "последняя: сегодня 03:04" },
  { nm: "BI · HUMO Intelligence", sub: "Прямое подключение к витринам качества и сегментов", ok: true, stTxt: "активен", extra: "read-only" },
  { nm: "Webhook · Anti-fraud", sub: "POST https://af.humo.uz/eidos · событие identity_merge", ok: false, stTxt: "ошибки", extra: "3 ретрая за час · 502" },
];

export function IntegrationsPage() {
  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Интеграции и выгрузки"
          sub="Направления доставки данных: webhooks, потоковые топики, файловые выгрузки и BI."
          actions={
            <>
              <Chip tone="info">демо</Chip>
              <button className="btn btn-primary btn-sm" onClick={() => toast("Демо: каталог коннекторов")}>Добавить направление</button>
            </>
          }
        />
        <div className="card">
          {ROWS.map((x) => (
            <div className="lrow" key={x.nm}>
              <div className="lic"><Icon name="link" size={17} /></div>
              <div className="lmain">
                <div className="lnm">{x.nm}</div>
                <div className="lsub">{x.sub}</div>
              </div>
              <div className="lend">
                <span className="chip" style={{ border: "none", padding: 0 }}>{x.extra}</span>
                <St tone={x.ok ? "on" : "warn"}>{x.stTxt}</St>
                <button className="btn btn-ghost btn-xs" onClick={() => toast("Демо: настройки направления")}>Настроить</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
