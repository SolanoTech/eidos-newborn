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

// Звук консоли: поющие чаши, шум леса и пасхалка-гроза (Web Audio API).
// Портировано из HTML-макета. Все звуки уважают глобальный флаг soundOn.

let ctx: AudioContext | null = null;
let master: GainNode | null = null;
let soundOn = true;
let lastStrike = 0;
let stormOn = false;
let stormStopper: (() => void) | null = null;

export function isSoundOn(): boolean {
  return soundOn;
}

export function setSoundOn(on: boolean): void {
  soundOn = on;
  if (!on && stormStopper) stormStopper();
}

function makeImpulse(dur: number, decay: number): AudioBuffer {
  const c = ctx!;
  const rate = c.sampleRate;
  const len = Math.floor(rate * dur);
  const buf = c.createBuffer(2, len, rate);
  for (let ch = 0; ch < 2; ch++) {
    const d = buf.getChannelData(ch);
    for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, decay);
  }
  return buf;
}

function flatNoise(dur: number): AudioBuffer {
  const c = ctx!;
  const rate = c.sampleRate;
  const len = Math.floor(rate * dur);
  const buf = c.createBuffer(2, len, rate);
  for (let ch = 0; ch < 2; ch++) {
    const d = buf.getChannelData(ch);
    for (let i = 0; i < len; i++) d[i] = Math.random() * 2 - 1;
  }
  return buf;
}

