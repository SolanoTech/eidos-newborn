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

import { useMemo, useState } from "react";
import { Chip, ViewBar } from "@eidos/ui-kit";
import { useNavigate } from "react-router-dom";

// ДЕМО-раздел: журнал омниканальных коммуникаций (модуля отправки нет).
interface Comm {
  channel: "push" | "sms" | "email" | "call";
  chLabel: string;
  date: string;
  time: string;
  type: "offer" | "service";
  dot: string;
  status: "open" | "deliv" | "click";
  statusText: string;
  title: string;
  body: string;
}

const today = new Date().toISOString().slice(0, 10);
const COMMS: Comm[] = [
  { channel: "push", chLabel: "Push", date: today, time: "сегодня · 19:42", type: "offer", dot: "gold", status: "open", statusText: "● Открыто · переход в приложение", title: "Премиальный депозит 24% — специально для вас", body: "Оркестрация: Temptation → NBO #1. Сегмент Champions." },
  { channel: "sms", chLabel: "SMS", date: "2026-06-25", time: "25.06 · 12:10", type: "service", dot: "ok", status: "deliv", statusText: "✓ Доставлено", title: "Код подтверждения операции", body: "Транзакционное · OTP для входа в приложение." },
  { channel: "email", chLabel: "E-mail", date: "2026-06-22", time: "22.06 · 09:00", type: "service", dot: "", status: "deliv", statusText: "✓ Доставлено", title: "Ежемесячный отчёт по картам", body: "Сервисная рассылка · согласие на e-mail отозвано после отправки." },
  { channel: "push", chLabel: "Push", date: "2026-06-18", time: "18.06 · 18:30", type: "offer", dot: "gold", status: "click", statusText: "↗ Клик · оффер принят", title: "Cashback «Лето» 5% активирован", body: "Подтверждение принятия оффера." },
  { channel: "call", chLabel: "Звонок", date: "2026-06-10", time: "10.06 · 15:22", type: "service", dot: "ok", status: "deliv", statusText: "✓ Состоялся", title: "Поздравление с апгрейдом статуса", body: "Контакт-центр · 2:14 · удовлетворённость 5/5." },
];

const ST: Record<string, string> = { open: "st-open", deliv: "st-deliv", click: "st-click" };
const ALL_CHANNELS: Array<[Comm["channel"], string]> = [["push", "Push"], ["sms", "SMS"], ["email", "E-mail"], ["call", "Звонок"]];

export function CommsPage() {
  const navigate = useNavigate();
  const [filterOpen, setFilterOpen] = useState(false);
  const [channels, setChannels] = useState<Set<string>>(new Set(ALL_CHANNELS.map(([c]) => c)));
  const [type, setType] = useState<"all" | "offer" | "service">("all");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  const list = useMemo(
    () => COMMS.filter((m) =>
      channels.has(m.channel)
      && (type === "all" || m.type === type)
      && (!from || m.date >= from) && (!to || m.date <= to)),
    [channels, type, from, to]
  );

  const toggleChannel = (ch: string) => {
    setChannels((prev) => {
      const next = new Set(prev);
      if (next.has(ch)) next.delete(ch);
      else next.add(ch);
      return next;
    });
  };

  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Коммуникации"
          sub="Журнал омниканальных взаимодействий с клиентом."
          actions={
            <>
              <Chip tone="info">демо</Chip>
              <button className={"btn btn-ghost btn-sm" + (filterOpen ? " active" : "")} onClick={() => setFilterOpen(!filterOpen)}>
                Фильтр
              </button>
              <button className="btn btn-primary btn-sm" onClick={() => navigate("/chat")}>Новое сообщение</button>
            </>
          }
        />

        <div className={"filter-panel" + (filterOpen ? " show" : "")}>
          <div className="fp-group">
            <div className="fp-label">Канал коммуникации</div>
            <div className="fp-chips">
              {ALL_CHANNELS.map(([ch, label]) => (
                <span key={ch} className={"fchip" + (channels.has(ch) ? " on" : "")} onClick={() => toggleChannel(ch)}>{label}</span>
              ))}
            </div>
          </div>
          <div className="fp-group">
            <div className="fp-label">Тип коммуникации</div>
            <div className="fp-chips">
              {([["all", "Все"], ["offer", "Оффер"], ["service", "Сервисная"]] as const).map(([t, label]) => (
                <span key={t} className={"fchip" + (type === t ? " on" : "")} onClick={() => setType(t)}>{label}</span>
              ))}
            </div>
          </div>
          <div className="fp-group">
            <div className="fp-label">Период отправки</div>
            <div className="fp-dates">
              <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} aria-label="Отправлено с" />
              <span className="fp-dash">—</span>
              <input type="date" value={to} onChange={(e) => setTo(e.target.value)} aria-label="Отправлено по" />
            </div>
          </div>
          <div className="fp-actions">
            <span className="fp-count">{list.length} из {COMMS.length}</span>
            <button className="btn btn-ghost btn-sm" onClick={() => { setChannels(new Set(ALL_CHANNELS.map(([c]) => c))); setType("all"); setFrom(""); setTo(""); }}>
              Сбросить
            </button>
          </div>
        </div>

        <div className="card">
          <div className="timeline">
            {list.length === 0 ? (
              <div className="sv-empty">Нет коммуникаций по заданным фильтрам.</div>
            ) : (
              list.map((m, i) => (
                <div className="tl-item" key={i}>
                  <div className={"tl-dot " + m.dot} />
                  <div className="tl-head">
                    <span className={"tl-ch " + m.channel}>{m.chLabel}</span>
                    <span className="tl-time">{m.time}</span>
                  </div>
                  <div className="tl-title">{m.title}</div>
                  <div className="tl-body">{m.body}</div>
                  <span className={"tl-status " + (ST[m.status] ?? "st-deliv")}>{m.statusText}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
