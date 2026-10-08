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

import { AxiosError } from "axios";
import { apiClient } from "@eidos/api-client";
import type { ConflictField, ResolveAction } from "./conflicts";

/**
 * Вертикаль юридических лиц: своя Золотая запись, свои маршруты. Точный поиск
 * идёт по ИНН, неточный — по названию мерчанта (триграммное сходство в core).
 */

export interface LegalEntitySummary {
  grLegalEntityId: string;
  grInn: string | null;
  grFullName: string | null;
  grShortName: string | null;
  grOpfName: string | null;
  grIsActive: boolean | null;
  grIsBankrupt: boolean | null;
}

export interface LegalEntityDetail extends LegalEntitySummary {
  version: number | null;
  grOkedCode: string | null;
  grOkedName: string | null;
  grRegistrationDate: string | null;
  grRegistrationNumber: string | null;
  grRegistrationAuthority: string | null;
  grStatutoryFund: number | null;
  grAddressFull: string | null;
  grRegionName: string | null;
  grDistrictName: string | null;
  grEmail: string | null;
  grPhones: string[] | null;
  grDirectorName: string | null;
  grFounders: Array<Record<string, unknown>> | null;
  grFoundersCount: number | null;
  grTaxMode: number | null;
  grVatNumber: string | null;
  grIsVatPayer: boolean | null;
  grTrustRating: string | null;
  grTrustScore: number | null;
  grIsSmallBusiness: boolean | null;
  grActivityStateName: string | null;
}

export interface LegalSearchPage {
  content: LegalEntitySummary[];
  number: number;
  totalPages: number;
  totalElements: number;
}

/** Точный поиск по ИНН: 404 от core означает «не найдено», а не ошибку. */
export async function searchByInn(inn: string): Promise<LegalEntityDetail | null> {
  try {
    const { data } = await apiClient.get<LegalEntityDetail>("/admin/legal-records/search/inn", {
      params: { inn },
    });
    return data;
  } catch (err) {
    if (err instanceof AxiosError && err.response?.status === 404) return null;
    throw err;
  }
}

/** Неточный поиск по названию мерчанта. */
export async function searchByName(query: string, page = 1): Promise<LegalSearchPage> {
  const { data } = await apiClient.get<LegalSearchPage>("/admin/legal-records/search/name", {
    params: { query, page },
  });
  return data;
}

export async function getLegalEntity(id: string): Promise<LegalEntityDetail> {
  const { data } = await apiClient.get<LegalEntityDetail>(`/admin/legal-records/${id}`);
  return data;
}

export interface FieldMeta {
  fieldName: string;
  sourceName: string;
  trustLevel: number | null;
  updatedAt: string | null;
}

export async function getLegalFieldMeta(id: string): Promise<FieldMeta[]> {
  const { data } = await apiClient.get<FieldMeta[]>(`/admin/legal-records/${id}/field-meta`);
  return data;
}

export interface ExternalId {
  sourceName: string;
  externalId: string;
  active: boolean;
  createdAt: string | null;
}

export async function getLegalExternalIds(id: string): Promise<ExternalId[]> {
  const { data } = await apiClient.get<ExternalId[]>(`/admin/legal-records/${id}/external-ids`);
  return data;
}

// ---------- Очередь конфликтов юрлиц ----------

export interface LegalConflictItem {
  id: number;
  reason: "GREY_ZONE_CONFLICT" | "UNKNOWN_SOURCE";
  grLegalEntityId: string | null;
  sourceName: string | null;
  sourceTrust: number | null;
  merchantName: string | null;
  inn: string | null;
  createdAt: string | null;
  conflicts: ConflictField[];
  snapshot: Record<string, string>;
}

export interface LegalConflictPage {
  content: LegalConflictItem[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export interface ConflictCounts {
  total: number;
  greyZone: number;
  unknownSource: number;
}

export async function listLegalConflicts(page = 1): Promise<LegalConflictPage> {
  const { data } = await apiClient.get<LegalConflictPage>("/admin/conflicts-legal", {
    params: { page },
  });
  return data;
}

export async function legalConflictCounts(): Promise<ConflictCounts> {
  const { data } = await apiClient.get<ConflictCounts>("/admin/conflicts-legal/count");
  return data;
}

export async function resolveLegalConflict(id: number, action: ResolveAction): Promise<void> {
  await apiClient.post(`/admin/conflicts-legal/${id}/resolve`, { action });
}
