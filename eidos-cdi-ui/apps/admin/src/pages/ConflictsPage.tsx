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

import { useCallback, useEffect, useState } from "react";
import { AxiosError } from "axios";
import { Avatar, Chip, Icon, Modal, Tile, ViewBar, toast } from "@eidos/ui-kit";
import {
  conflictCounts, listConflicts, resolveConflict,
  type ConflictCounts, type ConflictItem, type ResolveAction,
} from "../api/conflicts";
import {
  legalConflictCounts, listLegalConflicts, resolveLegalConflict,
} from "../api/legal";

/** Какая вертикаль показана: у физлиц и юрлиц свои очереди и свои эндпоинты. */
type Vertical = "person" | "legal";

function axiosDetail(err: unknown): string | undefined {
  return err instanceof AxiosError
    ? (err.response?.data as { detail?: string } | undefined)?.detail
    : undefined;
}

function age(createdAt: string | null): string {
  if (!createdAt) return "—";
  const ms = Date.now() - new Date(createdAt).getTime();
  const h = Math.floor(ms / 3_600_000);
  if (h < 1) return "< 1 ч";
  if (h < 24) return `${h} ч`;
  return `${Math.floor(h / 24)} дн`;
}

function ResolveModal({ item, resolve, onClose, onResolved }: {
  item: ConflictItem;
  resolve: (id: number, action: ResolveAction) => Promise<void>;
  onClose: () => void;
  onResolved: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const grey = item.reason === "GREY_ZONE_CONFLICT";

  const act = async (action: ResolveAction, label: string) => {
    setBusy(true);
    setError(null);
    try {
      await resolve(item.id, action);
      toast(`Конфликт разрешён · ${label}`);
      onResolved();
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось разрешить конфликт.");
      setBusy(false);
    }
  };

  const snapshotRows = Object.entries(item.snapshot ?? {}).slice(0, 12);

  return (
    <Modal
      title="Разрешение конфликта"
      subtitle={`${item.personName ?? "Без имени"} · ${item.reason === "GREY_ZONE_CONFLICT" ? "grey-zone: равное доверие" : "неизвестный источник"}`}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost btn-sm" onClick={onClose} disabled={busy}>Отложить</button>
          <button className="btn btn-danger btn-sm" onClick={() => act("REJECT", "запись отклонена")} disabled={busy}>
            Отклонить запись
          </button>
        </>
      }
    >
      {grey ? (
        <>
          {item.conflicts.map((c) => (
            <div key={c.fieldName}>
              <div className="cmp-field">{c.fieldName}</div>
              <div className="cmp">
                <div className="cmp-col win">
                  <div className="cmp-src">
                    {c.currentSource ?? "текущее"}
                    {c.currentTrust != null && <Chip tone="gold">trust {c.currentTrust}</Chip>}
                  </div>
                  <div className="cmp-val">{c.currentValue ?? "—"}</div>
                  <div className="cmp-note">текущее значение Golden Record</div>
                </div>
                <div className="cmp-col">
                  <div className="cmp-src">
                    {item.sourceName ?? "входящее"}
                    {item.sourceTrust != null
                      ? <Chip tone="gold">trust {item.sourceTrust}</Chip>
                      : <Chip tone="bad">не в реестре</Chip>}
                  </div>
                  <div className="cmp-val">{c.incomingValue ?? "—"}</div>
                  <div className="cmp-note">предложено источником {item.sourceName}</div>
                </div>
              </div>
            </div>
          ))}
          <div className="cmp" style={{ marginTop: 4 }}>
            <button className="btn btn-primary btn-sm" onClick={() => act("KEEP_CURRENT", "сохранено текущее значение")} disabled={busy}>
              Оставить текущие
            </button>
            <button className="btn btn-ghost btn-sm" onClick={() => act("ACCEPT_INCOMING", "принято входящее значение")} disabled={busy}>
              Принять входящие
            </button>
          </div>
        </>
      ) : (
        <>
          <div className="cmp-note">
            Источник «{item.sourceName}» отсутствует в реестре. Зарегистрируйте источник и переотправьте данные —
            либо отклоните запись.
          </div>
          <div className="attrs">
            {snapshotRows.map(([k, v]) => (
              <div className="attr" key={k}>
                <span className="k mono">{k}</span>
                <span className="v mono">{v}</span>
              </div>
            ))}
          </div>
        </>
      )}
      {error && <div className="errmsg">{error}</div>}
    </Modal>
  );
}

