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

import { useEffect, useMemo, useState } from "react";
import { CardPanel, Chip, Icon, Modal, ViewBar } from "@eidos/ui-kit";
import { useCustomer, fio } from "../CustomerContext";

// ДЕМО-раздел: модуль Nexus (entity resolution связей) не реализован —
// связи генерируются детерминированно от id клиента.
const PEOPLE = [
  "Рашидов Санжар Бахтиёрович", "Назарова Азиза Фарходовна", "Юлдашев Тимур Рустамович",
  "Исмаилова Малика Алишеровна", "Абдуллаев Шерзод Камилович", "Тошматова Нигора Улугбековна",
  "Холматов Бекзод Дилшодович", "Эргашева Зарина Жасуровна",
];

function hash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 9973;
  return h;
}
function initials(name: string): string {
  const p = name.split(/\s+/);
  return ((p[0]?.[0] ?? "") + (p[1]?.[0] ?? "")).toUpperCase();
}
function hue(name: string): string {
  return `hsl(${hash(name) % 360},30%,50%)`;
}
function short(name: string): string {
  const p = name.split(" ");
  return p[0] + (p[1] ? ` ${p[1][0]}.` : "");
}

function buildNexus(seedId: string) {
  const base = hash(seedId);
  const at = (k: number) => PEOPLE[(base + k) % PEOPLE.length];
  return {
    real: [
      { rel: "Супруг(а)", name: at(1), src: "ЗАГС · eGov", conf: 99, note: "Свидетельство о браке" },
      { rel: "Совладелец счёта", name: at(3), src: "Банк · договор", conf: 97, note: "Совместный сберегательный счёт" },
      { rel: "Учредитель ЮЛ", name: at(6), src: "ЕГРЮЛ · eGov", conf: 95, note: "Доля 50% в ООО" },
    ],
    implicit: [
      { sig: "Частые переводы P2P", name: at(2), strength: 82, basis: "18 переводов за 90 дней" },
      { sig: "Геолокация рядом", name: at(5), strength: 64, basis: "Совпадение локаций 12×/мес" },
      { sig: "Общий адрес доставки", name: at(4), strength: 71, basis: "2 общих адреса" },
      { sig: "Общее устройство / IP", name: at(7), strength: 57, basis: "Вход с одного устройства" },
    ],
  };
}

function RebuildModal({ onClose }: { onClose: () => void }) {
  const [pct, setPct] = useState(0);
  const [step, setStep] = useState("Инициализация…");
  const [done, setDone] = useState<null | boolean>(null);
  useEffect(() => {
    const steps: Array<[number, string]> = [
      [12, "Сбор идентификаторов из источников…"], [34, "Разрешение сущностей (entity resolution)…"],
      [58, "Расчёт поведенческих сигналов…"], [80, "Оценка достоверности связей…"], [100, "Построение графа…"],
    ];
    let p = 0;
    const t = window.setInterval(() => {
      p = Math.min(100, p + Math.random() * 3 + 1.6);
      setPct(p);
      const s = steps.filter(([n]) => p >= n).pop();
      if (s) setStep(s[1]);
      if (p >= 100) {
        window.clearInterval(t);
        window.setTimeout(() => setDone(Math.random() > 0.18), 440);
      }
    }, 80);
    return () => window.clearInterval(t);
  }, []);
  return (
    <Modal
      title="Пересборка графа"
      subtitle="Модуль Nexus · разрешение сущностей и сигналов · демо"
      onClose={onClose}
      footer={<button className="btn btn-ghost btn-sm" onClick={onClose}>Закрыть</button>}
    >
      {done === null ? (
        <div className="nxp-stage">
          <div className="nxp-step">{step}</div>
          <div className="nxp-bar"><i style={{ width: `${pct}%` }} /></div>
          <div className="nxp-pct"><span>{Math.round(pct)}</span>%</div>
        </div>
      ) : (
        <div className="nxp-stage nxp-result">
          <div className={"nxp-badge" + (done ? "" : " fail")}>{done ? "✓" : "!"}</div>
          <div className="nxp-verdict">{done ? "Граф успешно пересобран" : "Пересборка завершена с ошибкой"}</div>
          <div className="nxp-note">
            {done
              ? "Разрешено сущностей: 7 · конфликтов: 0 · время 1.2 c"
              : "Источник ЕГРЮЛ недоступен (timeout). Граф собран частично — повторите позже."}
          </div>
        </div>
      )}
    </Modal>
  );
}