function ensure(): void {
  if (ctx) return;
  const AC = window.AudioContext ?? (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
  ctx = new AC();
  master = ctx.createGain();
  master.gain.value = 0.85;
  const dry = ctx.createGain();
  dry.gain.value = 0.7;
  const conv = ctx.createConvolver();
  conv.buffer = makeImpulse(3.4, 2.4);
  const wet = ctx.createGain();
  wet.gain.value = 0.55;
  master.connect(dry).connect(ctx.destination);
  master.connect(conv);
  conv.connect(wet).connect(ctx.destination);
}

function resume(): void {
  if (ctx && ctx.state === "suspended") void ctx.resume();
}

/** Разблокировка аудио первым жестом пользователя. */
export function primeAudio(): void {
  try {
    ensure();
    resume();
  } catch {
    /* нет Web Audio — молча живём без звука */
  }
}

// Один голос чаши: негармоничные партиалы + расстроенное биение.
function bowl(freq: number): void {
  ensure();
  resume();
  const c = ctx!;
  const now = c.currentTime;
  const partials = [
    { r: 1.0, g: 1.0, d: 9.5 },
    { r: 2.74, g: 0.52, d: 7.2 },
    { r: 5.24, g: 0.3, d: 5.4 },
    { r: 8.3, g: 0.16, d: 4.0 },
    { r: 12.1, g: 0.07, d: 2.8 },
  ];
  const voice = c.createGain();
  voice.gain.value = 0.0001;
  voice.connect(master!);
  partials.forEach((p, i) => {
    [-1, 1].forEach((sgn) => {
      const o = c.createOscillator();
      o.type = "sine";
      o.frequency.value = freq * p.r;
      o.detune.value = sgn * (2.4 + i * 1.6);
      const g = c.createGain();
      g.gain.value = p.g * 0.5;
      o.connect(g).connect(voice);
      o.start(now);
      g.gain.setValueAtTime(p.g * 0.5, now);
      g.gain.exponentialRampToValueAtTime(0.0001, now + p.d);
      o.stop(now + p.d + 0.2);
    });
  });
  voice.gain.setValueAtTime(0.0001, now);
  voice.gain.exponentialRampToValueAtTime(0.24, now + 0.32);
  voice.gain.exponentialRampToValueAtTime(0.13, now + 1.7);
  voice.gain.exponentialRampToValueAtTime(0.0001, now + 9.5);
}

// Тёплая пентатоника, чтобы повторные удары оставались консонансными.
const SCALE = [196.0, 220.0, 261.63, 293.66, 329.63]; // G3 A3 C4 D4 E4
let scaleIdx = 0;

/** Удар чаши (welcome-капля). Возвращает true, если звук сыгран. */
export function strikeBowl(): boolean {
  if (!soundOn) return false;
  const t = performance.now();
  if (t - lastStrike < 850) return false;
  lastStrike = t;
  try {
    bowl(SCALE[scaleIdx % SCALE.length]);
    scaleIdx++;
    return true;
  } catch {
    return false;
  }
}

function swellEnv(param: AudioParam, now: number, dur: number, base: number): void {
  param.setValueAtTime(0.0001, now);
  let t = now + 0.06;
  while (t < now + dur - 0.15) {
    param.linearRampToValueAtTime((0.25 + Math.random() * 0.75) * base, t);
    t += 0.1 + Math.random() * 0.24;
  }
  param.linearRampToValueAtTime(0.0001, now + dur);
}

/** Шум леса — листва на ветру (пасхалка логотипа). */
export function rustle(): void {
  if (!soundOn) return;
  try {
    ensure();
    resume();
  } catch {
    return;
  }
  const c = ctx!;
  const now = c.currentTime;
  const dur = 2.3;
  const mk = (freqSetup: (f: BiquadFilterNode) => void, base: number) => {
    const s = c.createBufferSource();
    s.buffer = makeImpulse(dur, 0.05);
    const f = c.createBiquadFilter();
    freqSetup(f);
    const g = c.createGain();
    swellEnv(g.gain, now, dur, base);
    s.connect(f).connect(g).connect(master!);
    s.start(now);
  };
  mk((f) => {
    f.type = "bandpass";
    f.Q.value = 0.7;
    f.frequency.setValueAtTime(2600, now);
    f.frequency.linearRampToValueAtTime(3600, now + dur * 0.5);
    f.frequency.linearRampToValueAtTime(2400, now + dur);
  }, 0.12);
  mk((f) => {
    f.type = "bandpass";
    f.Q.value = 1.4;
    f.frequency.value = 6800;
  }, 0.05);
  mk((f) => {
    f.type = "lowpass";
    f.frequency.value = 900;
  }, 0.035);
}

function wobble(param: AudioParam, t0: number, t1: number, base: number, amp: number, step: number): void {
  let t = t0;
  while (t < t1) {
    param.linearRampToValueAtTime(base + (Math.random() * 2 - 1) * amp, t);
    t += step * (0.7 + Math.random() * 0.6);
  }
}

function thunder(dst: AudioNode, t0: number, inten: number): void {
  const c = ctx!;
  const crack = c.createBufferSource();
  crack.buffer = makeImpulse(0.5, 1.4);
  const cf = c.createBiquadFilter();
  cf.type = "bandpass";
  cf.frequency.value = 950;
  cf.Q.value = 0.6;
  const cg = c.createGain();
  cg.gain.setValueAtTime(0.0001, t0);
  cg.gain.exponentialRampToValueAtTime(0.5 * inten, t0 + 0.03);
  cg.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.5);
  crack.connect(cf).connect(cg).connect(dst);
  crack.start(t0);
  crack.stop(t0 + 0.6);
  const roll = c.createBufferSource();
  roll.buffer = flatNoise(6.5);
  const rf = c.createBiquadFilter();
  rf.type = "lowpass";
  rf.frequency.setValueAtTime(190, t0);
  rf.frequency.exponentialRampToValueAtTime(58, t0 + 5.5);
  const rg = c.createGain();
  rg.gain.setValueAtTime(0.0001, t0);
  rg.gain.exponentialRampToValueAtTime(0.6 * inten, t0 + 0.1);
  rg.gain.exponentialRampToValueAtTime(0.28 * inten, t0 + 1.1);
  rg.gain.exponentialRampToValueAtTime(0.36 * inten, t0 + 1.9);
  rg.gain.exponentialRampToValueAtTime(0.12 * inten, t0 + 3.4);
  rg.gain.exponentialRampToValueAtTime(0.0001, t0 + 6.2);
  roll.connect(rf).connect(rg).connect(dst);
  roll.start(t0);
  roll.stop(t0 + 6.5);
}

