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

import Keycloak from "keycloak-js";

/**
 * Единственный экземпляр адаптера Keycloak на приложение.
 *
 * Параметры можно переопределить через Vite-переменные (VITE_KC_URL,
 * VITE_KC_REALM, VITE_KC_CLIENT_ID); по умолчанию — локальный Keycloak
 * docker-compose (realm eidos, публичный клиент eidos-console, PKCE S256).
 */
const env = (import.meta as { env?: Record<string, string | undefined> }).env ?? {};

export const keycloak = new Keycloak({
  url: env.VITE_KC_URL ?? "http://localhost:8180",
  realm: env.VITE_KC_REALM ?? "eidos",
  clientId: env.VITE_KC_CLIENT_ID ?? "eidos-console",
});
