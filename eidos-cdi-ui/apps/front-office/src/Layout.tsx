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
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { AppShell, Avatar, Chip } from "@eidos/ui-kit";
import { useAuth } from "@eidos/auth";
import { buildNav } from "./nav";
import { useCustomer, fio, demoBio } from "./CustomerContext";
import { activeCount, onOffersChange } from "./offersStore";

const CUSTOMER_VIEWS = ["/c360", "/nexus", "/offers", "/comms", "/chat"];

export function Layout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const { clientId, detail } = useCustomer();
  const [offersBadge, setOffersBadge] = useState(activeCount());

  useEffect(() => onOffersChange(setOffersBadge), []);

  if (!user) return null;

  const groups = buildNav(offersBadge);
  const activeKey =
    groups.flatMap((g) => g.items).find((i) => location.pathname.startsWith(i.key))?.key ?? "";
  const showCtx = CUSTOMER_VIEWS.some((v) => location.pathname.startsWith(v)) && detail !== null;
  const bio = demoBio(clientId);

  const contextBar = (
    <div className={"cust-context" + (showCtx ? " show" : "")}>
      {detail && (
        <>
          <div className="ctx-photo">
            <Avatar seed={(detail.grLastName ?? "") + (detail.grFirstName ?? "")} />
          </div>
          <div>
            <div className="ctx-name">{fio(detail) || "Без имени"}</div>
            <div className="ctx-sub">
              ПИНФЛ {detail.grPinfl ?? "—"} · {detail.grMobilePhoneMain ?? "—"}
            </div>
          </div>
          <div className="ctx-badges" style={{ marginLeft: "auto", display: "flex", gap: 8 }}>
            <span className={"ctx-chip " + (bio.ok ? "bio-on" : "bio-off")}>
              {bio.ok ? "◉ Биометрия сдана" : "○ Биометрия не сдана"}
            </span>
            <span className="ctx-chip">Golden Record · версия {detail.version ?? "—"}</span>
            <Chip tone="info">биометрия — демо</Chip>
          </div>
        </>
      )}
    </div>
  );

  return (
    <AppShell
      subBrand="Customer Data Platform"
      groups={groups}
      activeKey={activeKey}
      onNavigate={(key) => navigate(key)}
      username={user.username}
      roleLabel="CDP Operator · HUMO"
      onLogout={() => { void logout(); }}
      contextBar={contextBar}
    >
      <Outlet />
    </AppShell>
  );
}
