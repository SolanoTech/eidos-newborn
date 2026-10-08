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
import { CardPanel, Chip, Icon, Modal, Toggle, ViewBar, toast } from "@eidos/ui-kit";
import { listSources, type Source } from "../api/sources";
import {
  addField, deleteField, getContract, listGoldenRecordFields, updateField, upsertContractMeta,
  type EntityType,
  type ContractView, type FieldDataType, type FieldInput, type FieldView, type GoldenRecordField,
} from "../api/contract";

function axiosDetail(err: unknown): string | undefined {
  return err instanceof AxiosError
    ? (err.response?.data as { detail?: string } | undefined)?.detail
    : undefined;
}

const DATA_TYPES: FieldDataType[] = ["STRING", "DATE", "INTEGER", "BOOLEAN", "GENDER"];

function parseValueMap(text: string): Record<string, string> | undefined {
  const trimmed = text.trim();
  if (!trimmed) return undefined;
  const map: Record<string, string> = {};
  for (const pair of trimmed.split(",")) {
    const [k, v] = pair.split(/[→=]/).map((s) => s.trim());
    if (k && v) map[k] = v;
  }
  return Object.keys(map).length ? map : undefined;
}

function formatValueMap(map: Record<string, string> | null): string {
  if (!map) return "";
  return Object.entries(map).map(([k, v]) => `${k}→${v}`).join(", ");
}

