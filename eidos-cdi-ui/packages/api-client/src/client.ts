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

import axios, { AxiosError } from "axios";

const BASE_URL = "/api/v1";

/** Событие, по которому слой UI разлогинивает пользователя (токен не обновить). */
export const AUTH_EXPIRED_EVENT = "eidos:auth-expired";

export const apiClient = axios.create({ baseURL: BASE_URL });

/**
 * Поставщик access-токена. Регистрируется слоем аутентификации (@eidos/auth,
 * Keycloak): перед запросом вызывается и должен вернуть свежий токен (сам
 * заботится об упреждающем refresh) либо null, если сессии нет.
 */
type AccessTokenProvider = () => Promise<string | null>;

let tokenProvider: AccessTokenProvider | null = null;

export function setAccessTokenProvider(provider: AccessTokenProvider): void {
  tokenProvider = provider;
}

function notifyAuthExpired(): void {
  window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
}

// Запрос: берём свежий токен у провайдера (Keycloak сам обновит по TTL).
apiClient.interceptors.request.use(async (config) => {
  if (tokenProvider) {
    try {
      const token = await tokenProvider();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch {
      // Провайдер не смог обновить сессию — запрос уйдёт без токена,
      // 401 обработается ниже.
    }
  }
  return config;
});

// Ответ: 401 означает, что сессия Keycloak истекла/отозвана — сообщаем слою UI.
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      notifyAuthExpired();
    }
    return Promise.reject(error);
  }
);
