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

import { useEffect, useState, type FormEvent } from "react";
import { AxiosError } from "axios";
import { Toggle, ViewBar, toast } from "@eidos/ui-kit";
import { getKafkaConfig, saveKafkaConfig, type KafkaConfigInput } from "../api/kafka";

const DEFAULTS: KafkaConfigInput = {
  bootstrapServers: "",
  topic: "client-data",
  groupId: "eidos-gateway",
  enabled: true,
};

export function KafkaPage() {
  const [form, setForm] = useState<KafkaConfigInput>(DEFAULTS);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void (async () => {
      try {
        const config = await getKafkaConfig();
        if (config) {
          setForm({
            bootstrapServers: config.bootstrapServers,
            topic: config.topic,
            groupId: config.groupId,
            enabled: config.enabled,
          });
        }
      } catch (err) {
        const detail = err instanceof AxiosError
          ? (err.response?.data as { detail?: string } | undefined)?.detail
          : undefined;
        setError(detail ?? "Не удалось загрузить конфигурацию.");
      } finally {
        setLoaded(true);
      }
    })();
  }, []);

  const set = <K extends keyof KafkaConfigInput>(k: K, v: KafkaConfigInput[K]) => {
    setForm((f) => ({ ...f, [k]: v }));
    setSaved(false);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await saveKafkaConfig(form);
      setSaved(true);
      toast("Kafka: консьюмер перезапущен");
    } catch (err) {
      const detail = err instanceof AxiosError
        ? (err.response?.data as { detail?: string } | undefined)?.detail
        : undefined;
      setError(detail ?? "Не удалось сохранить конфигурацию.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="view-panel">
      <div className="wrap" style={{ maxWidth: 640 }}>
        <ViewBar title="Конфигурация Kafka" sub="Консьюмер шлюза приёма данных. Сохранение перезапускает приём." />
        {!loaded ? (
          <div className="sv-empty">Загрузка…</div>
        ) : (
          <div className="card">
            <div className="modal-b" style={{ padding: "4px 0 0" }}>
              <div className="field">
                <label>Bootstrap servers</label>
                <input value={form.bootstrapServers} onChange={(e) => set("bootstrapServers", e.target.value)} placeholder="kafka-1:9092,kafka-2:9092" />
              </div>
              <div className="frow2">
                <div className="field">
                  <label>Топик</label>
                  <input value={form.topic} onChange={(e) => set("topic", e.target.value)} />
                </div>
                <div className="field">
                  <label>Consumer group</label>
                  <input value={form.groupId} onChange={(e) => set("groupId", e.target.value)} />
                </div>
              </div>
              <div className="fcheck">
                <Toggle on={form.enabled} onChange={(v) => set("enabled", v)} />
                <span>Консьюмер включён</span>
              </div>
              {saved && <div className="okmsg">Конфигурация сохранена, консьюмер перезапущен.</div>}
              {error && <div className="errmsg">{error}</div>}
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 18 }}>
              <button className="btn btn-primary" onClick={submit} disabled={busy}>
                {busy ? "Сохранение…" : "Сохранить и перезапустить"}
              </button>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
