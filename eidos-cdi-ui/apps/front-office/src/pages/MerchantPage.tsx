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
import { CardPanel, Chip, Icon, Tile, ViewBar, toast } from "@eidos/ui-kit";
import {
  getLegalEntity, getLegalExternalIds, getLegalFieldMeta,
  type ExternalId, type FieldMeta, type LegalEntityDetail,
} from "../api/merchant";

function money(v: number | null): string {
  if (v == null) return "—";
  return new Intl.NumberFormat("ru-RU", { minimumFractionDigits: 2 }).format(v);
}

function yesNo(v: boolean | null): string {
  if (v == null) return "—";
  return v ? "да" : "нет";
}

/**
 * Merchant 360 — карточка юрлица во фронт-офисе. Реквизиты, контакты,
 * благонадёжность и происхождение полей приходят из core; блоки продуктовой
 * аналитики пока демонстрационные и помечены как таковые.
 */
export function MerchantPage() {
  const { merchantId = "" } = useParams();
  const navigate = useNavigate();
  const [record, setRecord] = useState<LegalEntityDetail | null>(null);
  const [meta, setMeta] = useState<FieldMeta[]>([]);
  const [externalIds, setExternalIds] = useState<ExternalId[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setError(null);
    void (async () => {
      try {
        const [r, m, x] = await Promise.all([
          getLegalEntity(merchantId),
          getLegalFieldMeta(merchantId).catch(() => [] as FieldMeta[]),
          getLegalExternalIds(merchantId).catch(() => [] as ExternalId[]),
        ]);
        if (active) {
          setRecord(r);
          setMeta(m);
          setExternalIds(x);
        }
      } catch (err) {
        const status = err instanceof AxiosError ? err.response?.status : undefined;
        if (active) setError(status === 404 ? "Мерчант не найден." : "Не удалось загрузить карточку.");
      }
    })();
    return () => { active = false; };
  }, [merchantId]);

  const exportJson = () => {
    if (!record) return;
    const blob = new Blob(
      [JSON.stringify({ record, provenance: meta, externalIds }, null, 2)],
      { type: "application/json" }
    );
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `merchant-${record.grLegalEntityId}.json`;
    a.click();
    URL.revokeObjectURL(url);
    toast("Карточка мерчанта выгружена (JSON)");
  };

  const requisites: Array<[string, string | null]> = record ? [
    ["ИНН", record.grInn],
    ["ОПФ", record.grOpfName],
    ["Дата регистрации", record.grRegistrationDate],
    ["Рег. номер", record.grRegistrationNumber],
    ["Уставный фонд", money(record.grStatutoryFund)],
    ["ОКЭД", [record.grOkedCode, record.grOkedName].filter(Boolean).join(" · ") || null],
    ["Статус деятельности", record.grActivityStateName],
  ] : [];

  const contacts: Array<[string, string | null]> = record ? [
    ["Адрес", record.grAddressFull],
    ["Регион", [record.grRegionName, record.grDistrictName].filter(Boolean).join(", ") || null],
    ["Телефоны", record.grPhones?.length ? record.grPhones.join(", ") : null],
    ["E-mail", record.grEmail],
    ["Руководитель", record.grDirectorName],
  ] : [];

  return (
    <section className="view-panel">
      <div className="wrap" style={{ maxWidth: 1080 }}>
        <button className="link-back" onClick={() => navigate("/search")}>
          <Icon name="back" size={14} />
          К поиску
        </button>

        {error ? (
          <>
            <ViewBar title="Merchant 360" />
            <div className="errmsg">{error}</div>
          </>
        ) : record ? (
          <>
            <div className="rc-head">
              <div className="rc-photo"><Icon name="building" size={22} /></div>
              <div>
                <div className="rc-name">{record.grFullName ?? record.grShortName ?? "Без названия"}</div>
                <div className="rc-sub">ИНН {record.grInn ?? "—"} · единый профиль мерчанта</div>
              </div>
              <div style={{ marginLeft: "auto", display: "flex", gap: 8, alignItems: "center" }}>
                {record.grIsBankrupt
                  ? <Chip tone="bad">банкрот</Chip>
                  : record.grIsActive === false
                    ? <Chip tone="warn">не действует</Chip>
                    : <Chip tone="ok">действует</Chip>}
                <Chip tone="gold">версия {record.version ?? "—"}</Chip>
                <button className="btn btn-ghost btn-sm" onClick={exportJson}>Экспорт</button>
              </div>
            </div>

            <div className="tiles" style={{ gridTemplateColumns: "repeat(4,1fr)", marginBottom: 16 }}>
              <Tile
                label="Рейтинг надёжности"
                value={record.grTrustRating ?? "—"}
                note={record.grTrustScore != null ? `скоринг ${record.grTrustScore}` : "по данным реестра"}
              />
              <Tile label="Плательщик НДС" value={yesNo(record.grIsVatPayer)} note={record.grVatNumber ?? "номер не указан"} />
              <Tile label="Малый бизнес" value={yesNo(record.grIsSmallBusiness)} note="категория субъекта" />
              <Tile label="Учредителей" value={record.grFoundersCount ?? record.grFounders?.length ?? "—"} note="по данным источника" />
            </div>

            <div className="grid g-2">
              <CardPanel icon={<Icon name="id" size={16} />} title="Реквизиты">
                <div className="attrs">
                  {requisites.map(([k, v]) => (
                    <div className="attr" key={k}>
                      <span className="k">{k}</span>
                      <span className="v mono">{v ?? "—"}</span>
                    </div>
                  ))}
                </div>
              </CardPanel>

              <CardPanel icon={<Icon name="link" size={16} />} title="Адрес и контакты">
                <div className="attrs">
                  {contacts.map(([k, v]) => (
                    <div className="attr" key={k}>
                      <span className="k">{k}</span>
                      <span className="v mono">{v ?? "—"}</span>
                    </div>
                  ))}
                </div>
              </CardPanel>

              <CardPanel
                icon={<Icon name="clock" size={16} />}
                title="Происхождение полей"
                right="survivorship по доверию"
              >
                <div className="attrs">
                  {meta.length === 0 ? (
                    <div className="sv-empty">Провенанс пока не записан.</div>
                  ) : (
                    meta.slice(0, 14).map((p) => (
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

              <CardPanel icon={<Icon name="database" size={16} />} title="Идентификаторы в системах">
                <div className="attrs">
                  {externalIds.length === 0 ? (
                    <div className="sv-empty">Связанных идентификаторов нет.</div>
                  ) : (
                    externalIds.map((x) => (
                      <div className="attr" key={`${x.sourceName}:${x.externalId}`}>
                        <span className="k">{x.sourceName}</span>
                        <span className="v mono">
                          {x.externalId}
                          {!x.active && <Chip tone="warn">неактивен</Chip>}
                        </span>
                      </div>
                    ))
                  )}
                </div>
              </CardPanel>

              <CardPanel
                icon={<Icon name="chart" size={16} />}
                title="Обороты и продукты"
                right={<Chip tone="info">демо</Chip>}
              >
                <div className="sv-empty">
                  Продуктовая аналитика мерчанта появится после подключения источников
                  эквайринга. Реквизиты и статусы выше — реальные данные Золотой записи.
                </div>
              </CardPanel>

              <CardPanel
                icon={<Icon name="users" size={16} />}
                title="Связанные лица"
                right={<Chip tone="info">демо</Chip>}
              >
                <div className="attrs">
                  {!record.grFounders?.length ? (
                    <div className="sv-empty">Учредители не переданы источником.</div>
                  ) : (
                    record.grFounders.map((f, i) => (
                      <div className="attr" key={i}>
                        <span className="k">{typeof f.name === "string" ? f.name : `учредитель ${i + 1}`}</span>
                        <span className="v mono">
                          {typeof f.sharePercent === "number" ? `${f.sharePercent}%` : "—"}
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
