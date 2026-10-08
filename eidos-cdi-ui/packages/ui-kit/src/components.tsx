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

// Мелкие переиспользуемые элементы дизайн-системы консоли.
import type { CSSProperties, ReactNode } from "react";

/** Чип-бейдж: tone = gold | ok | warn | bad | info | undefined (нейтральный). */
export function Chip({ tone, children }: { tone?: "gold" | "ok" | "warn" | "bad" | "info"; children: ReactNode }) {
  return <span className={"chip" + (tone ? " " + tone : "")}>{children}</span>;
}

/** Статус с точкой: on | off | warn | bad. */
export function St({ tone, children }: { tone: "on" | "off" | "warn" | "bad"; children: ReactNode }) {
  return <span className={"st " + tone}>{children}</span>;
}

/** Полоса доверия источника (1–10). */
export function TrustBar({ value }: { value: number }) {
  const v = Math.max(0, Math.min(10, value ?? 0));
  return (
    <span className="trust">
      <i>
        <b style={{ width: `${v * 10}%` }} />
      </i>
      <span className="tv">{v}/10</span>
    </span>
  );
}

/** Переключатель-тумблер. */
export function Toggle({ on, onChange }: { on: boolean; onChange?: (next: boolean) => void }) {
  return (
    <div
      className={"toggle" + (on ? " on" : "")}
      role="switch"
      aria-checked={on}
      onClick={() => onChange?.(!on)}
    />
  );
}

/** Плитка-метрика дашборда. */
export function Tile({ label, value, unit, note }: { label: string; value: ReactNode; unit?: string; note?: ReactNode }) {
  return (
    <div className="tile">
      <div className="tl">{label}</div>
      <div className="tn">
        {value}
        {unit && <span className="tu">{unit}</span>}
      </div>
      {note && <div className="td">{note}</div>}
    </div>
  );
}

/** Карточка с заголовком (иконка + подпись + правый хвост). */
export function CardPanel({
  icon,
  title,
  right,
  children,
  style,
}: {
  icon?: ReactNode;
  title?: string;
  right?: ReactNode;
  children: ReactNode;
  style?: CSSProperties;
}) {
  return (
    <div className="card" style={style}>
      {(icon || title || right) && (
        <div className="card-h">
          {icon && <span className="ic">{icon}</span>}
          {title && <span className="ttl">{title}</span>}
          {right && <span className="right">{right}</span>}
        </div>
      )}
      {children}
    </div>
  );
}

/** Метрика с прогресс-баром (качество данных). */
export function SegItem({ label, pct, tone, valueText }: { label: string; pct: number; tone?: "g" | "w" | "b"; valueText?: string }) {
  const width = Math.max(0, Math.min(100, pct));
  return (
    <div className="seg-item">
      <div className="seg-row">
        <span>{label}</span>
        <span className="pc">{valueText ?? `${pct}%`}</span>
      </div>
      <div className="bar">
        <i className={tone ?? ""} style={{ width: `${width}%` }} />
      </div>
    </div>
  );
}

/** Заголовок раздела (view-bar из макета). */
export function ViewBar({ title, sub, actions }: { title: string; sub?: string; actions?: ReactNode }) {
  return (
    <div className="view-bar">
      <div>
        <div className="vb-title">{title}</div>
        {sub && <div className="vb-sub">{sub}</div>}
      </div>
      {actions && <div className="vb-actions">{actions}</div>}
    </div>
  );
}

/** Детерминированный градиентный аватар по seed (как в макете). */
function hashHue(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 360;
  return h;
}

let avatarSeq = 0;

export function Avatar({ seed }: { seed: string }) {
  const hue = hashHue(seed);
  const id = `pg${++avatarSeq}`;
  return (
    <svg viewBox="0 0 100 100" preserveAspectRatio="xMidYMid slice" width="100%" height="100%">
      <defs>
        <linearGradient id={id} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={`hsl(${hue},30%,64%)`} />
          <stop offset="1" stopColor={`hsl(${(hue + 26) % 360},34%,42%)`} />
        </linearGradient>
      </defs>
      <rect width="100" height="100" fill={`url(#${id})`} />
      <circle cx="50" cy="39" r="15.5" fill="rgba(255,255,255,.92)" />
      <path d="M23 90c0-16 12-26 27-26s27 10 27 26z" fill="rgba(255,255,255,.92)" />
    </svg>
  );
}

/** Мини-капля-логотип EIDOS. */
export function LogoDrop({ onClick }: { onClick?: () => void }) {
  return (
    <div className="logo-drop" role="img" aria-label="EIDOS" onClick={onClick}>
      <div className="logo-glass" />
      <span className="logo-hi" />
    </div>
  );
}

/** Словесный знак EIDOS с подзаголовком. */
export function Wordmark({ sub }: { sub: string }) {
  return (
    <div className="wordmark">
      <span className="nm">EIDOS</span>
      <span className="sub">{sub}</span>
    </div>
  );
}
