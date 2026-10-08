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
import { Chip, Icon } from "@eidos/ui-kit";
import { searchByInn, searchByName, type LegalEntitySummary } from "../api/legal";

type Mode = "inn" | "name";

function axiosDetail(err: unknown): string | undefined {
  return err instanceof AxiosError
    ? (err.response?.data as { detail?: string } | undefined)?.detail
    : undefined;
}

function title(r: LegalEntitySummary): string {
  return r.grFullName ?? r.grShortName ?? "Без названия";
}

/**
 * Поиск юридических лиц. Два режима, потому что и природа поиска разная:
 * ИНН — самодостаточный ключ (точное попадание либо ничего), название —
 * неточное совпадение с ранжированием по сходству.
 */
export function LegalSearchPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<Mode>("inn");
  const [inn, setInn] = useState("");
  const [name, setName] = useState("");
  const [results, setResults] = useState<LegalEntitySummary[] | null>(null);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const run = async () => {
    setError(null);
    setBusy(true);
    try {
      if (mode === "inn") {
        const found = await searchByInn(inn.trim());
        setResults(found ? [found] : []);
        setTotal(found ? 1 : 0);
      } else {
        const page = await searchByName(name.trim(), 1);
        setResults(page.content);
        setTotal(page.totalElements);
      }
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось выполнить поиск.");
      setResults(null);
    } finally {
      setBusy(false);
    }
  };

  const clear = () => {
    setInn("");
    setName("");
    setResults(null);
    setError(null);
  };

  const onKey = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") void run();
  };

  const canRun = mode === "inn" ? inn.trim().length > 0 : name.trim().length > 0;

  return (
    <section className="view-panel">
      <div className="search-view">
        <div className="sv-head">
          <div className="sv-title">Поиск юридических лиц</div>
          <div className="sv-sub">
            Точная идентификация по ИНН либо подбор по названию мерчанта — с учётом
            организационно-правовой формы и разных написаний.
          </div>
        </div>

        <div className="modes" style={{ display: "flex", gap: 8, marginBottom: 14 }}>
          <button
            className={`btn btn-sm ${mode === "inn" ? "btn-primary" : "btn-ghost"}`}
            onClick={() => { setMode("inn"); setResults(null); setError(null); }}
          >
            По ИНН · точный
          </button>
          <button
            className={`btn btn-sm ${mode === "name" ? "btn-primary" : "btn-ghost"}`}
            onClick={() => { setMode("name"); setResults(null); setError(null); }}
          >
            По названию · неточный
          </button>
        </div>

        <div className="sv-fields" onKeyDown={onKey}>
          {mode === "inn" ? (
            <div className="sv-fl full">
              <label>ИНН</label>
              <span className="inp">
                <input
                  value={inn}
                  onChange={(e) => setInn(e.target.value)}
                  placeholder="9 цифр"
                  inputMode="numeric"
                  maxLength={9}
                />
              </span>
            </div>
          ) : (
            <div className="sv-fl full">
              <label>Название мерчанта</label>
              <span className="inp">
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Оазис Маркет · OAZIS · ООО «Оазис»"
                />
              </span>
            </div>
          )}
        </div>

        <div className="sv-actions">
          <button className="btn btn-ghost" onClick={clear}>Сбросить</button>
          <button className="btn btn-primary" onClick={run} disabled={busy || !canRun}>
            {busy ? "Поиск…" : "Найти"}
          </button>
        </div>

        <div className="sv-results">
          {error && <div className="errmsg">{error}</div>}
          {results !== null && !error && (
            results.length === 0 ? (
              <div className="sv-empty">
                {mode === "inn"
                  ? "Юрлицо с таким ИНН не найдено."
                  : "Совпадений не найдено. Попробуйте часть названия без организационно-правовой формы."}
              </div>
            ) : (
              <>
                <div className="sv-rescap">{total} совпадений</div>
                {results.map((r) => (
                  <div
                    className="sv-res"
                    key={r.grLegalEntityId}
                    onClick={() => navigate(`/legal-records/${r.grLegalEntityId}`)}
                  >
                    <div className="rphoto"><Icon name="building" size={18} /></div>
                    <div>
                      <div className="rn">{title(r)}</div>
                      <div className="rm">
                        ИНН {r.grInn ?? "—"}
                        {r.grOpfName ? ` · ${r.grOpfName}` : ""}
                      </div>
                    </div>
                    <div className="score" style={{ display: "flex", gap: 6 }}>
                      {r.grIsBankrupt
                        ? <Chip tone="bad">банкрот</Chip>
                        : r.grIsActive === false
                          ? <Chip tone="warn">не действует</Chip>
                          : <Chip tone="ok">действует</Chip>}
                    </div>
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
