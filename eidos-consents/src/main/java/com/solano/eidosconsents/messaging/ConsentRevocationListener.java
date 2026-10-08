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

import com.solano.eidosconsents.repository.ConsentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Источник событий {@code expired} об отзыве.
 *
 * <p>Отзыв обрезает срок вчерашним днём, и планировщик истечений его бы не
 * увидел: проход за сегодня, как правило, уже был, а завтрашний ищет
 * согласия, закончившиеся сегодня. Поэтому об отзыве сообщаем сразу.</p>
 *
 * <p>Проверка «осталось ли у клиента другое действующее согласие того же
 * типа» делается <b>после коммита</b>, в новой транзакции: так два
 * одновременных отзыва разных согласий одной пары видят результат друг друга
 * и не промолчат оба. Худший случай — оба опубликуют событие, что допустимо
 * при доставке не меньше одного раза.</p>
 *
 * <p>С планировщиком события не дублируются: пары, где в день прохода было
 * отозвано действовавшее согласие, проход пропускает (см.
 * {@code ConsentRepository#findExpiredWithoutActiveConsent}). Обратная сторона —
 * если Kafka недоступна в момент отзыва, событие теряется: проход его не
 * переизлучит.</p>
 */
@Component
public class ConsentRevocationListener {

    private static final Logger log = LoggerFactory.getLogger(ConsentRevocationListener.class);

    private final ConsentRepository consentRepository;
    private final ConsentEventPublisher publisher;

    public ConsentRevocationListener(ConsentRepository consentRepository, ConsentEventPublisher publisher) {
        this.consentRepository = consentRepository;
        this.publisher = publisher;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onRevoked(ConsentRevoked revoked) {
        ConsentEvent expired = revoked.expired();
        boolean stillConsents = consentRepository
                .findFirstByClientUuidAndTypeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        expired.clientUuid(), expired.type(), revoked.revokedOn(), revoked.revokedOn())
                .isPresent();
        if (stillConsents) {
            log.debug("Client {} still has an active {} consent after revocation, no event",
                    expired.clientUuid(), expired.type().getCode());
            return;
        }
        publisher.publish(expired);
    }
}
