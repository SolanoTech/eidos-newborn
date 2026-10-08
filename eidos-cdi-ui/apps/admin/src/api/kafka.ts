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

export interface KafkaConfig {
  id: number | null;
  bootstrapServers: string;
  topic: string;
  groupId: string;
  enabled: boolean;
}

export type KafkaConfigInput = Omit<KafkaConfig, "id">;

const BASE = "/admin/kafka-config";

/** Текущая конфигурация, либо null если она ещё не задана (gateway отвечает 404). */
export async function getKafkaConfig(): Promise<KafkaConfig | null> {
  try {
    const { data } = await apiClient.get<KafkaConfig>(BASE);
    return data;
  } catch (err) {
    if (err instanceof AxiosError && err.response?.status === 404) {
      return null;
    }
    throw err;
  }
}

export async function saveKafkaConfig(
  input: KafkaConfigInput
): Promise<KafkaConfig> {
  const { data } = await apiClient.put<KafkaConfig>(BASE, input);
  return data;
}
