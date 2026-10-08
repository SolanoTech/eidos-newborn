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
import { useNavigate, useParams } from "react-router-dom";
import { AxiosError } from "axios";
import { Avatar, CardPanel, Chip, Icon, ViewBar, toast } from "@eidos/ui-kit";
import { getFieldMeta, getGoldenRecord, type FieldMeta, type GoldenRecordDetail } from "../api/search";

function fio(r: GoldenRecordDetail): string {
  return [r.grLastName, r.grFirstName, r.grMiddleName].filter(Boolean).join(" ");
}

function genderLabel(g: string | null): string {
  if (g === "M" || g === "MALE") return "Мужской";
  if (g === "F" || g === "FEMALE") return "Женский";
  return g ?? "—";
}

export function RecordCardPage() {
  const { clientId = "" } = useParams();
  const navigate = useNavigate();
  const [record, setRecord] = useState<GoldenRecordDetail | null>(null);
  const [meta, setMeta] = useState<FieldMeta[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setError(null);
    void (async () => {
      try {
        const [r, m] = await Promise.all([
          getGoldenRecord(clientId),
          getFieldMeta(clientId).catch(() => [] as FieldMeta[]),
        ]);
        if (active) {
          setRecord(r);
          setMeta(m);
        }
      } catch (err) {
        const status = err instanceof AxiosError ? err.response?.status : undefined;
        if (active) setError(status === 404 ? "Профиль не найден." : "Не удалось загрузить профиль.");
      }
    })();
    return () => { active = false; };
  }, [clientId]);

  const exportJson = () => {
    if (!record) return;
    const blob = new Blob([JSON.stringify({ record, provenance: meta }, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `golden-record-${record.grClientId}.json`;
    a.click();
    URL.revokeObjectURL(url);
    toast("Экспорт записи сформирован (JSON)");
  };

  const attrs: Array<[string, string | null]> = record ? [
    ["Телефон", record.grMobilePhoneMain],
    ["ПИНФЛ", record.grPinfl],
    ["Дата рождения", record.grBirthDate],
    ["Пол", genderLabel(record.grGender)],
    ["Паспорт", record.grDocPassData],
    ["Дата выдачи документа", record.grDocIssuedDate],
    ["Место рождения", record.grBirthPlace],
    ["Гражданство", record.grCitizenship],
  ] : [];

  return (
    <section className="view-panel">
      <div className="wrap" style={{ maxWidth: 980 }}>
        <button className="link-back" onClick={() => navigate("/search")}>
          <Icon name="back" size={14} />
          К поиску
        </button>
        {error ? (
          <>
            <ViewBar title="Карточка клиента" />
            <div className="errmsg">{error}</div>
          </>
        ) : record ? (
          <>
            <div className="rc-head">
              <div className="rc-photo"><Avatar seed={(record.grLastName ?? "") + (record.grFirstName ?? "")} /></div>
              <div>
                <div className="rc-name">{fio(record) || "Без имени"}</div>
                <div className="rc-sub">gr_client_id {record.grClientId.slice(0, 8)}… · единый профиль клиента</div>
              </div>
              <div style={{ marginLeft: "auto", display: "flex", gap: 8, alignItems: "center" }}>
                <Chip tone="gold">версия {record.version ?? "—"}</Chip>
                <button className="btn btn-ghost btn-sm" onClick={exportJson}>Экспорт</button>
              </div>
            </div>
            <div className="grid g-2">
              <CardPanel icon={<Icon name="id" size={16} />} title="Поля профиля">
                <div className="attrs">
                  {attrs.map(([k, v]) => (
                    <div className="attr" key={k}>
                      <span className="k">{k}</span>
                      <span className="v mono">{v ?? "—"}</span>
                    </div>
                  ))}
                </div>
              </CardPanel>
              <CardPanel icon={<Icon name="clock" size={16} />} title="Происхождение полей" right="survivorship по доверию">
                <div className="attrs">
                  {meta.length === 0 ? (
                    <div className="sv-empty">Провенанс пока не записан для этого профиля.</div>
                  ) : (
                    meta.map((p) => (
                      <div className="attr" key={p.fieldName}>
                        <span className="k mono">{p.fieldName}</span>
                        <span className="v">
                          <span className="src-tag">{p.sourceName}</span>
                          <Chip tone="gold">trust {p.trustLevel ?? "—"}</Chip>
                        </span>
                      </div>
                    ))
                  )}
                </div>
              </CardPanel>
            </div>
          </>
        ) : (
          <div className="sv-empty">Загрузка…</div>
        )}
      </div>
    </section>
  );
}
