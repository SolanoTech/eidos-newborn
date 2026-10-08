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

package com.solano.eidosgateway.kafka;

import com.solano.eidosgateway.auth.TokenAuthService;
import com.solano.eidosgateway.entity.gateway.KafkaConfig;
import com.solano.eidosgateway.entity.registry.EntityType;
import com.solano.eidosgateway.service.KafkaConfigService;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Третий интерфейс Gateway — Kafka-консьюмеры. Конфигурация (bootstrap, topic,
 * group) берётся из БД Gateway: <b>по одной записи на тип сущности</b>, и на
 * каждую включённую поднимается свой контейнер. Топик и определяет тип: физлица
 * и юрлица приходят разными очередями, источник тип не присылает. Если активных
 * конфигураций нет — Gateway работает только как REST.
 *
 * <p>Каждое сообщение авторизуется по заголовку {@code X-Access-Token} и
 * пересылается в eidos-stage. Невалидные/ошибочные сообщения логируются и
 * пропускаются, чтобы не блокировать партицию.</p>
 */
@Component
public class GatewayKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(GatewayKafkaConsumer.class);

    private final KafkaConfigService kafkaConfigService;
    private final GatewayMessageHandler messageHandler;

    private final Object lock = new Object();
    private final List<ConcurrentMessageListenerContainer<String, byte[]>> containers = new ArrayList<>();

    public GatewayKafkaConsumer(KafkaConfigService kafkaConfigService, GatewayMessageHandler messageHandler) {
        this.kafkaConfigService = kafkaConfigService;
        this.messageHandler = messageHandler;
    }

    /** Первичный запуск при старте приложения. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        refresh();
    }

    /**
     * Перезапуск консьюмера после изменения Kafka-конфигурации через admin API.
     * Срабатывает после коммита транзакции сохранения, чтобы из БД читалась уже
     * актуальная конфигурация.
     */
    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void onConfigChanged(KafkaConfigChangedEvent event) {
        log.info("Kafka config changed — restarting Gateway Kafka consumer");
        refresh();
    }

    /**
     * Пересоздаёт контейнер по актуальной конфигурации из БД: останавливает
     * текущий (если есть) и поднимает новый. Если активной конфигурации нет —
     * консьюмер остаётся остановленным. Ошибки старта логируются и не
     * пробрасываются, чтобы не уронить вызвавший запрос.
     */
    public void refresh() {
        synchronized (lock) {
            stopContainers();
            List<KafkaConfig> configs = kafkaConfigService.findAllActive();
            if (configs.isEmpty()) {
                log.info("No active Kafka config in DB — Gateway Kafka consumers not running");
                return;
            }
            for (KafkaConfig config : configs) {
                try {
                    startContainer(config);
                } catch (Exception e) {
                    log.error("Failed to start Gateway Kafka consumer for {} (topic={})",
                            config.getEntityType(), config.getTopic(), e);
                }
            }
        }
    }

    private void startContainer(KafkaConfig cfg) {
        DefaultKafkaConsumerFactory<String, byte[]> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps(cfg), new StringDeserializer(), new ByteArrayDeserializer());

        EntityType entityType = cfg.getEntityType() == null ? EntityType.PERSON : cfg.getEntityType();
        ContainerProperties containerProperties = new ContainerProperties(cfg.getTopic());
        containerProperties.setGroupId(cfg.getGroupId());
        containerProperties.setMessageListener(
                (MessageListener<String, byte[]>) record -> onMessage(record, entityType));

        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
        container.start();
        containers.add(container);
        log.info("Gateway Kafka consumer started: entityType={} topic={} group={} servers={}",
                entityType, cfg.getTopic(), cfg.getGroupId(), cfg.getBootstrapServers());
    }

    void onMessage(ConsumerRecord<String, byte[]> record, EntityType entityType) {
        String token = header(record, TokenAuthService.ACCESS_TOKEN_HEADER);
        try {
            messageHandler.handle(token, record.value(), entityType);
        } catch (Exception e) {
            log.warn("Skipping Kafka message {}-{}@{}: {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage());
            throw e;
        }
    }

    private static Map<String, Object> consumerProps(KafkaConfig cfg) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, cfg.getGroupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    private static String header(ConsumerRecord<String, byte[]> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    @PreDestroy
    public void stop() {
        synchronized (lock) {
            stopContainers();
        }
    }

    private void stopContainers() {
        if (containers.isEmpty()) {
            return;
        }
        containers.forEach(ConcurrentMessageListenerContainer::stop);
        log.info("Gateway Kafka consumers stopped ({})", containers.size());
        containers.clear();
    }
}
