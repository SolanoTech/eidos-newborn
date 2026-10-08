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

import { CardPanel, Icon } from "@eidos/ui-kit";

const KC_BASE =
  (import.meta as { env?: Record<string, string | undefined> }).env?.VITE_KC_URL ??
  "http://localhost:8180";

const KC_ADMIN = `${KC_BASE}/admin/master/console/#/eidos`;

/**
 * Доступ и роли после перехода на Keycloak: учётные записи, роли и
 * machine-to-machine доступ (бывшие API-ключи) управляются в Keycloak,
 * а не в консоли. Страница — навигационный хаб.
 */
export function AccessPage() {
  return (
    <>
      <div className="page-head">
        <div>
          <h1>Доступ и роли</h1>
          <p className="sub">
            Управление перенесено в Keycloak (realm <b>eidos</b>) — единый вход,
            роли и сервисные аккаунты
          </p>
        </div>
      </div>

      <div className="grid2">
        <CardPanel title="Пользователи консоли" right={<Icon name="users" />}>
          <p className="sub" style={{ marginTop: 0 }}>
            Создание, блокировка, смена пароля и назначение роли ADMIN — в разделе{" "}
            <b>Users</b> realm'а eidos.
          </p>
          <a
            className="btn btn-primary"
            href={`${KC_ADMIN}/users`}
            target="_blank"
            rel="noreferrer"
          >
            Открыть Users в Keycloak
          </a>
        </CardPanel>

        <CardPanel title="API-ключи → сервисные аккаунты" right={<Icon name="key" />}>
          <p className="sub" style={{ marginTop: 0 }}>
            Вместо таблицы ключей — OAuth2-клиенты с грантом{" "}
            <code>client_credentials</code> (пример: <b>eidos-m2m</b>). Ротация
            секрета и отзыв — в разделе <b>Clients</b>.
          </p>
          <a
            className="btn btn-primary"
            href={`${KC_ADMIN}/clients`}
            target="_blank"
            rel="noreferrer"
          >
            Открыть Clients в Keycloak
          </a>
        </CardPanel>
      </div>

      <CardPanel title="Как это работает">
        <ul className="sub" style={{ margin: 0, paddingLeft: 18, lineHeight: 1.7 }}>
          <li>Консоли входят через Keycloak (OIDC Authorization Code + PKCE).</li>
          <li>
            Сервисы проверяют подпись токена по JWKS realm'а; роль{" "}
            <code>ADMIN</code> приходит в claim <code>realm_access.roles</code>.
          </li>
          <li>
            Admin-маршруты идут через Gravitee API Gateway, который валидирует
            JWT на краю и подставляет внутренние заголовки доверия.
          </li>
        </ul>
      </CardPanel>
    </>
  );
}
