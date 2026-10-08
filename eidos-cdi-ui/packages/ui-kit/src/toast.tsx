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

// Глобальный тост консоли: вызов toast("…") из любого места, хост — один на
// приложение. Общение через CustomEvent, чтобы не тянуть контекст.
import { useEffect, useRef, useState } from "react";

const TOAST_EVENT = "eidos:toast";

export function toast(message: string): void {
  window.dispatchEvent(new CustomEvent(TOAST_EVENT, { detail: message }));
}

export function ToastHost() {
  const [message, setMessage] = useState("");
  const [show, setShow] = useState(false);
  const timer = useRef<number | undefined>(undefined);

  useEffect(() => {
    const onToast = (e: Event) => {
      setMessage(String((e as CustomEvent).detail ?? ""));
      setShow(true);
      window.clearTimeout(timer.current);
      timer.current = window.setTimeout(() => setShow(false), 2400);
    };
    window.addEventListener(TOAST_EVENT, onToast);
    return () => {
      window.removeEventListener(TOAST_EVENT, onToast);
      window.clearTimeout(timer.current);
    };
  }, []);

  return (
    <div className={"toast" + (show ? " show" : "")} role="status">
      <span className="ti">◆</span>
      <span>{message}</span>
    </div>
  );
}