/**
 * Пасхалка-гроза: раскат грома → шум хвойного леса на три минуты.
 * Возвращает true, если гроза стартовала (для запуска тени волка).
 */
export function storm(): boolean {
  if (stormOn || !soundOn) return false;
  try {
    ensure();
    resume();
  } catch {
    return false;
  }
  stormOn = true;
  const c = ctx!;
  const now = c.currentTime;
  const FOREST = 180;
  const fT0 = now + 2.2;
  const fT1 = fT0 + FOREST;
  const sg = c.createGain();
  sg.gain.value = 1;
  sg.connect(master!);
  const nodes: AudioScheduledSourceNode[] = [];
  stormStopper = () => {
    try {
      sg.gain.setTargetAtTime(0.0001, c.currentTime, 0.25);
    } catch {
      /* уже остановлено */
    }
    window.setTimeout(() => {
      nodes.forEach((n) => {
        try {
          n.stop();
        } catch {
          /* уже остановлен */
        }
      });
      try {
        sg.disconnect();
      } catch {
        /* уже отключён */
      }
    }, 1200);
    stormOn = false;
    stormStopper = null;
  };
  thunder(sg, now, 1.0);
  thunder(sg, now + 2.7, 0.45);
  const wind = c.createBufferSource();
  wind.buffer = flatNoise(2.7);
  wind.loop = true;
  const wf = c.createBiquadFilter();
  wf.type = "bandpass";
  wf.Q.value = 0.5;
  wf.frequency.setValueAtTime(430, fT0);
  for (let t = fT0 + 1; t < fT1; t += 2.4) wf.frequency.linearRampToValueAtTime(340 + Math.random() * 560, t);
  const wg = c.createGain();
  wg.gain.setValueAtTime(0.0001, fT0);
  wg.gain.linearRampToValueAtTime(0.15, fT0 + 3.5);
  wobble(wg.gain, fT0 + 3.5, fT1 - 8, 0.13, 0.06, 1.3);
  wg.gain.linearRampToValueAtTime(0.0001, fT1);
  wind.connect(wf).connect(wg).connect(sg);
  wind.start(fT0);
  wind.stop(fT1 + 0.5);
  nodes.push(wind);
  const hiss = c.createBufferSource();
  hiss.buffer = flatNoise(2.3);
  hiss.loop = true;
  const hf = c.createBiquadFilter();
  hf.type = "bandpass";
  hf.frequency.value = 5200;
  hf.Q.value = 1.3;
  const hg = c.createGain();
  hg.gain.setValueAtTime(0.0001, fT0 + 0.8);
  hg.gain.linearRampToValueAtTime(0.035, fT0 + 4.5);
  wobble(hg.gain, fT0 + 4.5, fT1 - 8, 0.03, 0.018, 0.8);
  hg.gain.linearRampToValueAtTime(0.0001, fT1);
  hiss.connect(hf).connect(hg).connect(sg);
  hiss.start(fT0 + 0.8);
  hiss.stop(fT1 + 0.5);
  nodes.push(hiss);
  const low = c.createBufferSource();
  low.buffer = flatNoise(3.1);
  low.loop = true;
  const lf = c.createBiquadFilter();
  lf.type = "lowpass";
  lf.frequency.value = 320;
  const lg = c.createGain();
  lg.gain.setValueAtTime(0.0001, fT0 + 0.4);
  lg.gain.linearRampToValueAtTime(0.045, fT0 + 5);
  wobble(lg.gain, fT0 + 5, fT1 - 8, 0.04, 0.015, 2.4);
  lg.gain.linearRampToValueAtTime(0.0001, fT1);
  low.connect(lf).connect(lg).connect(sg);
  low.start(fT0 + 0.4);
  low.stop(fT1 + 0.5);
  nodes.push(low);
  [26, 68, 117, 158].forEach((dt) => thunder(sg, fT0 + dt + Math.random() * 6, 0.18 + Math.random() * 0.16));
  window.setTimeout(() => {
    stormOn = false;
    stormStopper = null;
  }, (FOREST + 8) * 1000);
  return true;
}
