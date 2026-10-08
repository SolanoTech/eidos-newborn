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

import { apiClient } from "@eidos/api-client";

export interface ConflictField {
  fieldName: string;
  currentValue: string | null;
  incomingValue: string | null;
  currentSource: string | null;
  currentTrust: number | null;
}

export interface ConflictItem {
  id: number;
  reason: "GREY_ZONE_CONFLICT" | "UNKNOWN_SOURCE";
  grClientId: string | null;
  sourceName: string | null;
  sourceTrust: number | null;
  personName: string | null;
  createdAt: string | null;
  conflicts: ConflictField[];
  snapshot: Record<string, string>;
}

export interface ConflictPage {
  content: ConflictItem[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export interface ConflictCounts {
  total: number;
  greyZone: number;
  unknownSource: number;
}

export async function listConflicts(page = 1): Promise<ConflictPage> {
  const { data } = await apiClient.get<ConflictPage>("/admin/conflicts", { params: { page } });
  return data;
}

export async function conflictCounts(): Promise<ConflictCounts> {
  const { data } = await apiClient.get<ConflictCounts>("/admin/conflicts/count");
  return data;
}

export type ResolveAction = "KEEP_CURRENT" | "ACCEPT_INCOMING" | "REJECT";

export async function resolveConflict(id: number, action: ResolveAction): Promise<void> {
  await apiClient.post(`/admin/conflicts/${id}/resolve`, { action });
}
