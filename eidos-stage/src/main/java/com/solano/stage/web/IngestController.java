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

package com.solano.stage.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solano.stage.messaging.IngestMessage;
import com.solano.stage.pipeline.StagePipeline;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST-приём клиентских данных. Тело — тот же формат, что раньше приходил из
 * Kafka: {@code {"data": {...}, "source": "..."}}.
 *
 * <p>Тело принимается «сырыми» байтами и разбирается нашим Jackson 2
 * {@link ObjectMapper}, а не штатными MVC-конвертерами. Причина: Spring Boot 4
 * сериализует MVC через Jackson 3, тогда как контракт {@link IngestMessage}
 * (поле {@code data} типа {@code com.fasterxml.jackson.databind.JsonNode}) и весь
 * pipeline используют Jackson 2. Ручной разбор держит весь модуль на одной
 * версии Jackson и совместим с shared-library.</p>
 *
 * <p>Контроллер тонкий: обработка — в {@link StagePipeline}, маппинг ошибок в
 * HTTP-коды — в {@link GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/v1/client-data")
public class IngestController {

    private final StagePipeline pipeline;
    private final ObjectMapper objectMapper;

    public IngestController(StagePipeline pipeline, ObjectMapper objectMapper) {
        this.pipeline = pipeline;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Map<String, String> ingest(@RequestBody byte[] body) {
        IngestMessage message = parse(body);
        pipeline.process(message);
        return Map.of("status", "SUCCESS");
    }

    private IngestMessage parse(byte[] body) {
        try {
            return objectMapper.readValue(body, IngestMessage.class);
        } catch (Exception e) {
            throw new MalformedRequestException("Malformed request body: " + e.getMessage(), e);
        }
    }
}
