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

// Демо-хранилище офферов (модуль Temptation не реализован): состояние живёт
// в памяти вкладки, события синхронизируют бейдж в навигации.
export interface Offer {
  id: number;
  name: string;
  type: string;
  icon: "star" | "card" | "split" | "check" | "gift";
  desc: string;
  prop: number;
  channels: string[];
  status: "nbo" | "draft" | "accepted" | "revoked" | "active";
  rank?: string;
}

let seq = 100;
let offers: Offer[] = [
  { id: 1, name: "Премиальный депозит 24%", type: "Сберегательный продукт", icon: "star", desc: "Повышенная ставка для Champions-сегмента. Минимум ₽ 50 млн, капитализация ежемесячно.", prop: 88, channels: ["Push", "App-баннер"], status: "nbo", rank: "NBO #1" },
  { id: 2, name: "Premium HUMO Metal", type: "Карточный апгрейд", icon: "card", desc: "Бесплатный выпуск металлической карты с кешбэком 3% и lounge-доступом.", prop: 73, channels: ["Push", "SMS"], status: "nbo", rank: "NBO #2" },
  { id: 3, name: "Авто-рассрочка 0-0-12", type: "Кредитный продукт", icon: "split", desc: "Беспроцентная рассрочка на 12 месяцев для покупателей категории «авто».", prop: 41, channels: ["E-mail"], status: "draft" },
  { id: 4, name: "Cashback «Лето» 5%", type: "Транзакционный", icon: "check", desc: "Сезонный кешбэк по категории «рестораны». Принят клиентом 18.06, активен до 31.08.", prop: 100, channels: ["Push"], status: "accepted" },
];

const EVT = "cdp:offers";

function emit(): void {
  window.dispatchEvent(new CustomEvent(EVT, { detail: activeCount() }));
}

export function listOffers(): Offer[] {
  return [...offers];
}

export function activeCount(): number {
  return offers.filter((o) => o.status !== "revoked").length;
}

export function addOffer(o: Omit<Offer, "id">): Offer {
  const created = { ...o, id: ++seq };
  offers = [created, ...offers];
  emit();
  return created;
}

export function setStatus(id: number, status: Offer["status"]): Offer | undefined {
  const o = offers.find((x) => x.id === id);
  if (o) {
    o.status = status;
    emit();
  }
  return o;
}

export function onOffersChange(fn: (count: number) => void): () => void {
  const h = (e: Event) => fn(Number((e as CustomEvent).detail));
  window.addEventListener(EVT, h);
  return () => window.removeEventListener(EVT, h);
}