function FieldModal({
  sourceId, entityType, field, grFields, onClose, onSaved,
}: {
  sourceId: number;
  entityType: EntityType;
  field: FieldView | null;
  grFields: GoldenRecordField[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const editing = field !== null;
  const [src, setSrc] = useState(field?.sourceFieldName ?? "");
  const [target, setTarget] = useState(field?.targetGrField ?? grFields[0]?.jsonName ?? "");
  const [type, setType] = useState<FieldDataType>(field?.dataType ?? "STRING");
  const [dateFormat, setDateFormat] = useState(field?.sourceDateFormat ?? "");
  const [regex, setRegex] = useState(field?.validationRegex ?? "");
  const [valueMap, setValueMap] = useState(formatValueMap(field?.valueMap ?? null));
  const [defaultValue, setDefaultValue] = useState(field?.defaultValue ?? "");
  const [required, setRequired] = useState(field?.required ?? false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    const input: FieldInput = {
      sourceFieldName: src.trim(),
      targetGrField: target,
      dataType: type,
      required,
      sourceDateFormat: type === "DATE" ? dateFormat || null : null,
      validationRegex: regex || null,
      defaultValue: defaultValue || null,
      valueMap: parseValueMap(valueMap),
    };
    try {
      if (editing) await updateField(sourceId, field.id, input);
      else await addField(sourceId, input, entityType);
      toast("Поле сохранено · контракт получил новую версию");
      onSaved();
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось сохранить поле.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title={editing ? "Поле контракта" : "Новое поле"}
      subtitle="Сопоставление поля источника с полем Golden Record"
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
      <div className="field">
        <label>Поле источника (dot-path) <span className="req">*</span></label>
        <input value={src} onChange={(e) => setSrc(e.target.value)} placeholder="напр. person.phone" autoFocus />
      </div>
      <div className="field">
        <label>Целевое поле Golden Record</label>
        <select value={target} onChange={(e) => setTarget(e.target.value)}>
          {grFields.map((f) => (
            <option key={f.jsonName} value={f.jsonName}>
              {f.jsonName}{f.required ? " (обяз.)" : ""}
            </option>
          ))}
        </select>
      </div>
      <div className="frow2">
        <div className="field">
          <label>Тип</label>
          <select value={type} onChange={(e) => setType(e.target.value as FieldDataType)}>
            {DATA_TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Формат даты</label>
          <input value={dateFormat} onChange={(e) => setDateFormat(e.target.value)} placeholder="dd.MM.yyyy" disabled={type !== "DATE"} />
        </div>
      </div>
      <div className="field">
        <label>Regex валидации</label>
        <input value={regex} onChange={(e) => setRegex(e.target.value)} placeholder="^998\d{9}$" />
      </div>
      <div className="field">
        <label>Value-map</label>
        <input value={valueMap} onChange={(e) => setValueMap(e.target.value)} placeholder="напр. 1→M, 2→F" />
      </div>
      <div className="field">
        <label>Значение по умолчанию</label>
        <input value={defaultValue} onChange={(e) => setDefaultValue(e.target.value)} />
      </div>
      <div className="fcheck">
        <Toggle on={required} onChange={setRequired} />
        <span>Обязательное поле</span>
      </div>
      {error && <div className="errmsg">{error}</div>}
    </Modal>
  );
}

export function ConstructorPage() {
  const [sources, setSources] = useState<Source[]>([]);
  const [sourceId, setSourceId] = useState<number | null>(null);
  // Тип сущности задаёт, какие целевые поля Золотой записи доступны и какой из
  // контрактов источника редактируется: у источника их может быть два.
  const [entityType, setEntityType] = useState<EntityType>("PERSON");
  const [grFields, setGrFields] = useState<GoldenRecordField[]>([]);
  const [contract, setContract] = useState<ContractView | null>(null);
  const [identifier, setIdentifier] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<{ open: boolean; field: FieldView | null }>({ open: false, field: null });

  useEffect(() => {
    void (async () => {
      try {
        const list = await listSources();
        setSources(list);
        if (list.length > 0) setSourceId(list[0].id);
      } catch (err) {
        setError(axiosDetail(err) ?? "Не удалось загрузить данные конструктора.");
      }
    })();
  }, []);

  useEffect(() => {
    void (async () => {
      try {
        setGrFields(await listGoldenRecordFields(entityType));
      } catch (err) {
        setError(axiosDetail(err) ?? "Не удалось загрузить каталог полей Золотой записи.");
      }
    })();
  }, [entityType]);

  const loadContract = useCallback(async (id: number, type: EntityType) => {
    setError(null);
    try {
      const c = await getContract(id, type);
      setContract(c);
      setIdentifier(c?.clientIdentifierField ?? "");
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось загрузить контракт.");
      setContract(null);
    }
  }, []);

  useEffect(() => {
    if (sourceId !== null) void loadContract(sourceId, entityType);
  }, [sourceId, entityType, loadContract]);

  const saveIdentifier = async () => {
    if (sourceId === null) return;
    try {
      const c = await upsertContractMeta(sourceId, identifier.trim(), entityType);
      toast(`Контракт сохранён · версия ${c.version ?? 1}`);
      await loadContract(sourceId, entityType);
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось сохранить идентификатор.");
    }
  };

  const removeField = async (f: FieldView) => {
    if (sourceId === null || !window.confirm(`Удалить поле «${f.sourceFieldName}»?`)) return;
    try {
      await deleteField(sourceId, f.id);
      toast("Поле удалено из контракта");
      await loadContract(sourceId, entityType);
    } catch (err) {
      setError(axiosDetail(err) ?? "Не удалось удалить поле.");
    }
  };

  const fields = contract?.fields ?? [];
  const selected = sources.find((s) => s.id === sourceId) ?? null;

  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Конструктор контракта"
          sub="Сопоставление полей источника с полями Golden Record — без кода. Изменения версионируются."
          actions={
            <>
              <select
                className="field-inline"
                value={sourceId ?? ""}
                onChange={(e) => setSourceId(Number(e.target.value))}
              >
                {sources.map((s) => <option key={s.id} value={s.id}>{s.code}</option>)}
              </select>
              <select
                className="field-inline"
                value={entityType}
                onChange={(e) => setEntityType(e.target.value as EntityType)}
                title="Тип сущности: у источника может быть контракт на физлиц и на юрлиц"
              >
                <option value="PERSON">Физлица</option>
                <option value="LEGAL_ENTITY">Юрлица</option>
              </select>
              <button className="btn btn-primary btn-sm" onClick={() => setModal({ open: true, field: null })} disabled={sourceId === null}>
                + Поле
              </button>
            </>
          }
        />
        {error && <div className="errmsg" style={{ marginBottom: 16 }}>{error}</div>}

        <CardPanel icon={<Icon name="target" size={16} />} title={entityType === "PERSON" ? "Идентификация клиента в источнике" : "Идентификация юрлица в источнике"} style={{ marginBottom: 16 }}>
          <div style={{ display: "flex", gap: 12, alignItems: "flex-end", flexWrap: "wrap" }}>
            <div className="field" style={{ flex: 1, minWidth: 220 }}>
              <label>Поле-идентификатор (dot-path)</label>
              <input value={identifier} onChange={(e) => setIdentifier(e.target.value)} placeholder={entityType === "PERSON" ? "напр. client_id" : "напр. merchant_id"} />
            </div>
            <button className="btn btn-ghost" onClick={saveIdentifier} disabled={sourceId === null}>Сохранить</button>
          </div>
        </CardPanel>

        <div className="sect-h">
          <span className="t">Поля сопоставления</span>
          <span style={{ fontSize: 11, color: "var(--ink-dim)" }}>
            {fields.length} полей · контракт {selected?.code ?? "—"} · {entityType === "PERSON" ? "физлица" : "юрлица"} · версия {contract?.version ?? "—"}
          </span>
        </div>
        <div>
          {fields.map((f) => (
            <div className="frow" key={f.id}>
              <code>{f.sourceFieldName}</code>
              <span className="arrow">→</span>
              <span className="target">{f.targetGrField}</span>
              <Chip>{f.dataType}</Chip>
              {f.required && <Chip tone="warn">required</Chip>}
              {f.validationRegex && <code className="meta">{f.validationRegex}</code>}
              {f.valueMap && Object.keys(f.valueMap).length > 0 && (
                <span className="meta">{formatValueMap(f.valueMap)}</span>
              )}
              {f.sourceDateFormat && <span className="meta">{f.sourceDateFormat}</span>}
              <span className="acts">
                <button className="btn btn-ghost btn-xs" onClick={() => setModal({ open: true, field: f })}>Изменить</button>
                <button className="btn btn-danger btn-xs" onClick={() => removeField(f)}>Удалить</button>
              </span>
            </div>
          ))}
          {fields.length === 0 && <div className="sv-empty">Полей пока нет — добавьте первое кнопкой «+ Поле».</div>}
        </div>
      </div>
      {modal.open && sourceId !== null && (
        <FieldModal
          sourceId={sourceId}
          entityType={entityType}
          field={modal.field}
          grFields={grFields}
          onClose={() => setModal({ open: false, field: null })}
          onSaved={() => { setModal({ open: false, field: null }); void loadContract(sourceId, entityType); }}
        />
      )}
    </section>
  );
}
