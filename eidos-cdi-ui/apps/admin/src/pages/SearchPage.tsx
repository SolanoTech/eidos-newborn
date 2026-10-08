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
import { useNavigate } from "react-router-dom";
import { AxiosError } from "axios";
import { Avatar, Icon } from "@eidos/ui-kit";
import { structuredSearch, type GoldenRecordSummary, type StructuredQuery } from "../api/search";

function fio(r: GoldenRecordSummary): string {
  return [r.grLastName, r.grFirstName, r.grMiddleName].filter(Boolean).join(" ");
}

export function SearchPage() {
  const navigate = useNavigate();
  const [q, setQ] = useState<StructuredQuery>({});
  const [results, setResults] = useState<GoldenRecordSummary[] | null>(null);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const set = (k: keyof StructuredQuery, v: string) => setQ((prev) => ({ ...prev, [k]: v }));

  const run = async () => {
    setError(null);
    setBusy(true);
    try {
      const page = await structuredSearch(q, 1);
      setResults(page.content);
      setTotal(page.totalElements);
    } catch (err) {
      const detail = err instanceof AxiosError
        ? (err.response?.data as { detail?: string } | undefined)?.detail
        : undefined;
      setError(detail ?? "Не удалось выполнить поиск.");
      setResults(null);
    } finally {
      setBusy(false);
    }
  };

  const clear = () => {
    setQ({});
    setResults(null);
    setError(null);
  };

  const onKey = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") void run();
  };

  return (
    <section className="view-panel">
      <div className="search-view">
        <div className="sv-head">
          <div className="sv-title">Поиск Golden Records</div>
          <div className="sv-sub">Идентификация субъекта по ПИНФЛ, паспорту, ФИО или телефону.</div>
        </div>
        <div className="sv-fields" onKeyDown={onKey}>
          <div className="sv-fl full">
            <label>ПИНФЛ</label>
            <span className="inp"><input value={q.pinfl ?? ""} onChange={(e) => set("pinfl", e.target.value)} placeholder="14 цифр" inputMode="numeric" maxLength={14} /></span>
          </div>
          <div className="sv-fl full">
            <label>Серия и № паспорта</label>
            <span className="inp"><input value={q.passport ?? ""} onChange={(e) => set("passport", e.target.value)} placeholder="AB 1234567" /></span>
          </div>
          <div className="sv-fl">
            <label>Фамилия</label>
            <span className="inp"><input value={q.lastName ?? ""} onChange={(e) => set("lastName", e.target.value)} placeholder="Каримов" /></span>
          </div>
          <div className="sv-fl">
            <label>Имя</label>
            <span className="inp"><input value={q.firstName ?? ""} onChange={(e) => set("firstName", e.target.value)} placeholder="Алишер" /></span>
          </div>
          <div className="sv-fl">
            <label>Отчество</label>
            <span className="inp"><input value={q.middleName ?? ""} onChange={(e) => set("middleName", e.target.value)} placeholder="Бахтиёрович" /></span>
          </div>
          <div className="sv-fl">
            <label>Телефон</label>
            <span className="inp"><input value={q.phone ?? ""} onChange={(e) => set("phone", e.target.value)} placeholder="+998 ..." /></span>
          </div>
        </div>
        <div className="sv-actions">
          <button className="btn btn-ghost" onClick={clear}>Сбросить</button>
          <button className="btn btn-primary" onClick={run} disabled={busy}>
            {busy ? "Поиск…" : "Найти клиента"}
          </button>
        </div>
        <div className="sv-results">
          {error && <div className="errmsg">{error}</div>}
          {results !== null && !error && (
            results.length === 0 ? (
              <div className="sv-empty">Совпадений не найдено. Уточните критерии поиска.</div>
            ) : (
              <>
                <div className="sv-rescap">{total} совпадений</div>
                {results.map((r) => (
                  <div className="sv-res" key={r.grClientId} onClick={() => navigate(`/golden-records/${r.grClientId}`)}>
                    <div className="rphoto"><Avatar seed={(r.grLastName ?? "") + (r.grFirstName ?? "")} /></div>
                    <div>
                      <div className="rn">{fio(r) || "Без имени"}</div>
                      <div className="rm">ПИНФЛ {r.grPinfl ?? "—"} · {r.grMobilePhoneMain ?? "—"}</div>
                    </div>
                    <div className="score">точное совпадение</div>
                    <span className="chev"><Icon name="chevronRight" size={16} /></span>
                  </div>
                ))}
              </>
            )
          )}
        </div>
      </div>
    </section>
  );
}
