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
import com.solano.stage.entity.registry.EntityType;
import com.solano.stage.messaging.IngestMessage;
import com.solano.stage.pipeline.StagePipeline;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST-приём данных юридических лиц (мерчантов) — отдельный канал рядом с
 * {@link IngestController}. Тело то же самое: {@code {"data": {...}, "source": "..."}}.
 *
 * <p>Тип сущности определяется именно эндпоинтом: источник не присылает и не
 * может прислать {@code entity_type} — иначе один и тот же поток мог бы
 * произвольно менять целевую Золотую запись.</p>
 */
@RestController
@RequestMapping("/api/v1/legal-data")
public class LegalDataController {

    private final StagePipeline pipeline;
    private final ObjectMapper objectMapper;

    public LegalDataController(StagePipeline pipeline, ObjectMapper objectMapper) {
        this.pipeline = pipeline;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Map<String, String> ingest(@RequestBody byte[] body) {
        pipeline.process(parse(body), EntityType.LEGAL_ENTITY);
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
