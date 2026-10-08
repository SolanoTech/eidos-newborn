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

import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { CardPanel, Chip, Icon, Modal, SegItem, Tile, Toggle, ViewBar, toast } from "@eidos/ui-kit";
import {
  getConsents, getExternalIds, getFieldMeta, grantConsent, revokeConsent,
  type ConsentStatus, type ExternalId, type FieldMeta,
} from "../api/customer";
import { useCustomer, fio, demoBio } from "../CustomerContext";

const CONSENT_LABELS: Record<string, string> = {
  PERSONAL_DATA: "Персональные данные",
  BIO: "Биометрия",
  MARKETING_SMS: "Маркетинг · SMS",
  MARKETING_PUSH: "Маркетинг · Push",
  MARKETING_EMAIL: "Маркетинг · E-mail",
  PROFILING: "Профилирование",
  THIRD_PARTY: "Передача 3-м лицам",
};

function fmtD(iso: string | null): string {
  if (!iso) return "";
  const [y, m, d] = iso.split("-");
  return `${d}.${m}.${y}`;
}

/** Группировка провенанса по источникам для модалки «Источники слияния». */
function groupSources(meta: FieldMeta[]) {
  const map = new Map<string, { fields: number; trust: number | null; last: string | null }>();
  for (const m of meta) {
    const g = map.get(m.sourceName) ?? { fields: 0, trust: m.trustLevel, last: null };
    g.fields += 1;
    if ((m.trustLevel ?? 0) > (g.trust ?? 0)) g.trust = m.trustLevel;
    if (!g.last || (m.updatedAt ?? "") > g.last) g.last = m.updatedAt;
    map.set(m.sourceName, g);
  }
  return [...map.entries()].map(([source, g]) => ({ source, ...g }));
}

