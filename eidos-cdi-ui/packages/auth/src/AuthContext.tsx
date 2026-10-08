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

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import {
  AUTH_EXPIRED_EVENT,
  setAccessTokenProvider,
  type User,
} from "@eidos/api-client";
import { keycloak } from "./keycloak";

interface AuthContextValue {
  user: User | null;
  initializing: boolean;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/** Пользователь из claims access-токена Keycloak. */
function userFromToken(): User | null {
  const claims = keycloak.tokenParsed as Record<string, unknown> | undefined;
  if (!claims) return null;
  return {
    username: (claims["preferred_username"] as string) ?? "user",
    role: "ADMIN",
    fullName: (claims["name"] as string) ?? undefined,
    email: (claims["email"] as string) ?? undefined,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [initializing, setInitializing] = useState(true);
  // React 18 StrictMode монтирует эффекты дважды — keycloak.init можно звать один раз.
  const initStarted = useRef(false);

  useEffect(() => {
    if (initStarted.current) return;
    initStarted.current = true;

    // Свежий токен для каждого запроса: упреждающий refresh за 30 секунд до exp.
    setAccessTokenProvider(async () => {
      if (!keycloak.authenticated) return null;
      try {
        await keycloak.updateToken(30);
      } catch {
        return null;
      }
      return keycloak.token ?? null;
    });

    keycloak
      .init({
        onLoad: "login-required",
        pkceMethod: "S256",
        checkLoginIframe: false,
      })
      .then((authenticated) => {
        setUser(authenticated ? userFromToken() : null);
      })
      .catch(() => setUser(null))
      .finally(() => setInitializing(false));
  }, []);

  // Сессия истекла/отозвана (401 из API или неудачный refresh) — на логин Keycloak.
  useEffect(() => {
    const onExpired = () => {
      setUser(null);
      void keycloak.login();
    };
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired);
  }, []);

  const logout = useCallback(async () => {
    setUser(null);
    await keycloak.logout({ redirectUri: window.location.origin });
  }, []);

  const value = useMemo(
    () => ({ user, initializing, logout }),
    [user, initializing, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within <AuthProvider>");
  }
  return ctx;
}
