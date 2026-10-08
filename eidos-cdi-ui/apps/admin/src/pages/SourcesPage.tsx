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

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { AxiosError } from "axios";
import { Modal, St, Toggle, TrustBar, ViewBar, toast } from "@eidos/ui-kit";
import {
  createSource, deleteSource, listSources, updateSource,
  type Source, type SourceInput,
} from "../api/sources";

function axiosDetail(err: unknown): string | undefined {
  return err instanceof AxiosError
    ? (err.response?.data as { detail?: string } | undefined)?.detail
    : undefined;
}

function randomToken(): string {
  const bytes = new Uint8Array(24);
  crypto.getRandomValues(bytes);
  return "tk_" + btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function SourceModal({ source, onClose, onSaved }: { source: Source | null; onClose: () => void; onSaved: () => void }) {
  const editing = source !== null;
  const [form, setForm] = useState<SourceInput>({
    code: source?.code ?? "",
    name: source?.name ?? "",
    token: source?.token ?? "",
    trustLevel: source?.trustLevel ?? 5,
    enabled: source?.enabled ?? true,
  });
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = <K extends keyof SourceInput>(k: K, v: SourceInput[K]) => setForm((f) => ({ ...f, [k]: v }));

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      if (editing) await updateSource(source.id, form);
      else await createSource(form);
      toast("Источник сохранён");
      onSaved();
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось сохранить источник.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title={editing ? "Источник" : "Новый источник"}
      subtitle={editing ? "Изменение параметров подключения и доверия" : "Регистрация системы-источника в реестре"}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>Отмена</button>
          <button className="btn btn-primary btn-sm" onClick={submit} disabled={busy}>
            {busy ? "Сохранение…" : "Сохранить"}
          </button>
        </>
      }
    >
      <div className="frow2">
        <div className="field">
          <label>Код <span className="req">*</span></label>
          <input value={form.code} onChange={(e) => set("code", e.target.value)} placeholder="напр. tieto" />
        </div>
        <div className="field">
          <label>Уровень доверия (1–10)</label>
          <input type="number" min={1} max={10} value={form.trustLevel} onChange={(e) => set("trustLevel", Number(e.target.value))} />
        </div>
      </div>
      <div className="field">
        <label>Название <span className="req">*</span></label>
        <input value={form.name} onChange={(e) => set("name", e.target.value)} placeholder="Человекочитаемое имя" />
      </div>
      <div className="field">
        <label>Токен доступа (X-Access-Token)</label>
        <div style={{ display: "flex", gap: 9 }}>
          <input value={form.token} onChange={(e) => set("token", e.target.value)} style={{ flex: 1 }} />
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => { set("token", randomToken()); toast("Новый токен сгенерирован"); }}>
            Сгенерировать
          </button>
        </div>
      </div>
      <div className="fcheck">
        <Toggle on={form.enabled} onChange={(v) => set("enabled", v)} />
        <span>Источник активен</span>
      </div>
      {error && <div className="errmsg">{error}</div>}
    </Modal>
  );
}

export function SourcesPage() {
  const [sources, setSources] = useState<Source[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<{ open: boolean; source: Source | null }>({ open: false, source: null });

  const load = useCallback(async () => {
    setError(null);
    try {
      setSources(await listSources());
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось загрузить источники.");
      setSources([]);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const remove = async (s: Source) => {
    if (!window.confirm(`Удалить источник «${s.name}»?`)) return;
    try {
      await deleteSource(s.id);
      toast("Источник удалён");
      void load();
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось удалить источник.");
    }
  };

  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Реестр источников"
          sub="Единый список систем-источников для stage, gateway и core. Уровень доверия управляет приоритетом при слиянии."
          actions={
            <button className="btn btn-primary btn-sm" onClick={() => setModal({ open: true, source: null })}>
              Новый источник
            </button>
          }
        />
        {error && <div className="errmsg" style={{ marginBottom: 16 }}>{error}</div>}
        <div className="card">
          <table className="tbl">
            <thead>
              <tr><th>Код</th><th>Название</th><th>Доверие</th><th>Токен</th><th>Статус</th><th></th></tr>
            </thead>
            <tbody>
              {(sources ?? []).map((s) => (
                <tr key={s.id}>
                  <td><code>{s.code}</code></td>
                  <td><b>{s.name}</b></td>
                  <td><TrustBar value={s.trustLevel} /></td>
                  <td className="muted"><code>{s.token.slice(0, 5)}…{s.token.slice(-2)}</code></td>
                  <td>{s.enabled ? <St tone="on">активен</St> : <St tone="off">выключен</St>}</td>
                  <td className="acts">
                    <button className="btn btn-ghost btn-xs" onClick={() => setModal({ open: true, source: s })}>Изменить</button>
                    <button className="btn btn-danger btn-xs" onClick={() => remove(s)}>Удалить</button>
                  </td>
                </tr>
              ))}
              {sources !== null && sources.length === 0 && (
                <tr><td colSpan={6} className="muted">Источников пока нет — добавьте первый.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      {modal.open && (
        <SourceModal
          source={modal.source}
          onClose={() => setModal({ open: false, source: null })}
          onSaved={() => { setModal({ open: false, source: null }); void load(); }}
        />
      )}
    </section>
  );
}
