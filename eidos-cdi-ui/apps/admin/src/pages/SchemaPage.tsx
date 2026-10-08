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

import { useState } from "react";
import { Chip, Tile, Toggle, ViewBar, toast } from "@eidos/ui-kit";

// ДЕМО-раздел: событийной платформы в системе пока нет, данные — витрина макета.
const INITIAL = [
  { ev: "card_transaction", attrs: 22, ret: "24 мес", vol: "1.8 млн", last: "только что", on: true },
  { ev: "session_start", attrs: 9, ret: "12 мес", vol: "340 тыс", last: "только что", on: true },
  { ev: "payment_p2p", attrs: 14, ret: "24 мес", vol: "120 тыс", last: "1 мин назад", on: true },
  { ev: "profile_update", attrs: 31, ret: "36 мес", vol: "18 тыс", last: "4 мин назад", on: true },
  { ev: "consent_change", attrs: 7, ret: "бессрочно", vol: "2.1 тыс", last: "12 мин назад", on: true },
  { ev: "offer_reaction", attrs: 11, ret: "12 мес", vol: "46 тыс", last: "2 мин назад", on: true },
  { ev: "geo_ping", attrs: 5, ret: "3 мес", vol: "—", last: "выключено", on: false },
];

export function SchemaPage() {
  const [rows, setRows] = useState(INITIAL);
  const toggle = (i: number) => {
    setRows((prev) => prev.map((r, idx) => (idx === i ? { ...r, on: !r.on } : r)));
    toast(`Демо: трекинг «${rows[i].ev}» ${rows[i].on ? "выключен" : "включён"}`);
  };
  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Схема событий"
          sub="Каталог событий и атрибутов клиента. Управление трекингом, типами и retention."
          actions={
            <>
              <Chip tone="info">демо</Chip>
              <button className="btn btn-primary btn-sm" onClick={() => toast("Демо: мастер нового события")}>Новое событие</button>
            </>
          }
        />
        <div className="tiles" style={{ gridTemplateColumns: "repeat(3,1fr)", marginBottom: 16 }}>
          <Tile label="Типов событий" value={18} note="16 отслеживаются" />
          <Tile label="Атрибутов клиента" value={124} note="42 системных · 82 источника" />
          <Tile label="Событий за сутки" value="2.4" unit="млн" note={<><span className="up">+6%</span> к прошлой неделе</>} />
        </div>
        <div className="card">
          <table className="tbl">
            <thead>
              <tr><th>Событие</th><th>Атрибутов</th><th>Retention</th><th>Объём / сутки</th><th>Последнее</th><th>Трекинг</th></tr>
            </thead>
            <tbody>
              {rows.map((e, i) => (
                <tr key={e.ev}>
                  <td><code>{e.ev}</code></td>
                  <td className="num">{e.attrs}</td>
                  <td className="muted">{e.ret}</td>
                  <td className="num">{e.vol}</td>
                  <td className="muted">{e.last}</td>
                  <td><Toggle on={e.on} onChange={() => toggle(i)} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
