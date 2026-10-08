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

export type FieldDataType =
  | "STRING" | "DATE" | "INTEGER" | "BOOLEAN" | "GENDER"
  /** Массив простых значений — поддерево JSON переносится как есть. */
  | "ARRAY"
  /** Массив структур (например, учредители юрлица). */
  | "STRUCT";

/**
 * Тип сущности контракта. Источники его не присылают — он определяется каналом
 * приёма; в конструкторе он выбирается вручную, чтобы задать, какие целевые
 * поля Золотой записи доступны.
 */
export type EntityType = "PERSON" | "LEGAL_ENTITY";

export interface FieldView {
  id: number;
  sourceFieldName: string;
  targetGrField: string;
  dataType: FieldDataType;
  required: boolean | null;
  sourceDateFormat: string | null;
  validationRegex: string | null;
  defaultValue: string | null;
  ordering: number | null;
  valueMap: Record<string, string> | null;
}

export interface ContractView {
  id: number;
  sourceId: number;
  sourceCode: string;
  entityType?: EntityType;
  clientIdentifierField: string | null;
  version: number | null;
  fields: FieldView[];
}

export interface FieldInput {
  sourceFieldName: string;
  targetGrField: string;
  dataType: FieldDataType;
  required: boolean;
  sourceDateFormat?: string | null;
  validationRegex?: string | null;
  defaultValue?: string | null;
  valueMap?: Record<string, string>;
}

export interface GoldenRecordField {
  jsonName: string;
  javaName: string;
  type: string;
  required: boolean;
  pattern: string | null;
}

/** Контракт источника, либо null если он ещё не создан (stage отвечает 404). */
export async function getContract(
  sourceId: number,
  entityType: EntityType = "PERSON"
): Promise<ContractView | null> {
  try {
    const { data } = await apiClient.get<ContractView>(
      `/admin/sources/${sourceId}/contract`,
      { params: { entityType } }
    );
    return data;
  } catch (err) {
    if (err instanceof AxiosError && err.response?.status === 404) {
      return null;
    }
    throw err;
  }
}

export async function upsertContractMeta(
  sourceId: number,
  clientIdentifierField: string,
  entityType: EntityType = "PERSON"
): Promise<ContractView> {
  const { data } = await apiClient.put<ContractView>(
    `/admin/sources/${sourceId}/contract`,
    { clientIdentifierField },
    { params: { entityType } }
  );
  return data;
}

export async function addField(
  sourceId: number,
  input: FieldInput,
  entityType: EntityType = "PERSON"
): Promise<FieldView> {
  const { data } = await apiClient.post<FieldView>(
    `/admin/sources/${sourceId}/contract/fields`,
    input,
    { params: { entityType } }
  );
  return data;
}

export async function updateField(
  sourceId: number,
  fieldId: number,
  input: FieldInput
): Promise<FieldView> {
  const { data } = await apiClient.put<FieldView>(
    `/admin/sources/${sourceId}/contract/fields/${fieldId}`,
    input
  );
  return data;
}

export async function deleteField(
  sourceId: number,
  fieldId: number
): Promise<void> {
  await apiClient.delete(`/admin/sources/${sourceId}/contract/fields/${fieldId}`);
}

export async function listGoldenRecordFields(
  entityType: EntityType = "PERSON"
): Promise<GoldenRecordField[]> {
  const { data } = await apiClient.get<GoldenRecordField[]>(
    "/admin/golden-record-fields",
    { params: { entityType } }
  );
  return data;
}
