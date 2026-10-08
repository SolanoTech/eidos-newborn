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

import { useEffect, useState } from "react";
import { ViewBar, toast } from "@eidos/ui-kit";
import { listAudit, type AuditEvent } from "../api/audit";

function fmtTime(iso: string): string {
  const d = new Date(iso);
  const today = new Date();
  const sameDay = d.toDateString() === today.toDateString();
  const hm = d.toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" });
  if (sameDay) return `сегодня · ${hm}`;
  return `${d.toLocaleDateString("ru-RU")} · ${hm}`;
}

export function AuditPage() {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listAudit(100).then(setEvents).catch(() => setError("Не удалось загрузить аудит-лог."));
  }, []);

  const exportCsv = () => {
    const rows = [["time", "actor", "action", "details"]]
      .concat(events.map((e) => [e.createdAt, e.actor, e.action, e.details ?? ""]));
    const csv = rows.map((r) => r.map((c) => `"${String(c).split('"').join('""')}"`).join(",")).join("\n");
    const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "eidos-audit.csv";
    a.click();
    URL.revokeObjectURL(url);
    toast("Экспорт аудита сформирован");
  };

  return (
    <section className="view-panel">
      <div className="wrap" style={{ maxWidth: 840 }}>
        <ViewBar
          title="Аудит-лог"
          sub="Все действия администраторов консоли. Неизменяемый журнал, экспорт в SIEM."
          actions={<button className="btn btn-ghost btn-sm" onClick={exportCsv}>Экспорт CSV</button>}
        />
        {error && <div className="errmsg" style={{ marginBottom: 16 }}>{error}</div>}
        <div className="card">
          {events.length === 0 ? (
            <div className="sv-empty">Журнал пока пуст.</div>
          ) : (
            <div className="timeline">
              {events.map((e) => (
                <div className="tl-item" key={e.id}>
                  <div className={"tl-dot " + e.category} />
                  <div className="tl-head">
                    <span className="tl-who">{e.actor}</span>
                    <span className="tl-time">{fmtTime(e.createdAt)}</span>
                  </div>
                  <div className="tl-title">{e.action}</div>
                  {e.details && <div className="tl-body">{e.details}</div>}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
