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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Отправка событий о согласиях в Kafka.
 *
 * <p>Сообщение — JSON, сериализованный тем же {@link ObjectMapper}, что и
 * REST-ответы сервиса, и отправленный строковым сериализатором. Так на проводе
 * оказывается ровно то же представление, что видно в API, без служебных
 * заголовков типа {@code __TypeId__}, — потребителем может быть любой язык.</p>
 *
 * <p>Ключ сообщения — {@code client_uuid}: события одного клиента попадают в
 * одну партицию и сохраняют порядок между собой.</p>
 *
 * <p>Событие о выдаче согласия публикуется <b>после коммита</b> транзакции:
 * иначе потребители узнали бы о согласии, которое откатилось.</p>
 *
 * <p>Два режима отправки:</p>
 * <ul>
 *   <li>{@link #publish} — «отправил и забыл» для событий из запросов (выдача,
 *       отзыв). Ошибка только логируется: согласие уже сохранено, и запрос из-за
 *       Kafka падать не должен. Повторной отправки нет — если Kafka недоступна,
 *       событие теряется.</li>
 *   <li>{@link #send} — с ожиданием подтверждения брокера, для планировщика
 *       истечений. Ошибка пробрасывается, и проход за этот день повторяется
 *       позже.</li>
 * </ul>
 */
@Component
public class ConsentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ConsentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final boolean enabled;
    private final Duration sendTimeout;

    public ConsentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${eidos.consents.events.topic}") String topic,
            @Value("${eidos.consents.events.enabled}") boolean enabled,
            @Value("${eidos.consents.events.send-timeout:30s}") Duration sendTimeout
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.enabled = enabled;
        this.sendTimeout = sendTimeout;
    }

    /**
     * Слушает доменное событие сервиса и отправляет его в Kafka после успешного
     * коммита транзакции, в которой согласие было сохранено.
     */
    @TransactionalEventListener
    public void onCommittedConsentEvent(ConsentEvent event) {
        publish(event);
    }

    /**
     * Отправка без ожидания. Ошибки — и синхронные, и асинхронные (брокер не
     * подтвердил запись) — только логируются.
     */
    public void publish(ConsentEvent event) {
        if (!enabled) {
            log.debug("Consent events disabled, skipping {} for client {}",
                    event.eventType(), event.clientUuid());
            return;
        }
        try {
            kafkaTemplate.send(topic, event.partitionKey(), objectMapper.writeValueAsString(event))
                    .whenComplete((result, error) -> {
                        if (error == null) {
                            logPublished(event);
                        } else {
                            logLost(event, error);
                        }
                    });
        } catch (Exception e) {
            // Публикация не должна валить сохранение согласия: сам факт уже в БД.
            logLost(event, e);
        }
    }

    /**
     * Отправка с ожиданием подтверждения брокера.
     *
     * @throws ConsentEventSendException если брокер не подтвердил запись за
     *         {@code eidos.consents.events.send-timeout}
     */
    public void send(ConsentEvent event) {
        if (!enabled) {
            log.debug("Consent events disabled, skipping {} for client {}",
                    event.eventType(), event.clientUuid());
            return;
        }
        try {
            kafkaTemplate.send(topic, event.partitionKey(), objectMapper.writeValueAsString(event))
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConsentEventSendException(event, e);
        } catch (Exception e) {
            throw new ConsentEventSendException(event, e);
        }
        logPublished(event);
    }

    private static void logPublished(ConsentEvent event) {
        log.info("Published {} consent event: client={} type={} end_date={}",
                event.eventType().getCode(), event.clientUuid(),
                event.type().getCode(), event.endDate());
    }

    private static void logLost(ConsentEvent event, Throwable error) {
        log.error("Failed to publish {} consent event for client {} type={}, the event is lost",
                event.eventType(), event.clientUuid(), event.type().getCode(), error);
    }
}