export function ConflictsPage() {
  const [vertical, setVertical] = useState<Vertical>("person");
  const [items, setItems] = useState<ConflictItem[]>([]);
  const [counts, setCounts] = useState<ConflictCounts | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<ConflictItem | null>(null);

  const load = useCallback(async (v: Vertical) => {
    setError(null);
    try {
      if (v === "person") {
        const [page, c] = await Promise.all([listConflicts(1), conflictCounts()]);
        setItems(page.content);
        setCounts(c);
        // Бейдж в навигации считает только физлиц — это основной поток.
        window.dispatchEvent(new CustomEvent("eidos:conflicts", { detail: c.total }));
      } else {
        const [page, c] = await Promise.all([listLegalConflicts(1), legalConflictCounts()]);
        // Приводим к общей форме: различаются только заголовок и id записи.
        setItems(page.content.map((it) => ({
          id: it.id,
          reason: it.reason,
          grClientId: it.grLegalEntityId,
          sourceName: it.sourceName,
          sourceTrust: it.sourceTrust,
          personName: it.merchantName ?? (it.inn ? `ИНН ${it.inn}` : null),
          createdAt: it.createdAt,
          conflicts: it.conflicts,
          snapshot: it.snapshot,
        })));
        setCounts(c);
      }
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось загрузить очередь конфликтов.");
      setItems([]);
      setCounts(null);
    }
  }, []);

  useEffect(() => { void load(vertical); }, [load, vertical]);

  const resolve = vertical === "person" ? resolveConflict : resolveLegalConflict;

  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Очередь конфликтов"
          sub="Спорные записи: равное доверие источников при расхождении значения или неизвестный источник."
        />

        <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
          <button
            className={`btn btn-sm ${vertical === "person" ? "btn-primary" : "btn-ghost"}`}
            onClick={() => setVertical("person")}
          >
            Физлица
          </button>
          <button
            className={`btn btn-sm ${vertical === "legal" ? "btn-primary" : "btn-ghost"}`}
            onClick={() => setVertical("legal")}
          >
            Юрлица
          </button>
        </div>

        {error && <div className="errmsg" style={{ marginBottom: 16 }}>{error}</div>}
        <div className="tiles" style={{ gridTemplateColumns: "repeat(3,1fr)", marginBottom: 16 }}>
          <Tile label="В очереди" value={counts?.total ?? "—"} note="спорных записей" />
          <Tile label="Grey-zone" value={counts?.greyZone ?? "—"} note="равное доверие" />
          <Tile label="Unknown source" value={counts?.unknownSource ?? "—"} note="нет в реестре" />
        </div>
        <div className="card">
          {items.length === 0 ? (
            <div className="sv-empty">
              {vertical === "person"
                ? "Очередь пуста — все конфликты по физлицам разрешены."
                : "Очередь пуста — все конфликты по юрлицам разрешены."}
            </div>
          ) : (
            items.map((c) => (
              <div className="cf-item" key={c.id}>
                <div className="cf-av">
                  {vertical === "person"
                    ? <Avatar seed={c.personName ?? String(c.id)} />
                    : <Icon name="building" size={18} />}
                </div>
                <div>
                  <div className="cf-nm">
                    {c.personName ?? "Без имени"}
                    {c.conflicts.length > 0 && (
                      <>
                        {" · "}
                        <span className="mono" style={{ fontWeight: 400, color: "var(--ink-2)" }}>
                          {c.conflicts.map((f) => f.fieldName).join(", ")}
                        </span>
                      </>
                    )}
                  </div>
                  <div className="cf-sub">
                    {c.grClientId
                      ? `${vertical === "person" ? "GR" : "LE"} ${c.grClientId.slice(0, 8)}… · расхождение значений при слиянии`
                      : `источник ${c.sourceName ?? "?"} · нет в реестре`}
                  </div>
                </div>
                <div className="cf-meta">
                  <Chip tone={c.reason === "GREY_ZONE_CONFLICT" ? "warn" : "info"}>
                    {c.reason === "GREY_ZONE_CONFLICT" ? "grey-zone" : "unknown source"}
                  </Chip>
                  <span className="cf-age">{age(c.createdAt)}</span>
                  <button className="btn btn-primary btn-xs" onClick={() => setSelected(c)}>Разрешить</button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
      {selected && (
        <ResolveModal
          item={selected}
          resolve={resolve}
          onClose={() => setSelected(null)}
          onResolved={() => { setSelected(null); void load(vertical); }}
        />
      )}
    </section>
  );
}
