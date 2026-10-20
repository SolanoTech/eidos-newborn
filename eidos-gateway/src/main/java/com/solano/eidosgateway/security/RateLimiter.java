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

package com.solano.eidosgateway.security;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Ограничитель частоты запросов на основе ведра токенов: ведро наполняется
 * равномерно до {@code perMinute} в минуту, каждый запрос забирает один токен.
 *
 * <p>Ведро, а не счётчик в окне: счётчик в фиксированном окне пропускает
 * двойную норму на стыке двух окон, а ведро сглаживает поток и при этом
 * позволяет короткий всплеск на полный запас.</p>
 *
 * <p>Состояние — в памяти процесса. Для одного экземпляра шлюза этого
 * достаточно; при нескольких экземплярах фактический предел умножается на их
 * число, поэтому общий предел на источника остаётся задачей обратного прокси
 * или внешнего ограничителя.</p>
 *
 * <p>Класс потокобезопасен: ведро берёт свои токены под собственной
 * блокировкой, карта ведёр — {@link ConcurrentHashMap}.</p>
 */
public class RateLimiter {

    /** Ведро, к которому не обращались дольше этого срока, можно забыть. */
    private static final long IDLE_EVICTION_NANOS = TimeUnit.MINUTES.toNanos(10);

    /** При таком числе ведёр делается уборка: иначе карта растёт вместе с числом ключей. */
    private static final int SWEEP_THRESHOLD = 10_000;

    private static final long NANOS_PER_MINUTE = TimeUnit.MINUTES.toNanos(1);

    /** Решение по запросу: пропускать или ждать {@code retryAfterSeconds}. */
    public record Decision(boolean allowed, int retryAfterSeconds) {

        static Decision pass() {
            return new Decision(true, 0);
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public RateLimiter() {
        this(System::nanoTime);
    }

    /** Для тестов: позволяет управлять временем вместо ожидания. */
    public RateLimiter(LongSupplier nanoClock) {
        this.clock = nanoClock;
    }

    /**
     * Забирает токен из ведра ключа.
     *
     * @param key      ключ, по которому считается частота (источник и область)
     * @param perMinute предел запросов в минуту; значение {@code <= 0} выключает ограничение
     */
    public Decision tryAcquire(String key, int perMinute) {
        if (perMinute <= 0) {
            return Decision.pass();
        }
        long now = clock.getAsLong();
        sweepIfCrowded(now);
        return buckets.computeIfAbsent(key, k -> new Bucket(perMinute, now)).take(perMinute, now);
    }

    private void sweepIfCrowded(long now) {
        if (buckets.size() < SWEEP_THRESHOLD) {
            return;
        }
        Iterator<Bucket> it = buckets.values().iterator();
        while (it.hasNext()) {
            if (it.next().idleSince(now) > IDLE_EVICTION_NANOS) {
                it.remove();
            }
        }
    }

    private static final class Bucket {

        private double tokens;
        private long updatedAt;

        private Bucket(int perMinute, long now) {
            this.tokens = perMinute;
            this.updatedAt = now;
        }

        private synchronized Decision take(int perMinute, long now) {
            // Предел мог измениться вместе с конфигурацией — запас не должен
            // пережить её: он обрезается текущим значением.
            double refilled = (double) (now - updatedAt) / NANOS_PER_MINUTE * perMinute;
            tokens = Math.min(perMinute, tokens + Math.max(refilled, 0));
            updatedAt = now;
            if (tokens >= 1) {
                tokens -= 1;
                return Decision.pass();
            }
            double missing = 1 - tokens;
            int wait = (int) Math.ceil(missing * 60 / perMinute);
            return new Decision(false, Math.max(wait, 1));
        }

        private synchronized long idleSince(long now) {
            return now - updatedAt;
        }
    }
}
