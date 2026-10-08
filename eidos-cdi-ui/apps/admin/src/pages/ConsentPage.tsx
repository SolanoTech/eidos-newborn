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
import { CardPanel, Chip, Icon, Toggle, ViewBar, toast } from "@eidos/ui-kit";

// ДЕМО-раздел: категории согласий и политика контактов — витрина макета.
const INITIAL = [
  { nm: "Маркетинг · Push", sub: "Правовое основание: согласие · TTL 12 мес · 84% базы", on: true },
  { nm: "Маркетинг · SMS", sub: "Правовое основание: согласие · TTL 12 мес · 61% базы", on: true },
  { nm: "Маркетинг · E-mail", sub: "Правовое основание: согласие · TTL 24 мес · 47% базы", on: true },
  { nm: "Профилирование и NBO", sub: "Правовое основание: законный интерес · оценка DPIA пройдена", on: true },
  { nm: "Передача данных партнёрам", sub: "Правовое основание: согласие · TTL 6 мес · 12% базы", on: false },
];

export function ConsentPage() {
  const [rows, setRows] = useState(INITIAL);
  const [globalOptOut, setGlobalOptOut] = useState(true);
  const toggle = (i: number) => {
    setRows((prev) => prev.map((r, idx) => (idx === i ? { ...r, on: !r.on } : r)));
    toast(`Демо: категория «${rows[i].nm}» ${rows[i].on ? "выключена" : "включена"}`);
  };
  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Согласия и политика контактов"
          sub="Категории согласий, правовые основания и лимиты коммуникационного давления на клиента."
          actions={
            <>
              <Chip tone="info">демо</Chip>
              <button className="btn btn-primary btn-sm" onClick={() => toast("Демо: мастер новой категории согласия")}>Новая категория</button>
            </>
          }
        />
        <div className="grid g-2">
          <CardPanel icon={<Icon name="consent" size={16} />} title="Категории согласий">
            {rows.map((c, i) => (
              <div className="lrow" key={c.nm}>
                <div className="lic"><Icon name="consent" size={17} /></div>
                <div className="lmain">
                  <div className="lnm">{c.nm}</div>
                  <div className="lsub">{c.sub}</div>
                </div>
                <div className="lend"><Toggle on={c.on} onChange={() => toggle(i)} /></div>
              </div>
            ))}
          </CardPanel>
          <CardPanel icon={<Icon name="clock" size={16} />} title="Политика контактов (frequency capping)">
            <div className="modal-b" style={{ padding: 0 }}>
              <div className="frow2">
                <div className="field"><label>Максимум push / сутки</label><input defaultValue="2" inputMode="numeric" /></div>
                <div className="field"><label>Максимум SMS / неделя</label><input defaultValue="3" inputMode="numeric" /></div>
              </div>
              <div className="frow2">
                <div className="field"><label>Тихие часы, с</label><input defaultValue="21:00" /></div>
                <div className="field"><label>Тихие часы, до</label><input defaultValue="09:00" /></div>
              </div>
              <div className="field">
                <label>Приоритет при конфликте кампаний</label>
                <select defaultValue="Сервисные > NBO > массовые">
                  <option>Сервисные &gt; NBO &gt; массовые</option>
                  <option>По CLV клиента</option>
                  <option>По дате постановки</option>
                </select>
              </div>
              <div className="fcheck">
                <Toggle on={globalOptOut} onChange={setGlobalOptOut} />
                <span>Учитывать глобальный отказ от маркетинга</span>
              </div>
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 16 }}>
              <button className="btn btn-primary btn-sm" onClick={() => toast("Демо: политика контактов сохранена")}>
                Сохранить политику
              </button>
            </div>
          </CardPanel>
        </div>
      </div>
    </section>
  );
}
