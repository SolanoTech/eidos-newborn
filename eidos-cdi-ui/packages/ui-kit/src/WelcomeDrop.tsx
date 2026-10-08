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

import { useEffect, useRef, useState, type ReactNode } from "react";
import { primeAudio, strikeBowl, storm } from "./audio";
import { toast } from "./toast";

/**
 * Приветственный экран консоли: живая стеклянная капля с параллаксом,
 * поющими чашами при наведении и пасхалкой-грозой (10 кликов по экрану).
 */
export function WelcomeDrop({
  title,
  sub,
  hint,
  cta,
}: {
  title: string;
  sub: string;
  hint: string;
  cta?: ReactNode;
}) {
  const rootRef = useRef<HTMLDivElement>(null);
  const dropRef = useRef<HTMLDivElement>(null);
  const wolfRef = useRef<HTMLDivElement>(null);
  const [clicks, setClicks] = useState(0);

  // Параллакс капли за курсором.
  useEffect(() => {
    const root = rootRef.current;
    const drop = dropRef.current;
    if (!root || !drop) return;
    let tx = 0, ty = 0, cx = 0, cy = 0, raf = 0;
    const onMove = (e: PointerEvent) => {
      const r = root.getBoundingClientRect();
      if (!r.width) return;
      const nx = (e.clientX - (r.left + r.width / 2)) / (r.width / 2);
      const ny = (e.clientY - (r.top + r.height / 2)) / (r.height / 2);
      tx = Math.max(-1, Math.min(1, nx)) * 16;
      ty = Math.max(-1, Math.min(1, ny)) * 16;
    };
    const loop = () => {
      cx += (tx - cx) * 0.06;
      cy += (ty - cy) * 0.06;
      drop.style.setProperty("--px", cx.toFixed(1) + "px");
      drop.style.setProperty("--py", cy.toFixed(1) + "px");
      raf = requestAnimationFrame(loop);
    };
    window.addEventListener("pointermove", onMove, { passive: true });
    raf = requestAnimationFrame(loop);
    return () => {
      window.removeEventListener("pointermove", onMove);
      cancelAnimationFrame(raf);
    };
  }, []);

  // Пасхалка: 10 кликов по экрану — гроза + тень волка Тенгри.
  useEffect(() => {
    if (clicks === 0) return;
    if (clicks > 9) {
      setClicks(0);
      primeAudio();
      if (storm()) {
        toast("⛈ Пасхалка: гроза над лесом EIDOS · хвойный лес шумит три минуты");
        const wolf = wolfRef.current;
        if (wolf) {
          window.setTimeout(() => {
            wolf.classList.remove("run");
            void wolf.offsetWidth;
            wolf.classList.add("run");
          }, 1400);
        }
      }
      return;
    }
    const t = window.setTimeout(() => setClicks(0), 2500);
    return () => window.clearTimeout(t);
  }, [clicks]);

  const sing = () => {
    primeAudio();
    if (strikeBowl()) {
      const el = dropRef.current;
      if (el) {
        el.classList.add("sing");
        window.setTimeout(() => el.classList.remove("sing"), 2200);
      }
    }
  };

  return (
    <div className="empty" ref={rootRef} onClick={() => setClicks((n) => n + 1)}>
      <svg width="0" height="0" aria-hidden="true" style={{ position: "absolute", width: 0, height: 0, pointerEvents: "none" }}>
        <defs>
          <filter id="ripple" x="-30%" y="-30%" width="160%" height="160%" colorInterpolationFilters="sRGB">
            <feTurbulence type="fractalNoise" baseFrequency="0.018 0.028" numOctaves={2} seed={6} result="t">
              <animate attributeName="baseFrequency" dur="10s" values="0.018 0.028;0.026 0.020;0.020 0.030;0.018 0.028" repeatCount="indefinite" />
            </feTurbulence>
            <feDisplacementMap in="SourceGraphic" in2="t" scale={16} xChannelSelector="R" yChannelSelector="G" />
          </filter>
        </defs>
      </svg>

      <div className="drop" ref={dropRef} aria-hidden="true" onPointerEnter={sing} onClick={sing}>
        <div className="halo" />
        <div className="drop-inner">
          <div className="drop-glass" />
          <div className="caustic" />
          <span className="sheen" />
          <span className="spec spec-a" />
          <span className="spec spec-b" />
        </div>
      </div>

      <div className="em-content">
        <div className="em-title">{title}</div>
        <div className="em-sub">{sub}</div>
        <div className="em-hint">{hint}</div>
        {cta && <div className="em-cta">{cta}</div>}
      </div>

      <div className="wolf-shadow" ref={wolfRef} aria-hidden="true">
        <svg viewBox="0 0 240 120" width="100%" height="100%">
          <g fill="currentColor">
            <path d="M232 52 L218 44 L208 27 L201 36 L194 25 L184 38 L150 36 L112 40 L88 38 L70 26 L48 14 L40 18 L56 32 L68 44 L78 52 L64 70 L34 92 L24 100 L32 103 L58 84 L76 68 L92 62 L120 60 L142 62 L168 78 L192 98 L200 106 L207 101 L184 78 L168 62 L184 58 L200 56 L214 58 Z" />
            <path d="M96 60 L78 84 L70 100 L77 102 L90 82 L102 64 Z" opacity=".85" />
            <path d="M150 62 L160 84 L170 100 L177 97 L164 78 L158 62 Z" opacity=".85" />
          </g>
        </svg>
      </div>
    </div>
  );
}
