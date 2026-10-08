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
import { Avatar, Icon, toast } from "@eidos/ui-kit";
import { structuredSearch, type GoldenRecordSummary, type StructuredQuery } from "../api/customer";
import { searchByInn, searchByName, type LegalEntitySummary } from "../api/merchant";
import { useCustomer } from "../CustomerContext";

/** Кого ищем: физлицо-клиента или мерчанта. У них разные ключи поиска. */
type Subject = "client" | "merchant";

function name(r: GoldenRecordSummary): string {
  return [r.grLastName, r.grFirstName, r.grMiddleName].filter(Boolean).join(" ");
}

export function SearchPage() {
  const navigate = useNavigate();
  const { select } = useCustomer();
  const [subject, setSubject] = useState<Subject>("client");
  const [q, setQ] = useState<StructuredQuery>({});
  const [results, setResults] = useState<GoldenRecordSummary[] | null>(null);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Мерчанты: ИНН — точный ключ, название — неточный подбор.
  const [inn, setInn] = useState("");
  const [merchantName, setMerchantName] = useState("");
  const [merchants, setMerchants] = useState<LegalEntitySummary[] | null>(null);

  const switchSubject = (next: Subject) => {
    setSubject(next);
    setResults(null);
    setMerchants(null);
    setError(null);
  };

  const runMerchants = async () => {
    setError(null);
    setBusy(true);
    try {
      if (inn.trim()) {
        const found = await searchByInn(inn.trim());
        setMerchants(found ? [found] : []);
        setTotal(found ? 1 : 0);
        toast(found ? "Мерчант найден" : "Мерчант с таким ИНН не найден");
      } else {
        const page = await searchByName(merchantName.trim(), 1);
        setMerchants(page.content);
        setTotal(page.totalElements);
        toast(`${page.totalElements} мерчант(ов) найдено`);
      }
    } catch (err) {
      const detail = err instanceof AxiosError
        ? (err.response?.data as { detail?: string } | undefined)?.detail
        : undefined;
      setError(detail ?? "Не удалось выполнить поиск.");
      setMerchants(null);
    } finally {
      setBusy(false);
    }
  };

  const set = (k: keyof StructuredQuery, v: string) => setQ((p) => ({ ...p, [k]: v }));

  const run = async () => {
    setError(null);
    setBusy(true);
    try {
      const page = await structuredSearch(q, 1);
      setResults(page.content);
      setTotal(page.totalElements);
      toast(`${page.totalElements} клиент(ов) найдено`);
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

  const open = (r: GoldenRecordSummary) => {
    select(r.grClientId);
    navigate("/c360");
    toast("Открыт профиль: " + name(r));
  };

  return (
    <section className="view-panel">
      <div className="search-view">
        <div className="sv-head">
          <div className="sv-title">{subject === "client" ? "Поиск клиента" : "Поиск мерчанта"}</div>
          <div className="sv-sub">
            {subject === "client"
              ? "Идентификация субъекта по ПИНФЛ, паспорту, ФИО или телефону."
              : "Точная идентификация по ИНН либо подбор по названию мерчанта."}
          </div>
        </div>
        <div style={{ display: "flex", gap: 8, marginBottom: 14 }}>
          <button
            className={`btn btn-sm ${subject === "client" ? "btn-primary" : "btn-ghost"}`}
            onClick={() => switchSubject("client")}
          >
            Клиент
          </button>
          <button
            className={`btn btn-sm ${subject === "merchant" ? "btn-primary" : "btn-ghost"}`}
            onClick={() => switchSubject("merchant")}
          >
            Мерчант
          </button>
        </div>
        {subject === "client" ? (
        <div className="sv-fields" onKeyDown={(e) => { if (e.key === "Enter") void run(); }}>
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
            <span className="inp"><input value={q.lastName ?? ""} onChange={(e) => set("lastName", e.target.value)} placeholder="Каримова" /></span>
          </div>
          <div className="sv-fl">
            <label>Имя</label>
            <span className="inp"><input value={q.firstName ?? ""} onChange={(e) => set("firstName", e.target.value)} placeholder="Дилноза" /></span>
          </div>
          <div className="sv-fl">
            <label>Отчество</label>
            <span className="inp"><input value={q.middleName ?? ""} onChange={(e) => set("middleName", e.target.value)} placeholder="Шавкатовна" /></span>
          </div>
          <div className="sv-fl">
            <label>Телефон</label>
            <span className="inp"><input value={q.phone ?? ""} onChange={(e) => set("phone", e.target.value)} placeholder="+998 ..." /></span>
          </div>
        </div>
        ) : (
        <div className="sv-fields" onKeyDown={(e) => { if (e.key === "Enter") void runMerchants(); }}>
          <div className="sv-fl full">
            <label>ИНН</label>
            <span className="inp">
              <input value={inn} onChange={(e) => setInn(e.target.value)} placeholder="9 цифр · точный поиск" inputMode="numeric" maxLength={9} />
            </span>
          </div>
          <div className="sv-fl full">
            <label>Название мерчанта</label>
            <span className="inp">
              <input value={merchantName} onChange={(e) => setMerchantName(e.target.value)} placeholder="Оазис Маркет · OAZIS — если ИНН неизвестен" />
            </span>
          </div>
        </div>
        )}
        <div className="sv-actions">
          <button
            className="btn btn-ghost"
            onClick={() => { setQ({}); setInn(""); setMerchantName(""); setResults(null); setMerchants(null); setError(null); }}
          >
            Сбросить
          </button>
          <button
            className="btn btn-primary"
            onClick={subject === "client" ? run : runMerchants}
            disabled={busy || (subject === "merchant" && !inn.trim() && !merchantName.trim())}
          >
            {busy ? "Поиск…" : subject === "client" ? "Найти клиента" : "Найти мерчанта"}
          </button>
        </div>
        <div className="sv-results">
          {error && <div className="errmsg">{error}</div>}
          {subject === "merchant" && merchants !== null && !error && (
            merchants.length === 0 ? (
              <div className="sv-empty">Мерчант не найден. Попробуйте часть названия без организационно-правовой формы.</div>
            ) : (
              <>
                <div className="sv-rescap">{total} совпадений</div>
                {merchants.map((m) => (
                  <div className="sv-res" key={m.grLegalEntityId} onClick={() => navigate(`/merchant/${m.grLegalEntityId}`)}>
                    <div className="rphoto"><Icon name="building" size={18} /></div>
                    <div>
                      <div className="rn">{m.grFullName ?? m.grShortName ?? "Без названия"}</div>
                      <div className="rm">ИНН {m.grInn ?? "—"}{m.grOpfName ? ` · ${m.grOpfName}` : ""}</div>
                    </div>
                    <div className="score">{m.grIsBankrupt ? "банкрот" : m.grIsActive === false ? "не действует" : "действует"}</div>
                    <span className="chev"><Icon name="chevronRight" size={16} /></span>
                  </div>
                ))}
              </>
            )
          )}
          {subject === "client" && results !== null && !error && (
            results.length === 0 ? (
              <div className="sv-empty">Совпадений не найдено. Уточните критерии поиска.</div>
            ) : (
              <>
                <div className="sv-rescap">{total} совпадений</div>
                {results.map((r) => (
                  <div className="sv-res" key={r.grClientId} onClick={() => open(r)}>
                    <div className="rphoto"><Avatar seed={(r.grLastName ?? "") + (r.grFirstName ?? "")} /></div>
                    <div>
                      <div className="rn">{name(r) || "Без имени"}</div>
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
