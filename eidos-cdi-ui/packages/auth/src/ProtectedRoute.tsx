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

import type { ReactNode } from "react";
import { useAuth } from "./AuthContext";

/**
 * Ждёт инициализацию Keycloak. Неаутентифицированного пользователя адаптер
 * сам уводит на страницу логина Keycloak (onLoad: login-required), поэтому
 * ветки «редирект на /login» здесь больше нет.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth();

  if (initializing) {
    return <div className="centered-note">Вход через Keycloak…</div>;
  }
  if (!user) {
    return <div className="centered-note">Перенаправление на вход…</div>;
  }
  return <>{children}</>;
}