export function NexusPage() {
  const { clientId, detail } = useCustomer();
  const [rebuild, setRebuild] = useState(false);
  const data = useMemo(() => buildNexus(clientId ?? "seed"), [clientId]);

  const nodes = [
    ...data.real.map((r) => ({ name: r.name, type: "real" as const })),
    ...data.implicit.map((m) => ({ name: m.name, type: "imp" as const })),
  ];
  const cx = 400, cy = 190, rx = 250, ry = 132;
  const placed = nodes.map((n, k) => {
    const a = -Math.PI / 2 + (k / nodes.length) * Math.PI * 2;
    return { ...n, x: cx + Math.cos(a) * rx, y: cy + Math.sin(a) * ry };
  });

  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Связи"
          sub="Модуль Nexus · подтверждённые (документы) и неявные (поведенческие) связи субъекта."
          actions={
            <>
              <Chip tone="info">демо</Chip>
              <button className="btn btn-ghost btn-sm" onClick={() => setRebuild(true)}>Пересобрать граф</button>
            </>
          }
        />
        <div className="card nx-graph-card">
          <div className="nx-bg" aria-hidden="true" />
          <div className="card-h">
            <span className="ic"><Icon name="link" size={16} /></span>
            <span className="ttl">Граф связей субъекта</span>
            <span className="right">{nodes.length} связей · {data.real.length} подтв. · {data.implicit.length} неявн.</span>
          </div>
          <svg className="nx-graph" viewBox="0 0 800 380" aria-hidden="true">
            {placed.map((n, i) => (
              <line key={"e" + i} x1={cx} y1={cy} x2={n.x} y2={n.y} className={"nx-edge " + n.type}
                strokeDasharray={n.type === "imp" ? "4 7" : undefined} />
            ))}
            {placed.map((n, i) => (
              <line key={"f" + i} x1={cx} y1={cy} x2={n.x} y2={n.y} className="nx-flow"
                style={{ animationDelay: `${(-(i * 0.17) % 1.15).toFixed(2)}s` }} />
            ))}
            {placed.map((n, i) => (
              <g className={"nx-node " + n.type} key={"n" + i}>
                <circle cx={n.x} cy={n.y} r={6} />
                <text x={n.x} y={n.y < cy ? n.y - 13 : n.y + 21} textAnchor="middle">{short(n.name)}</text>
              </g>
            ))}
            <g className="nx-center">
              <circle cx={cx} cy={cy} r={11} />
              <text x={cx} y={cy + 32} textAnchor="middle">{detail ? short(fio(detail)) : "субъект"} · субъект</text>
            </g>
          </svg>
          <div className="nx-legend">
            <span><i className="ln" /> Подтверждённые (документы)</span>
            <span><i className="ln dashed" /> Неявные (поведение)</span>
          </div>
        </div>
        <div className="nx-cols">
          <CardPanel icon={<Icon name="checkCircle" size={16} />} title="Подтверждённые связи" right="документально">
            <div className="nx-list">
              {data.real.map((r) => (
                <div className="nx-item" key={r.name + r.rel}>
                  <div className="nx-av" style={{ background: hue(r.name) }}>{initials(r.name)}</div>
                  <div className="nx-meta">
                    <div className="nx-nm">{r.name}</div>
                    <div className="nx-sub">{r.rel} · {r.note}</div>
                    <div className="nx-tags"><span className="nx-src">{r.src}</span></div>
                  </div>
                  <div className="nx-conf real">{r.conf}%</div>
                </div>
              ))}
            </div>
          </CardPanel>
          <CardPanel icon={<Icon name="chart" size={16} />} title="Неявные связи" right="поведенческие сигналы">
            <div className="nx-list">
              {data.implicit.map((m) => (
                <div className="nx-item" key={m.name + m.sig}>
                  <div className="nx-av" style={{ background: hue(m.name) }}>{initials(m.name)}</div>
                  <div className="nx-meta">
                    <div className="nx-nm">{m.name}</div>
                    <div className="nx-sub">{m.sig} · {m.basis}</div>
                    <div className="nx-bar"><i style={{ width: `${m.strength}%` }} /></div>
                  </div>
                  <div className="nx-conf imp">{m.strength}%</div>
                </div>
              ))}
            </div>
          </CardPanel>
        </div>
      </div>
      {rebuild && <RebuildModal onClose={() => setRebuild(false)} />}
    </section>
  );
}
