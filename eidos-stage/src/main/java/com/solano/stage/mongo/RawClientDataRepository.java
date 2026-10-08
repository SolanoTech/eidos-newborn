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

package com.solano.stage.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;

/**
 * Хранит сырые данные источника в MongoDB <b>в том же формате</b>, в котором они
 * пришли. Каждый документ: {@code source}, {@code data} (как есть), {@code receivedAt}.
 *
 * <p>Сохранение в Mongo — первый шаг pipeline и происходит всегда, ещё до
 * попытки трансформации, чтобы ни одно входящее сообщение не было потеряно.</p>
 */
@Repository
public class RawClientDataRepository {

    private static final Logger log = LoggerFactory.getLogger(RawClientDataRepository.class);

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final String collectionName;

    public RawClientDataRepository(
            MongoTemplate mongoTemplate,
            ObjectMapper objectMapper,
            @Value("${eidos.stage.raw-collection:raw_client_data}") String collectionName
    ) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.collectionName = collectionName;
    }

    /**
     * Сохраняет сырые данные. Возвращает строковый id вставленного документа
     * (для трассировки / последующих ссылок).
     */
    public String save(String source, JsonNode data) {
        return save(source, data, null);
    }

    /** Сохраняет сырьё с пометкой типа сущности (канал приёма). */
    public String save(String source, JsonNode data, String entityType) {
        Document document = new Document()
                .append("source", source)
                .append("entityType", entityType)
                .append("data", toBson(data))
                .append("receivedAt", Date.from(Instant.now()));

        MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);
        collection.insertOne(document);

        ObjectId id = document.getObjectId("_id");
        String idHex = id == null ? null : id.toHexString();
        log.debug("Stored raw client data id={} source={}", idHex, source);
        return idHex;
    }

    /**
     * Конвертирует {@link JsonNode} в BSON-представление, сохраняя структуру.
     * Объекты → вложенный {@link Document}; массивы и примитивы оборачиваются,
     * чтобы их тоже можно было хранить без потери формата.
     */
    private Object toBson(JsonNode data) {
        if (data == null || data.isNull() || data.isMissingNode()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(data);
            if (data.isObject()) {
                return Document.parse(json);
            }
            // Не-объект (массив/примитив) оборачиваем, т.к. поле data может быть любым.
            return Document.parse("{\"value\": " + json + "}").get("value");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert raw data to BSON for source store", e);
        }
    }
}
