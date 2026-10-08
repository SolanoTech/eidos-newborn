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

import type { ShellNavGroup } from "@eidos/ui-kit";

export function buildNav(offersBadge: number | null): ShellNavGroup[] {
  return [
    {
      items: [{ key: "/search", label: "Поиск клиента и мерчанта", icon: "search" }],
    },
    {
      caption: "Карточка клиента",
      items: [
        { key: "/c360", label: "Customer 360", icon: "target" },
        { key: "/nexus", label: "Связи", icon: "link" },
        { key: "/offers", label: "Офферы клиента", icon: "consent", badge: offersBadge },
        { key: "/comms", label: "Коммуникации", icon: "id" },
        { key: "/chat", label: "AI-чат с клиентом", icon: "users" },
      ],
    },
  ];
}
