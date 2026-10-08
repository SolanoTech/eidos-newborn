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
import { CardPanel, Icon, SegItem, St, Tile, ViewBar, toast } from "@eidos/ui-kit";
import { getDashboard, type Dashboard } from "../api/dashboard";

const fmt = new Intl.NumberFormat("ru-RU");

export function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboard().then(setData).catch(() => setError("Не удалось загрузить дашборд — проверьте доступность сервисов."));
  }, []);

  const m = data?.metrics;
  const sources = data?.sources ?? [];
  const activity = new Map((data?.activity ?? []).map((a) => [a.sourceCode, a]));
  const activeCount = sources.filter((s) => s.enabled).length;
  const accepted24h = (data?.activity ?? []).reduce((sum, a) => sum + a.accepted, 0);
  const autoMerge = m && m.grTotal > 0 ? Math.round((1 - m.tentativeTotal / m.grTotal) * 1000) / 10 : null;

  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Обзор платформы"
          sub="Состояние Single Customer View: источники, поток данных, конфликты, качество."
          actions={
            <button className="btn btn-ghost btn-sm" onClick={() => toast("Отчёт за сутки сформирован")}>
              Отчёт за сутки
            </button>
          }
        />
        {error && <div className="errmsg" style={{ marginBottom: 16 }}>{error}</div>}
        <div className="tiles" style={{ marginBottom: 16 }}>
          <Tile label="Golden Records" value={m ? fmt.format(m.grTotal) : "—"} note={<><span className="up">+{fmt.format(accepted24h)}</span> за сутки</>} />
          <Tile label="Источников" value={sources.length || "—"} note={`${activeCount} активны · ${sources.length - activeCount} отключено`} />
          <Tile label="Конфликтов" value={m ? m.tentativeTotal : "—"} note={<><span className="down">{m ? m.tentativeGreyZone : 0} grey-zone</span> ждут решения</>} />
          <Tile label="Auto-merge rate" value={autoMerge ?? "—"} unit="%" note="детерминированный резолв" />
        </div>
        <div className="grid g-2">
          <CardPanel icon={<Icon name="database" size={16} />} title="Активность источников" right="за 24 часа">
            <table className="tbl">
              <thead>
                <tr><th>Источник</th><th>Принято</th><th>Отклонено</th><th>Статус</th></tr>
              </thead>
              <tbody>
                {sources.map((s) => {
                  const a = activity.get(s.code);
                  return (
                    <tr key={s.id}>
                      <td><code>{s.code}</code></td>
                      <td className="num">{fmt.format(a?.accepted ?? 0)}</td>
                      <td className="num muted">{fmt.format(a?.rejected ?? 0)}</td>
                      <td>{s.enabled ? <St tone="on">активен</St> : <St tone="off">выключен</St>}</td>
                    </tr>
                  );
                })}
                {sources.length === 0 && (
                  <tr><td colSpan={4} className="muted">Источники не зарегистрированы.</td></tr>
                )}
              </tbody>
            </table>
          </CardPanel>
          <CardPanel icon={<Icon name="shield" size={16} />} title="Качество Golden Records" right="по текущим данным">
            <SegItem label="Валидность телефона (^998\d{9}$)" pct={m?.phoneValidPct ?? 0} tone="g" />
            <SegItem label="Валидность ПИНФЛ (14 цифр)" pct={m?.pinflValidPct ?? 0} tone="g" />
            <SegItem label="Полнота отчества" pct={m?.middleNamePct ?? 0} />
            <SegItem label="Свежесть (обновлено < 30 дн.)" pct={m?.fresh30dPct ?? 0} />
          </CardPanel>
        </div>
      </div>
    </section>
  );
}
