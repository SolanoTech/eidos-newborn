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

package com.solano.eidosconsents.messaging;

import com.solano.eidosconsents.entity.Consent;
import com.solano.eidosconsents.entity.ConsentScanState;
import com.solano.eidosconsents.entity.ConsentType;
import com.solano.eidosconsents.repository.ConsentRepository;
import com.solano.eidosconsents.repository.ConsentScanStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Источник событий об истечении согласий.
 *
 * <p>Истечение — не действие, а следствие даты в строке: в полночь никакой код
 * не срабатывает и публиковать событие некому. Поэтому продюсера приходится
 * заводить явно — этот планировщик раз в сутки находит согласия, у которых
 * вчера был последний день действия.</p>
 *
 * <p>Согласие активно, пока {@code start_date <= день <= end_date}, поэтому
 * «истекло сегодня» означает {@code end_date = вчера}. Отзыв действующего
 * согласия сообщает о себе сам, сразу после коммита
 * ({@link ConsentRevocationListener}), и проход такие пары пропускает.</p>
 *
 * <p><b>Событие означает, что клиент перестал быть согласен, а не что
 * закончилась строка в таблице.</b> Поэтому окончание срока само по себе повода
 * не даёт: если у клиента есть другое действующее согласие того же типа, ничего
 * не публикуется. И на пару «клиент + тип» уходит ровно одно событие, даже если
 * в один день закончилось несколько согласий сразу.</p>
 *
 * <p><b>Пропущенные дни догоняются.</b> Последний пройденный день хранится в
 * БД ({@link ConsentScanState}); каждый запуск проходит все дни после него по
 * сегодняшний включительно, но не глубже
 * {@code eidos.consents.events.expiry-catch-up-days}. Кроме расписания проход
 * запускается при старте сервиса — простой в 00:05 догоняется сразу после
 * подъёма. День засчитывается, только если брокер подтвердил все его события;
 * иначе проход останавливается и повторит этот день при следующем запуске.
 * При самом первом запуске (состояния ещё нет) проходится только сегодняшний
 * день.</p>
 *
 * <p>Доставка — at-least-once: день, прерванный ошибкой, повторяется целиком, а
 * несколько экземпляров сервиса проходят дни независимо друг от друга.
 * Потребитель должен быть идемпотентен (для шифрования это естественно —
 * «зашифровать, если ещё не зашифровано»).</p>
 */
@Component
public class ConsentExpiryScanner {

    private static final Logger log = LoggerFactory.getLogger(ConsentExpiryScanner.class);

    private final ConsentRepository consentRepository;
    private final ConsentScanStateRepository scanStateRepository;
    private final ConsentEventPublisher publisher;
    private final Clock clock;
    private final int maxCatchUpDays;

    public ConsentExpiryScanner(
            ConsentRepository consentRepository,
            ConsentScanStateRepository scanStateRepository,
            ConsentEventPublisher publisher,
            Clock clock,
            @Value("${eidos.consents.events.expiry-catch-up-days:30}") int maxCatchUpDays
    ) {
        this.consentRepository = consentRepository;
        this.scanStateRepository = scanStateRepository;
        this.publisher = publisher;
        this.clock = clock;
        this.maxCatchUpDays = maxCatchUpDays;
    }

    /**
     * Догоняющий проход при старте: покрывает простой во время планового
     * запуска. Сбой (например, недоступна БД) не должен мешать подъёму сервиса —
     * дни догонит следующий плановый проход.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scanOnStartup() {
        try {
            scanToday();
        } catch (RuntimeException e) {
            log.error("Expiry catch-up on startup failed, waiting for the scheduled run", e);
        }
    }

    /**
     * Плановый проход: все дни после последнего пройденного по сегодняшний.
     * Ошибка отправки не пробрасывается — день останется непройденным и
     * повторится при следующем запуске.
     */
    @Scheduled(cron = "${eidos.consents.events.expiry-cron}")
    public synchronized void scanToday() {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = firstDayToScan(today);
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
            try {
                publishExpiredOn(date);
            } catch (ConsentEventSendException e) {
                log.error("Expiry scan for {} interrupted, it will be retried on the next run", date, e);
                return;
            }
            scanStateRepository.save(new ConsentScanState(ConsentScanState.EXPIRY, date));
        }
    }

    private LocalDate firstDayToScan(LocalDate today) {
        LocalDate lastScanned = scanStateRepository.findById(ConsentScanState.EXPIRY)
                .map(ConsentScanState::getLastScannedDate)
                .orElse(null);
        if (lastScanned == null) {
            return today;
        }
        LocalDate next = lastScanned.plusDays(1);
        LocalDate earliest = today.minusDays(maxCatchUpDays);
        if (next.isBefore(earliest)) {
            log.warn("Expiry scan skips {} .. {}: older than the catch-up depth of {} days",
                    next, earliest.minusDays(1), maxCatchUpDays);
            return earliest;
        }
        return next;
    }

    /**
     * Излучает {@code expired} по клиентам, потерявшим на дату {@code date}
     * согласие какого-либо типа: срок закончился вчера
     * ({@code end_date = date - 1}) и действующей замены того же типа нет.
     *
     * @return сколько событий отправлено
     * @throws ConsentEventSendException если брокер не подтвердил событие;
     *         часть событий дня к этому моменту могла уже уйти
     */
    public int publishExpiredOn(LocalDate date) {
        List<Consent> expired =
                consentRepository.findExpiredWithoutActiveConsent(date.minusDays(1), date);
        if (expired.isEmpty()) {
            log.debug("Nobody lost consent on {}", date);
            return 0;
        }
        Collection<Consent> perClientAndType = latestPerClientAndType(expired);
        perClientAndType.forEach(consent ->
                publisher.send(ConsentEvent.of(ConsentEventType.EXPIRED, consent)));
        log.info("Published {} expired consent events for {}", perClientAndType.size(), date);
        return perClientAndType.size();
    }

    /**
     * Одно событие на пару «клиент + тип». Запрос уже отсортирован так, что
     * внутри пары первым идёт самое позднее согласие — его даты и попадают в
     * событие.
     */
    private static Collection<Consent> latestPerClientAndType(List<Consent> expired) {
        Map<Key, Consent> latest = new LinkedHashMap<>();
        for (Consent consent : expired) {
            latest.putIfAbsent(new Key(consent.getClientUuid(), consent.getType()), consent);
        }
        return latest.values();
    }

    private record Key(UUID clientUuid, ConsentType type) {
    }
}
