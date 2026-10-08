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

import { useEffect, useRef, useState } from "react";
import { Chip } from "@eidos/ui-kit";
import { useCustomer, fio } from "../CustomerContext";

// ДЕМО-раздел: ассистент отвечает заготовленными репликами (LLM-интеграция —
// отдельным шагом).
interface Msg {
  role: "ai" | "me";
  html: string;
}

const REPLIES: Record<string, string> = {
  "сформулируй текст оффера":
    "Вариант push-сообщения:<br><br>«Для вас — <b>премиальный депозит под 24%</b> с ежемесячной капитализацией. Только для статуса Champions. Оформить за 1 минуту в приложении →»<div class=\"insight\">Тон: персональный, без давления. Лучшее окно отправки — сегодня 19:00–21:00.</div>",
  "почему низкий риск оттока?":
    "Риск оттока <b>7%</b>: высокая частота транзакций (F4), растущий депозит (+12% за квартал), активный цифровой канал (88%). Сигналов недовольства нет.",
  "лучшее время для контакта?":
    "Оптимальное окно — <b>будни, 19:00–21:00</b>, канал <b>Push</b>. Open-rate 64% против 22% утром.",
};
const SUGGESTS = ["Сформулируй текст оффера", "Почему низкий риск оттока?", "Лучшее время для контакта?"];

export function ChatPage() {
  const { detail } = useCustomer();
  const name = fio(detail) || "клиента";
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [typing, setTyping] = useState(false);
  const [text, setText] = useState("");
  const logRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMsgs([
      {
        role: "ai",
        html: `Профиль <b>${name}</b> загружен. Champions-сегмент, низкий риск оттока (7%), высокая склонность к сберегательным продуктам.<div class="insight">💡 Рекомендация: предложить <b>Премиальный депозит 24%</b> (propensity 88%) через push в окне 19:00–21:00 — предпочтительный канал и время.</div>`,
      },
      { role: "ai", html: "Чем помочь по этому клиенту?" },
    ]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [name]);

  useEffect(() => {
    logRef.current?.scrollTo({ top: logRef.current.scrollHeight });
  }, [msgs, typing]);

  const send = (raw?: string) => {
    const t = (raw ?? text).trim();
    if (!t) return;
    setMsgs((m) => [...m, { role: "me", html: t.replace(/</g, "&lt;") }]);
    setText("");
    setTyping(true);
    window.setTimeout(() => {
      setTyping(false);
      const reply = REPLIES[t.toLowerCase()]
        ?? "Анализирую профиль… Рекомендую сфокусироваться на сберегательных продуктах: сегмент Champions с высокой склонностью к депозитам и низкой ценовой чувствительностью.";
      setMsgs((m) => [...m, { role: "ai", html: reply }]);
    }, 800);
  };

  return (
    <section className="view-panel" data-view="chat" style={{ display: "flex", flexDirection: "column", paddingBottom: 24 }}>
      <div className="view-bar" style={{ maxWidth: 900, width: "100%", margin: "0 auto 18px" }}>
        <div>
          <div className="vb-title">AI-чат с клиентом</div>
          <div className="vb-sub">Ассистент EIDOS видит весь профиль и помогает вести диалог и подбирать действия.</div>
        </div>
        <div className="vb-actions"><Chip tone="info">демо</Chip></div>
      </div>
      <div className="chat">
        <div className="chat-head">
          <div className="chat-ic">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2">
              <path d="M12 2a7 7 0 0 1 7 7c0 5-7 13-7 13S5 14 5 9a7 7 0 0 1 7-7Z" /><circle cx="12" cy="9" r="2.4" />
            </svg>
          </div>
          <div>
            <div className="cn">EIDOS Assistant</div>
            <div className="cs">контекст клиента загружен · 360° профиль</div>
          </div>
        </div>
        <div className="chat-log" ref={logRef}>
          {msgs.map((m, i) => (
            <div className={"msg " + m.role} key={i}>
              <div className="mav">{m.role === "ai" ? "AI" : "Я"}</div>
              <div className="bubble" dangerouslySetInnerHTML={{ __html: m.html }} />
            </div>
          ))}
          {msgs.length === 2 && !typing && (
            <div className="msg ai">
              <div className="mav">AI</div>
              <div className="bubble">
                <div className="suggests">
                  {SUGGESTS.map((s) => (
                    <button className="sug" key={s} onClick={() => send(s)}>{s}</button>
                  ))}
                </div>
              </div>
            </div>
          )}
          {typing && (
            <div className="msg ai">
              <div className="mav">AI</div>
              <div className="bubble"><div className="typing"><i /><i /><i /></div></div>
            </div>
          )}
        </div>
        <div className="chat-input">
          <input
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") send(); }}
            placeholder="Спросите ассистента о клиенте…"
            autoComplete="off"
          />
          <button className="chat-send" onClick={() => send()} aria-label="Отправить">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4z" />
            </svg>
          </button>
        </div>
      </div>
    </section>
  );
}
