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

import {
  createContext, useCallback, useContext, useEffect, useMemo, useState,
  type ReactNode,
} from "react";
import { getGoldenRecord, type GoldenRecordDetail } from "./api/customer";

interface CustomerCtx {
  clientId: string | null;
  detail: GoldenRecordDetail | null;
  select: (clientId: string) => void;
  clear: () => void;
}

const Ctx = createContext<CustomerCtx | undefined>(undefined);
const KEY = "cdp.clientId";

/** Контекст выбранного клиента: переживает перезагрузку через sessionStorage. */
export function CustomerProvider({ children }: { children: ReactNode }) {
  const [clientId, setClientId] = useState<string | null>(() => sessionStorage.getItem(KEY));
  const [detail, setDetail] = useState<GoldenRecordDetail | null>(null);

  useEffect(() => {
    let active = true;
    if (!clientId) {
      setDetail(null);
      return;
    }
    getGoldenRecord(clientId)
      .then((d) => { if (active) setDetail(d); })
      .catch(() => { if (active) setDetail(null); });
    return () => { active = false; };
  }, [clientId]);

  const select = useCallback((id: string) => {
    sessionStorage.setItem(KEY, id);
    setClientId(id);
  }, []);
  const clear = useCallback(() => {
    sessionStorage.removeItem(KEY);
    setClientId(null);
  }, []);

  const value = useMemo(() => ({ clientId, detail, select, clear }), [clientId, detail, select, clear]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useCustomer(): CustomerCtx {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useCustomer must be used within <CustomerProvider>");
  return ctx;
}

export function fio(d: GoldenRecordDetail | null): string {
  if (!d) return "";
  return [d.grLastName, d.grFirstName, d.grMiddleName].filter(Boolean).join(" ");
}

/** Детерминированный демо-признак биометрии (поля в Golden Record нет). */
export function demoBio(clientId: string | null): { ok: boolean; label: string } {
  if (!clientId) return { ok: false, label: "Не сдана" };
  let h = 0;
  for (let i = 0; i < clientId.length; i++) h = (h * 31 + clientId.charCodeAt(i)) % 97;
  const ok = h % 4 !== 2;
  return { ok, label: ok ? (h % 2 ? "Сдана · MyID Face" : "Сдана · MyID Face + отпечаток") : "Не сдана" };
}
