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

export interface GoldenRecordSummary {
  grClientId: string;
  grFirstName: string | null;
  grMiddleName: string | null;
  grLastName: string | null;
  grMobilePhoneMain: string | null;
  grPinfl: string | null;
  grBirthDate: string | null;
  grDocPassData: string | null;
}

export interface SearchPage {
  content: GoldenRecordSummary[];
  number: number;
  totalPages: number;
  totalElements: number;
}

export interface StructuredQuery {
  pinfl?: string;
  passport?: string;
  lastName?: string;
  firstName?: string;
  middleName?: string;
  phone?: string;
}

export async function structuredSearch(q: StructuredQuery, page = 1): Promise<SearchPage> {
  const { data } = await apiClient.get<SearchPage>("/admin/golden-records/search/structured", {
    params: { ...q, page },
  });
  return data;
}

export interface GoldenRecordDetail {
  grClientId: string;
  version: number | null;
  grFirstName: string | null;
  grMiddleName: string | null;
  grLastName: string | null;
  grGender: string | null;
  grBirthDate: string | null;
  grBirthPlace: string | null;
  grBirthCountry: string | null;
  grMobilePhoneMain: string | null;
  grPinfl: string | null;
  grDocPassData: string | null;
  grDocIssuedDate: string | null;
  grCitizenship: string | null;
}

export async function getGoldenRecord(clientId: string): Promise<GoldenRecordDetail> {
  const { data } = await apiClient.get<GoldenRecordDetail>(`/admin/golden-records/${clientId}`);
  return data;
}

export interface FieldMeta {
  fieldName: string;
  sourceName: string;
  trustLevel: number | null;
  updatedAt: string | null;
}

export async function getFieldMeta(clientId: string): Promise<FieldMeta[]> {
  const { data } = await apiClient.get<FieldMeta[]>(`/admin/golden-records/${clientId}/field-meta`);
  return data;
}
