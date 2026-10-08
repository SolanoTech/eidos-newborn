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

/** Группы навигации консоли (как в макете). Бейдж конфликтов подставляется в Layout. */
export function buildNav(conflictCount: number | null): ShellNavGroup[] {
  return [
    {
      items: [{ key: "/dash", label: "Дашборд", icon: "dashboard" }],
    },
    {
      caption: "Данные",
      items: [
        { key: "/sources", label: "Источники", icon: "database" },
        { key: "/constructor", label: "Конструктор контракта", icon: "sliders" },
        { key: "/schema", label: "Схема событий", icon: "stack" },
        { key: "/kafka", label: "Kafka шлюза", icon: "kafka" },
      ],
    },
    {
      caption: "Качество",
      items: [
        { key: "/conflicts", label: "Очередь конфликтов", icon: "alert", badge: conflictCount },
        { key: "/quality", label: "Качество данных", icon: "shield" },
      ],
    },
    {
      caption: "Клиенты",
      items: [{ key: "/search", label: "Поиск Golden Records", icon: "search" }],
    },
    {
      caption: "Юридические лица",
      items: [{ key: "/legal-search", label: "Поиск юрлиц", icon: "building" }],
    },
    {
      caption: "Управление",
      items: [
        { key: "/consent", label: "Согласия и контакты", icon: "consent" },
        { key: "/integrations", label: "Интеграции и выгрузки", icon: "link" },
        { key: "/access", label: "Доступы и API", icon: "key" },
        { key: "/audit", label: "Аудит-лог", icon: "clock" },
        { key: "/privacy", label: "Приватность · GDPR", icon: "lock" },
      ],
    },
  ];
}
