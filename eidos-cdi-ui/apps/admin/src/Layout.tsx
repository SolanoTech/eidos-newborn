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
import { AppShell } from "@eidos/ui-kit";
import { useAuth } from "@eidos/auth";
import { buildNav } from "./nav";
import { conflictCounts } from "./api/conflicts";

/** Связывает AppShell с роутером, пользователем и живым бейджем конфликтов. */
export function Layout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [conflicts, setConflicts] = useState<number | null>(null);

  useEffect(() => {
    conflictCounts().then((c) => setConflicts(c.total)).catch(() => setConflicts(null));
    const onUpdate = (e: Event) => setConflicts(Number((e as CustomEvent).detail));
    window.addEventListener("eidos:conflicts", onUpdate);
    return () => window.removeEventListener("eidos:conflicts", onUpdate);
  }, [location.pathname]);

  if (!user) return null;

  const groups = buildNav(conflicts);
  const activeKey =
    groups.flatMap((g) => g.items).find((i) => location.pathname.startsWith(i.key))?.key ?? "";

  return (
    <AppShell
      subBrand="CDI · Admin Console"
      groups={groups}
      activeKey={activeKey}
      onNavigate={(key) => navigate(key)}
      username={user.username}
      roleLabel="Администратор"
      onLogout={() => { void logout(); }}
    >
      <Outlet />
    </AppShell>
  );
}
