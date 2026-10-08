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

import { useState, type ReactElement } from "react";
import { Chip, Modal, ViewBar, toast } from "@eidos/ui-kit";
import { addOffer, listOffers, setStatus, type Offer } from "../offersStore";
import { useCustomer, fio } from "../CustomerContext";

const PRODUCTS = [
  "Премиальный депозит", "Накопительный счёт", "Premium HUMO Metal", "Дебетовая карта HUMO",
  "Кредитная линия", "Авто-рассрочка 0-0-12", "Cashback-программа", "P2P / переводы",
  "Страховой продукт", "Инвестиционный продукт",
];
const CHANNELS = ["Push", "SMS", "E-mail", "App-баннер"];

const ICONS: Record<Offer["icon"], ReactElement> = {
  star: <path d="M12 2l3 7h7l-5.5 4 2 7L12 17l-6.5 3 2-7L2 9h7z" />,
  card: <><rect x="2" y="5" width="20" height="14" rx="2" /><path d="M2 10h20" /></>,
  split: <><path d="M3 10h11M3 6h11M3 14h7" /><path d="M17 8l4 4-4 4" /></>,
  check: <path d="M20 6L9 17l-5-5" />,
  gift: <><path d="M20.6 13.4l-7.2 7.2a2 2 0 0 1-2.8 0l-6.2-6.2A2 2 0 0 1 3.8 13V4.6a1 1 0 0 1 1-1H13a2 2 0 0 1 1.4.6l6.2 6.2a2 2 0 0 1 0 2.8z" /><circle cx="8" cy="8" r="1.4" /></>,
};

function fmtDate(s: string): string {
  const [y, m, d] = s.split("-");
  return `${d}.${m}.${y}`;
}

function CreateModal({ client, onClose, onCreated }: { client: string; onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState("");
  const [product, setProduct] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [channels, setChannels] = useState<string[]>(["Push"]);
  const [err, setErr] = useState("");

  const toggleCh = (ch: string) =>
    setChannels((prev) => prev.includes(ch) ? prev.filter((c) => c !== ch) : [...prev, ch]);

  const create = () => {
    if (!name.trim()) { setErr("Укажите наименование предложения."); return; }
    if (!product) { setErr("Выберите продукт."); return; }
    if (!from || !to) { setErr("Укажите срок действия (с и по)."); return; }
    if (to < from) { setErr("Дата окончания раньше даты начала."); return; }
    addOffer({
      name: name.trim(), type: product, icon: "gift",
      desc: `Продукт: ${product}. Срок действия: ${fmtDate(from)} — ${fmtDate(to)}. Создан вручную оператором.`,
      prop: Math.floor(45 + Math.random() * 45),
      channels: channels.length ? channels : ["Push"],
      status: "draft",
    });
    toast(`Оффер «${name.trim()}» создан (черновик)`);
    onCreated();
  };

  return (
    <Modal
      title="Новый персональный оффер"
      subtitle={`Подсистема Temptation · клиент ${client} · демо`}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose}>Отмена</button>
          <button className="btn btn-primary" onClick={create}>Создать оффер</button>
        </>
      }
    >
      <div className="field">
        <label>Наименование персонального предложения <span className="req">*</span></label>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Напр. Премиальный депозит 24%" autoFocus />
      </div>
      <div className="field">
        <label>Выбор продукта <span className="req">*</span></label>
        <select value={product} onChange={(e) => setProduct(e.target.value)}>
          <option value="" disabled>— выберите продукт —</option>
          {PRODUCTS.map((p) => <option key={p}>{p}</option>)}
        </select>
      </div>
      <div className="field">
        <label>Срок действия оффера <span className="req">*</span></label>
        <div className="frow2">
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} aria-label="Действует с" />
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} aria-label="Действует по" />
        </div>
      </div>
      <div className="field">
        <label>Каналы доставки</label>
        <div className="chl-pick">
          {CHANNELS.map((ch) => (
            <span key={ch} className={"pick" + (channels.includes(ch) ? " on" : "")} onClick={() => toggleCh(ch)}>{ch}</span>
          ))}
        </div>
      </div>
      {err && <div className="errmsg">{err}</div>}
    </Modal>
  );
}

export function OffersPage() {
  const { detail } = useCustomer();
  const [, force] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const offers = listOffers();
  const refresh = () => force((n) => n + 1);

  const ribbon = (o: Offer) => {
    if (o.status === "accepted") return <span className="ribbon rib-accept">Принят ✓</span>;
    if (o.status === "draft") return <span className="ribbon rib-draft">Черновик</span>;
    if (o.status === "revoked") return <span className="ribbon rib-revoked">Отозван</span>;
    if (o.status === "active") return <span className="ribbon rib-accept">Активен</span>;
    return <span className="ribbon">{o.rank ?? "NBO"}</span>;
  };
  const pcolor = (p: number) => (p >= 70 ? "var(--ok)" : p >= 50 ? "var(--ink)" : "var(--warn)");
  const act = (id: number, status: Offer["status"], msg: string) => {
    const o = setStatus(id, status);
    if (o) toast(`Оффер «${o.name}» ${msg}`);
    refresh();
  };

  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Офферы клиента"
          sub="Персональные предложения на основе propensity-моделей подсистемы Temptation."
          actions={
            <>
              <Chip tone="info">демо</Chip>
              <button className="btn btn-primary btn-sm" onClick={() => setCreateOpen(true)}>＋ Создать оффер</button>
            </>
          }
        />
        <div className="offers">
          {offers.map((o) => (
            <div className={"offer" + (o.status === "revoked" ? " revoked" : "")} key={o.id}>
              {ribbon(o)}
              <div className="oh">
                <div className="oc">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">{ICONS[o.icon]}</svg>
                </div>
                <div>
                  <div className="on">{o.name}</div>
                  <div className="ot">{o.type}</div>
                </div>
              </div>
              <div className="od">{o.desc}</div>
              <div className="prop">
                <span className="pl">{o.status === "accepted" ? "Конверсия" : "Propensity"}</span>
                <span className="pv" style={{ color: o.status === "revoked" ? "var(--ink-dim)" : pcolor(o.prop) }}>
                  {o.status === "accepted" ? "Принят" : o.status === "revoked" ? "—" : `${o.prop}%`}
                </span>
                <div className="bar" style={{ flex: 1 }}><i style={{ width: `${o.status === "revoked" ? 0 : o.prop}%` }} /></div>
              </div>
              <div className="ofoot">
                <div className="chl">{o.channels.map((c) => <span className="mini" key={c}>{c}</span>)}</div>
                <div className="acts">
                  {o.status === "revoked"
                    ? <button className="btn btn-ghost btn-sm" onClick={() => act(o.id, "draft", "восстановлен")}>Восстановить</button>
                    : <button className="btn btn-danger btn-sm" onClick={() => act(o.id, "revoked", "отозван")}>Отозвать оффер</button>}
                  {o.status === "accepted" && <button className="btn btn-ghost btn-sm" onClick={() => toast("Открыта аналитика")}>Аналитика</button>}
                  {o.status === "draft" && <button className="btn btn-ghost btn-sm" onClick={() => toast("Оффер открыт в конструкторе")}>Доработать</button>}
                  {(o.status === "nbo") && <button className="btn btn-primary btn-sm" onClick={() => act(o.id, "active", "активирован")}>Активировать</button>}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
      {createOpen && (
        <CreateModal
          client={detail ? fio(detail) : "клиент"}
          onClose={() => setCreateOpen(false)}
          onCreated={() => { setCreateOpen(false); refresh(); }}
        />
      )}
    </section>
  );
}
