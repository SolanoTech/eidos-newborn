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
import { Chip, St, Tile, ViewBar, toast } from "@eidos/ui-kit";

// ДЕМО-раздел: запросы субъектов данных (DSR) — витрина макета.
interface Dsr { id: string; subj: string; type: string; got: string; st: "in" | "wait" | "done"; }
const INITIAL: Dsr[] = [
  { id: "DSR-2041", subj: "Каримов А.Б.", type: "Выгрузка данных", got: "30.06.2026", st: "in" },
  { id: "DSR-2040", subj: "Юсупова Д.Ш.", type: "Право на забвение", got: "29.06.2026", st: "wait" },
  { id: "DSR-2038", subj: "Ортиков С.А.", type: "Ограничение обработки", got: "27.06.2026", st: "in" },
  { id: "DSR-2035", subj: "Назарова А.Ф.", type: "Выгрузка данных", got: "24.06.2026", st: "done" },
];

export function PrivacyPage() {
  const [rows, setRows] = useState(INITIAL);
  const done = (id: string) => {
    setRows((prev) => prev.map((r) => (r.id === id ? { ...r, st: "done" as const } : r)));
    toast(`Демо: запрос ${id} выполнен · аудит записан`);
  };
  const open = rows.filter((r) => r.st !== "done").length;
  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Приватность · GDPR"
          sub="Запросы субъектов данных: выгрузка, удаление (right to be forgotten), ограничение обработки."
          actions={<Chip tone="info">демо</Chip>}
        />
        <div className="tiles" style={{ gridTemplateColumns: "repeat(3,1fr)", marginBottom: 16 }}>
          <Tile label="Открытых запросов" value={open} note="SLA 30 дней" />
          <Tile label="Выполнено за месяц" value={14} note="среднее время 2.1 дня" />
          <Tile label="Анонимизировано" value={9} note="записей навсегда" />
        </div>
        <div className="card">
          <table className="tbl">
            <thead>
              <tr><th>Запрос</th><th>Субъект</th><th>Тип</th><th>Получен</th><th>Статус</th><th></th></tr>
            </thead>
            <tbody>
              {rows.map((g) => (
                <tr key={g.id}>
                  <td><code>{g.id}</code></td>
                  <td><b>{g.subj}</b></td>
                  <td>{g.type}</td>
                  <td className="muted num">{g.got}</td>
                  <td>
                    {g.st === "in" && <St tone="warn">в работе</St>}
                    {g.st === "wait" && <St tone="bad">ожидает</St>}
                    {g.st === "done" && <St tone="on">выполнен</St>}
                  </td>
                  <td className="acts">
                    {g.st !== "done"
                      ? <button className="btn btn-primary btn-xs" onClick={() => done(g.id)}>Выполнить</button>
                      : <button className="btn btn-ghost btn-xs" onClick={() => toast(`Демо: акт выполнения ${g.id}`)}>Акт</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
