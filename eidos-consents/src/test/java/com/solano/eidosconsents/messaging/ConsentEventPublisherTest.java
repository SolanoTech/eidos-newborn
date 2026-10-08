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

import com.solano.eidosconsents.entity.ConsentType;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@code publish} не роняет вызывающего ни при каких ошибках Kafka;
 * {@code send} ждёт подтверждения и сообщает о сбое — на этом держится
 * догоняющий проход планировщика.
 */
@ExtendWith(MockitoExtension.class)
class ConsentEventPublisherTest {

    private static final String TOPIC = "consent-events";

    @Mock KafkaTemplate<String, String> kafkaTemplate;

    private final ConsentEvent event = new ConsentEvent(ConsentEventType.EXPIRED, UUID.randomUUID(),
            ConsentType.PERSONAL_DATA, LocalDate.of(2026, 2, 21), LocalDate.of(2026, 8, 20));

    @Test
    void send_waitsForTheBroker_andKeysByClient() {
        when(kafkaTemplate.send(eq(TOPIC), eq(event.partitionKey()), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher(true).send(event);

        verify(kafkaTemplate).send(eq(TOPIC), eq(event.partitionKey()), anyString());
    }

    @Test
    void send_reportsAFailedWrite() {
        when(kafkaTemplate.send(eq(TOPIC), eq(event.partitionKey()), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("no broker")));

        assertThatThrownBy(() -> publisher(true).send(event))
                .isInstanceOf(ConsentEventSendException.class);
    }

    @Test
    void publish_swallowsAFailedWrite() {
        CompletableFuture<SendResult<String, String>> failed =
                CompletableFuture.failedFuture(new TimeoutException("no broker"));
        when(kafkaTemplate.send(eq(TOPIC), eq(event.partitionKey()), anyString())).thenReturn(failed);

        assertThatCode(() -> publisher(true).publish(event)).doesNotThrowAnyException();
    }

    @Test
    void publish_swallowsASynchronousError() {
        when(kafkaTemplate.send(eq(TOPIC), eq(event.partitionKey()), anyString()))
                .thenThrow(new TimeoutException("metadata not available"));

        assertThatCode(() -> publisher(true).publish(event)).doesNotThrowAnyException();
    }

    @Test
    void disabled_sendsNothing() {
        ConsentEventPublisher publisher = publisher(false);

        publisher.publish(event);
        publisher.send(event);

        verifyNoInteractions(kafkaTemplate);
    }

    private ConsentEventPublisher publisher(boolean enabled) {
        return new ConsentEventPublisher(
                kafkaTemplate, new ObjectMapper(), TOPIC, enabled, Duration.ofSeconds(1));
    }
}
