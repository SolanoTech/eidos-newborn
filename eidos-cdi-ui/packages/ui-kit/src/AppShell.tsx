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

import { useEffect, useState, type ReactNode } from "react";
import { Icon, type IconName } from "./Icon";
import { LogoDrop, Wordmark } from "./components";
import { ToastHost, toast } from "./toast";
import { isSoundOn, setSoundOn, primeAudio, rustle } from "./audio";

export interface ShellNavItem {
  key: string;
  label: string;
  icon: IconName;
  badge?: string | number | null;
}

export interface ShellNavGroup {
  caption?: string;
  items: ShellNavItem[];
}

const THEME_KEY = "eidos.theme";

function applyTheme(theme: string): void {
  document.documentElement.setAttribute("data-theme", theme);
}

export function getInitialTheme(): string {
  return localStorage.getItem(THEME_KEY) ?? "light";
}

/**
 * Каркас консоли из макета: верхняя панель (логотип, звук, тема, выход,
 * пользователь) + сворачиваемый rail с группами и бейджами + область раздела.
 * Не зависит от роутинга: навигация и пользователь передаются пропсами.
 */
export function AppShell({
  subBrand,
  groups,
  activeKey,
  onNavigate,
  username,
  roleLabel,
  onLogout,
  contextBar,
  children,
}: {
  subBrand: string;
  groups: ShellNavGroup[];
  activeKey: string;
  onNavigate: (key: string) => void;
  username: string;
  roleLabel: string;
  onLogout: () => void;
  /** Опциональная полоса контекста (например, выбранный клиент) над рабочей областью. */
  contextBar?: ReactNode;
  children: ReactNode;
}) {
  const [theme, setTheme] = useState(getInitialTheme);
  const [sound, setSound] = useState(isSoundOn());

  useEffect(() => {
    applyTheme(theme);
    localStorage.setItem(THEME_KEY, theme);
  }, [theme]);

  // Разблокировка Web Audio первым жестом.
  useEffect(() => {
    const prime = () => primeAudio();
    window.addEventListener("pointerdown", prime, { once: true });
    return () => window.removeEventListener("pointerdown", prime);
  }, []);

  // Пасхалка: 5 быстрых кликов по логотипу — шум леса.
  const [logoClicks, setLogoClicks] = useState(0);
  useEffect(() => {
    if (logoClicks === 0) return;
    if (logoClicks >= 5) {
      setLogoClicks(0);
      primeAudio();
      rustle();
      toast("🌿 Пасхалка найдена · лес EIDOS шумит листвой");
      return;
    }
    const t = window.setTimeout(() => setLogoClicks(0), 1600);
    return () => window.clearTimeout(t);
  }, [logoClicks]);

  const initials = username
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p.charAt(0).toUpperCase())
    .join("") || "?";

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <LogoDrop onClick={() => setLogoClicks((n) => n + 1)} />
          <Wordmark sub={subBrand} />
        </div>
        <div className="spacer" />
        <div className="topright">
          <button
            className={"icon-btn" + (sound ? "" : " muted")}
            aria-label="Звук поющих чаш"
            title="Звук поющих чаш"
            aria-pressed={sound}
            onClick={() => {
              const next = !sound;
              setSound(next);
              setSoundOn(next);
              if (next) primeAudio();
              toast(next ? "Звук поющих чаш включён" : "Звук выключен");
            }}
          >
            <Icon name={sound ? "sndOn" : "sndOff"} size={17} />
          </button>
          <button
            className="icon-btn"
            aria-label="Сменить тему"
            title="Светлая / тёмная тема"
            onClick={() => {
              const next = theme === "dark" ? "light" : "dark";
              setTheme(next);
              toast(next === "dark" ? "Тёмная тема включена" : "Светлая тема включена");
            }}
          >
            <Icon name={theme === "dark" ? "sun" : "moon"} size={17} />
          </button>
          <button className="icon-btn" aria-label="Выйти" title="Выйти" onClick={onLogout}>
            <Icon name="logout" size={16} />
          </button>
          <div className="usr">
            <div className="av">{initials}</div>
            <div>
              <div className="nm">{username}</div>
              <div className="rl">{roleLabel}</div>
            </div>
          </div>
        </div>
      </header>

      <div className="body">
        <nav className="rail" aria-label="Разделы консоли">
          <div className="rail-inner">
            {groups.map((group, gi) => (
              <div key={gi}>
                {gi > 0 && <div className="rail-sep" />}
                {group.caption && <div className="rail-cap">{group.caption}</div>}
                <div className="rail-group">
                  {group.items.map((item) => (
                    <div
                      key={item.key}
                      className={"rail-item" + (item.key === activeKey ? " active" : "")}
                      title={item.label}
                      onClick={() => onNavigate(item.key)}
                    >
                      <span className="rail-ic">
                        <Icon name={item.icon} size={20} />
                      </span>
                      <span className="rail-label">{item.label}</span>
                      {item.badge != null && item.badge !== 0 && (
                        <span className="rail-badge">{item.badge}</span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </nav>

        <main className="main">
          {contextBar}
          <div className="stage">{children}</div>
        </main>
      </div>
      <ToastHost />
    </div>
  );
}