function MyidModal({ onClose }: { onClose: () => void }) {
  const { detail, clientId } = useCustomer();
  const bio = demoBio(clientId);
  const [pct, setPct] = useState(0);
  const [step, setStep] = useState("Установка защищённого соединения…");
  const [done, setDone] = useState(false);
  const C = 326.726;

  useEffect(() => {
    const steps: Array<[number, string]> = [
      [8, "Установка защищённого соединения…"], [26, "Отправка запроса в ГЦП «MyID»…"],
      [48, "Сверка ПИНФЛ и паспорта…"], [70, "Проверка биометрического шаблона…"],
      [90, "Формирование протокола сверки…"], [100, "Готово"],
    ];
    let p = 0;
    const t = window.setInterval(() => {
      p = Math.min(100, p + Math.random() * 3 + 1.4);
      setPct(p);
      const s = steps.filter(([n]) => p >= n).pop();
      if (s) setStep(s[1]);
      if (p >= 100) {
        window.clearInterval(t);
        window.setTimeout(() => setDone(true), 480);
      }
    }, 90);
    return () => window.clearInterval(t);
  }, []);

  const checks: Array<[string, string, boolean]> = [
    ["ПИНФЛ", detail?.grPinfl ?? "—", true],
    ["Ф.И.О.", fio(detail) || "—", true],
    ["Паспорт", detail?.grDocPassData ?? "—", true],
    ["Биометрия (Face)", bio.ok ? "шаблон совпал" : "не предоставлена клиентом", bio.ok],
  ];

  return (
    <Modal
      title="Сверка с MyID"
      subtitle={`Гос. биометрическая идентификация · ${fio(detail) || "клиент"} · демо-интеграция`}
      onClose={onClose}
      footer={<button className="btn btn-ghost btn-sm" onClick={onClose}>Закрыть</button>}
    >
      {!done ? (
        <div className="myid-stage">
          <div className="ring-wrap">
            <svg viewBox="0 0 120 120" className="pring" aria-hidden="true">
              <circle className="pring-bg" cx="60" cy="60" r="52" />
              <circle className="pring-fg" cx="60" cy="60" r="52" style={{ strokeDashoffset: C * (1 - pct / 100) }} />
            </svg>
            <div className="pring-num"><span>{Math.round(pct)}</span><i>%</i></div>
          </div>
          <div className="myid-step">{step}</div>
        </div>
      ) : (
        <div className="myid-stage myid-result">
          <div className={"myid-badge" + (bio.ok ? "" : " warn")}>{bio.ok ? "✓" : "!"}</div>
          <div className="myid-verdict">{bio.ok ? "Личность подтверждена" : "Подтверждено по документам"}</div>
          <div className="myid-conf">Достоверность сверки · <b>{bio.ok ? "99.4%" : "92.7%"}</b></div>
          <div className="myid-checks">
            {checks.map(([k, v, ok]) => (
              <div className="myid-check" key={k}>
                <span>{k}</span>
                <span className={ok ? "ok" : "no"}>{ok ? "✓ " : "— "}{v}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </Modal>
  );
}

function SourcesModal({ meta, onClose }: { meta: FieldMeta[]; onClose: () => void }) {
  const { detail } = useCustomer();
  const groups = groupSources(meta);
  const col = (t: number | null) => (t ?? 0) >= 8 ? "var(--ok)" : (t ?? 0) >= 5 ? "var(--gold)" : "var(--info)";
  return (
    <Modal
      title="Источники единой записи"
      subtitle={`${detail?.grClientId.slice(0, 13) ?? "GR"}… · слияние ${groups.length} систем`}
      onClose={onClose}
      footer={<button className="btn btn-ghost btn-sm" onClick={onClose}>Закрыть</button>}
    >
      <div className="src-sub">Детерминированный резолв по ПИНФЛ · survivorship по уровню доверия</div>
      <div className="src-list">
        {groups.length === 0 && <div className="sv-empty">Провенанс пока не записан — запись создана напрямую.</div>}
        {groups.map((g) => (
          <div className="src-item" key={g.source}>
            <div className="src-ic">{(g.source[0] ?? "•").toUpperCase()}</div>
            <div className="src-main">
              <div className="src-name">{g.source} <span className="src-tag">детерм.</span></div>
              <div className="src-sys">{g.fields} полей профиля</div>
              <div className="src-id">trust {g.trust ?? "—"}</div>
            </div>
            <div className="src-meta">
              <div className="src-date">обновлено {g.last ? fmtD(g.last.slice(0, 10)) : "—"}</div>
              <div className="src-conf" style={{ color: col(g.trust) }}>
                <i><b style={{ width: `${(g.trust ?? 0) * 10}%`, background: col(g.trust) }} /></i>
                {g.trust ?? 0}/10
              </div>
            </div>
          </div>
        ))}
      </div>
    </Modal>
  );
}

function ExportModal({ meta, externalIds, onClose }: { meta: FieldMeta[]; externalIds: ExternalId[]; onClose: () => void }) {
  const { detail } = useCustomer();
  const [fmt, setFmt] = useState<"card" | "json">("card");
  const record = useMemo(() => ({
    golden_record_id: detail?.grClientId,
    version: detail?.version,
    resolution_type: "Детерминированный (ПИНФЛ)",
    sources_merged: groupSources(meta).length,
    created: detail?.createdAt,
    subject: {
      last_name: detail?.grLastName, first_name: detail?.grFirstName, middle_name: detail?.grMiddleName,
      pinfl: detail?.grPinfl, passport: detail?.grDocPassData, phone: detail?.grMobilePhoneMain,
      email: detail?.grContactsEmail, citizenship: detail?.grCitizenship,
    },
    external_ids: externalIds,
    provenance: meta,
    exported_at: new Date().toISOString(),
  }), [detail, meta, externalIds]);

  const rows: Array<[string, string]> = [
    ["Golden Record ID", detail?.grClientId ?? "—"],
    ["Версия", String(detail?.version ?? "—")],
    ["Тип резолва", "Детерминированный (ПИНФЛ)"],
    ["Источников слито", String(groupSources(meta).length)],
    ["Фамилия", detail?.grLastName ?? "—"],
    ["Имя", detail?.grFirstName ?? "—"],
    ["Отчество", detail?.grMiddleName ?? "—"],
    ["ПИНФЛ", detail?.grPinfl ?? "—"],
    ["Паспорт", detail?.grDocPassData ?? "—"],
    ["Телефон", detail?.grMobilePhoneMain ?? "—"],
    ["E-mail", detail?.grContactsEmail ?? "—"],
    ["Гражданство", detail?.grCitizenship ?? "—"],
  ];

  const download = () => {
    const blob = new Blob([JSON.stringify(record, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `golden-record-${detail?.grClientId ?? "record"}.json`;
    a.click();
    URL.revokeObjectURL(url);
    toast("Запись экспортирована · JSON");
    onClose();
  };

  return (
    <Modal
      title="Экспорт записи"
      subtitle="Предпросмотр Golden Record перед скачиванием"
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>Отмена</button>
          <button className="btn btn-primary btn-sm" onClick={download}>Скачать JSON</button>
        </>
      }
    >
      <div className="exp-tabs">
        <span className={"exp-tab" + (fmt === "card" ? " on" : "")} onClick={() => setFmt("card")}>Карточка</span>
        <span className={"exp-tab" + (fmt === "json" ? " on" : "")} onClick={() => setFmt("json")}>JSON</span>
      </div>
      <div className="exp-preview">
        {fmt === "json" ? (
          <div className="exp-json">{JSON.stringify(record, null, 2)}</div>
        ) : (
          rows.map(([k, v]) => (
            <div className="exp-row" key={k}><span className="k">{k}</span><span className="v">{v}</span></div>
          ))
        )}
      </div>
    </Modal>
  );
}

const ID_HISTORY_DEMO = [
  { d: "12.03.2021", t: "Идентификатор привязан", m: "Первичная привязка в источнике", s: "Источник", k: "add" },
  { d: "15.01.2023", t: "Верифицирован", m: "Сопоставление по ПИНФЛ", s: "EIDOS · merge", k: "verify" },
  { d: "21.06.2025", t: "Статус: активен", m: "Сопоставлен в Golden Record", s: "EIDOS · core", k: "verify" },
];

function IdHistoryModal({ ext, onClose }: { ext: ExternalId; onClose: () => void }) {
  const kmap: Record<string, string> = { add: "Привязка", verify: "Верификация", change: "Изменение", revoke: "Отвязка" };
  return (
    <Modal
      title="История изменений"
      subtitle={`${ext.sourceName} · ${ext.externalId} · журнал — демо`}
      onClose={onClose}
      footer={<button className="btn btn-ghost btn-sm" onClick={onClose}>Закрыть</button>}
    >
      <div className="idh-head">{ID_HISTORY_DEMO.length} записей в журнале</div>
      <div className="idh-list">
        {ID_HISTORY_DEMO.map((e, i) => (
          <div className={"idh-item " + e.k} key={i}>
            <div className="idh-dot" />
            <div className="idh-main">
              <div className="idh-top"><span className="idh-t">{e.t}</span><span className="idh-d">{e.d}</span></div>
              <div className="idh-m">{e.m}</div>
              <div className="idh-s"><span className="idh-tag">{kmap[e.k]}</span>{e.s}</div>
            </div>
          </div>
        ))}
      </div>
    </Modal>
  );
}

export function C360Page() {
  const navigate = useNavigate();
  const { clientId, detail } = useCustomer();
  const [meta, setMeta] = useState<FieldMeta[]>([]);
  const [externalIds, setExternalIds] = useState<ExternalId[]>([]);
  const [consents, setConsents] = useState<ConsentStatus[]>([]);
  const [modal, setModal] = useState<"" | "myid" | "export" | "sources">("");
  const [idHist, setIdHist] = useState<ExternalId | null>(null);
  const bio = demoBio(clientId);

  const loadConsents = (id: string) => {
    getConsents(id).then(setConsents).catch(() => setConsents([]));
  };

  useEffect(() => {
    if (!clientId) return;
    getFieldMeta(clientId).then(setMeta).catch(() => setMeta([]));
    getExternalIds(clientId).then(setExternalIds).catch(() => setExternalIds([]));
    loadConsents(clientId);
  }, [clientId]);

  if (!clientId) {
    return (
      <section className="view-panel">
        <div className="wrap">
          <div className="sv-empty">
            Клиент не выбран.{" "}
            <button className="btn btn-ghost btn-sm" onClick={() => navigate("/search")}>Найти клиента</button>
          </div>
        </div>
      </section>
    );
  }

  const toggleConsent = async (c: ConsentStatus) => {
    try {
      if (c.active && c.consentId) {
        await revokeConsent(c.consentId);
        toast(`Согласие «${CONSENT_LABELS[c.type] ?? c.type}» отозвано`);
      } else {
        await grantConsent(clientId, c.code);
        toast(`Согласие «${CONSENT_LABELS[c.type] ?? c.type}» выдано`);
      }
      loadConsents(clientId);
    } catch {
      toast("Не удалось изменить согласие");
    }
  };

  const sourcesCount = groupSources(meta).length;

  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Customer 360"
          sub="Единый профиль: личность, ценность, поведение, согласия и предиктивные атрибуты."
          actions={
            <>
              <button className="btn btn-ghost btn-sm" onClick={() => setModal("myid")}>Сверить с MyID</button>
              <button className="btn btn-primary btn-sm" onClick={() => setModal("export")}>Экспорт записи</button>
            </>
          }
        />

        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 10 }}>
          <Chip tone="info">аналитические блоки — демо</Chip>
        </div>
        <div className="tiles" style={{ marginBottom: 16 }}>
          <Tile label="Lifetime Value" value="₽ 184" unit="млн" note="+12% за квартал" />
          <Tile label="RFM-сегмент" value="Champions" note="R5 · F4 · M5" />
          <Tile label="Риск оттока" value="7" unit="%" note="низкий" />
          <Tile label="Engagement" value="86" note="активность 30 дн." />
        </div>

        <div className="golden" role="button" tabIndex={0} onClick={() => setModal("sources")} style={{ marginBottom: 16 }} title="Показать источники слияния">
          <div className="gr-top"><b>◆</b> Golden Record · Единая запись <span className="gr-hint">Источники слияния →</span></div>
          <div className="gr-id">{detail?.grClientId ?? "—"}</div>
          <div className="gr-row">
            <div className="gr-stat"><div className="l">Версия записи</div><div className="n">{detail?.version ?? "—"}</div></div>
            <div className="gr-stat"><div className="l">Источников слито</div><div className="n">{sourcesCount}</div></div>
            <div className="gr-stat"><div className="l">Тип резолва</div><div className="n">Детерм. <small>(ПИНФЛ)</small></div></div>
            <div className="gr-stat"><div className="l">Создан</div><div className="n">{detail?.createdAt ? fmtD(detail.createdAt.slice(0, 10)) : "—"}</div></div>
          </div>
        </div>

        <div className="grid g-2">
          <CardPanel icon={<Icon name="id" size={16} />} title="Персональные данные">
            <div className="attrs">
              <div className="attr"><span className="k">Фамилия</span><span className="v">{detail?.grLastName ?? "—"}</span></div>
              <div className="attr"><span className="k">Имя</span><span className="v">{detail?.grFirstName ?? "—"}</span></div>
              <div className="attr"><span className="k">Отчество</span><span className="v">{detail?.grMiddleName ?? "—"}</span></div>
              <div className="attr"><span className="k">Дата рождения</span><span className="v mono">{detail?.grBirthDate ? fmtD(detail.grBirthDate) : "—"}</span></div>
              <div className="attr"><span className="k">Пол</span><span className="v">{detail?.grGender === "M" || detail?.grGender === "MALE" ? "Мужской" : detail?.grGender === "F" || detail?.grGender === "FEMALE" ? "Женский" : "—"}</span></div>
              <div className="attr"><span className="k">Гражданство</span><span className="v">{detail?.grCitizenship ?? "—"}</span></div>
              <div className="attr"><span className="k">Место рождения</span><span className="v">{detail?.grBirthPlace ?? "—"}</span></div>
            </div>
          </CardPanel>
          <CardPanel icon={<Icon name="database" size={16} />} title="Документы и контакты">
            <div className="attrs">
              <div className="attr"><span className="k">ПИНФЛ</span><span className="v mono">{detail?.grPinfl ?? "—"} <span className="verified">✓ верифиц.</span></span></div>
              <div className="attr"><span className="k">Паспорт</span><span className="v mono">{detail?.grDocPassData ?? "—"}</span></div>
              <div className="attr"><span className="k">Дата выдачи</span><span className="v mono">{detail?.grDocIssuedDate ? fmtD(detail.grDocIssuedDate) : "—"}</span></div>
              <div className="attr"><span className="k">Телефон</span><span className="v mono">{detail?.grMobilePhoneMain ?? "—"} <span className="verified">✓ OTP</span></span></div>
              <div className="attr"><span className="k">E-mail</span><span className="v">{detail?.grContactsEmail ?? "—"}</span></div>
              <div className="attr"><span className="k">Адрес</span><span className="v">{detail?.grAddrPermanentAddress ?? "—"}</span></div>
              <div className="attr"><span className="k">Биометрия</span><span className="v">{bio.label} <span className="src">демо</span></span></div>
            </div>
          </CardPanel>
        </div>

        <div className="grid g-3" style={{ marginTop: 16 }}>
          <CardPanel title="Поведенческие сегменты" right={<Chip tone="info">демо</Chip>}>
            <SegItem label="Affluent banking" pct={92} />
            <SegItem label="Цифровой канал" pct={88} />
            <SegItem label="Кредитный аппетит" pct={64} />
            <SegItem label="Сберегательное" pct={71} />
          </CardPanel>
          <CardPanel title="Продукты на руках" right={<Chip tone="info">демо</Chip>}>
            <div className="attrs">
              <div className="attr"><span className="k">Дебетовая HUMO</span><span className="v">2 карты</span></div>
              <div className="attr"><span className="k">Депозит</span><span className="v">₽ 95 млн</span></div>
              <div className="attr"><span className="k">Рассрочка</span><span className="v">активна</span></div>
              <div className="attr"><span className="k">Кредитная линия</span><span className="v" style={{ color: "var(--ink-dim)" }}>нет</span></div>
              <div className="attr"><span className="k">P2P / переводы</span><span className="v">часто</span></div>
            </div>
          </CardPanel>
          <CardPanel title="Согласия">
            {consents.map((c) => (
              <div className="consent" key={c.type}>
                <div className="cst-l">
                  <span className="cst-name">{CONSENT_LABELS[c.type] ?? c.type}</span>
                  <span className="cst-exp">
                    {c.active
                      ? `действует до ${fmtD(c.endDate)}`
                      : c.endDate
                        ? `отозвано / истекло ${fmtD(c.endDate)}`
                        : "не предоставлено"}
                  </span>
                </div>
                <Toggle on={c.active} onChange={() => toggleConsent(c)} />
              </div>
            ))}
            {consents.length === 0 && <div className="sv-empty">Сводка согласий недоступна.</div>}
          </CardPanel>
        </div>

        <CardPanel
          icon={<Icon name="target" size={16} />}
          title="Связанные идентификаторы (граф личности)"
          right={`${externalIds.length} идентификаторов`}
          style={{ marginTop: 16 }}
        >
          <div className="id-links">
            {externalIds.length === 0 && (
              <div className="sv-empty" style={{ width: "100%" }}>
                Внешние идентификаторы не привязаны — появятся после приёма данных из источников.
              </div>
            )}
            {externalIds.map((ext) => (
              <div className="idlink" role="button" tabIndex={0} key={ext.sourceName + ext.externalId} onClick={() => setIdHist(ext)}>
                <div className="il-ic" style={{ background: "var(--accent)", color: "var(--accent-ink)" }}>
                  {(ext.sourceName[0] ?? "•").toUpperCase()}
                </div>
                <div>
                  <div className="il-k">{ext.sourceName}</div>
                  <div className="il-v">{ext.externalId}</div>
                </div>
              </div>
            ))}
          </div>
        </CardPanel>

        <div className="grid g-2" style={{ marginTop: 16 }}>
          <CardPanel icon={<Icon name="chart" size={16} />} title="Активность · 30 дней" right={<Chip tone="info">демо</Chip>}>
            <svg viewBox="0 0 600 120" width="100%" height="110" preserveAspectRatio="none">
              <path d="M0 90 L40 80 L80 88 L120 60 L160 72 L200 48 L240 58 L280 30 L320 44 L360 36 L400 52 L440 28 L480 40 L520 22 L560 34 L600 18" fill="none" stroke="var(--accent-2)" strokeWidth="2" />
            </svg>
          </CardPanel>
          <CardPanel title="Предиктивные атрибуты (Glow)" right={<Chip tone="info">демо</Chip>}>
            <div className="attrs">
              <div className="attr"><span className="k">Next Best Offer</span><span className="v">Премиальный депозит</span></div>
              <div className="attr"><span className="k">Предпочт. канал</span><span className="v">Push · 19:00–21:00</span></div>
              <div className="attr"><span className="k">Чувствительность к цене</span><span className="v">Низкая</span></div>
              <div className="attr"><span className="k">Up-sell вероятность</span><span className="v" style={{ color: "var(--ok)" }}>73%</span></div>
              <div className="attr"><span className="k">Финансовое здоровье</span><span className="v">Стабильно</span></div>
            </div>
          </CardPanel>
        </div>
      </div>

      {modal === "myid" && <MyidModal onClose={() => setModal("")} />}
      {modal === "sources" && <SourcesModal meta={meta} onClose={() => setModal("")} />}
      {modal === "export" && <ExportModal meta={meta} externalIds={externalIds} onClose={() => setModal("")} />}
      {idHist && <IdHistoryModal ext={idHist} onClose={() => setIdHist(null)} />}
    </section>
  );
}
