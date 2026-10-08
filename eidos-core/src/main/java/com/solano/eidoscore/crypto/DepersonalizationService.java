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

package com.solano.eidoscore.crypto;

import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.entity.vault.PdToken;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.service.mapper.GoldenRecordMapper;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Обезличивание и обратное раскрытие Золотой записи.
 *
 * <p>Обезличивание заменяет персональные значения токенами хранилища: сами
 * значения уезжают в зашифрованном виде, в карточке остаются ссылки. Обратное
 * раскрытие возвращает значения на место, когда клиент снова даёт согласие.</p>
 *
 * <p>Слепой индекс не трогается ни в одну из сторон: он посчитан от открытых
 * значений и должен таким остаться, иначе сопоставление и точный поиск
 * перестанут находить обезличенные записи. По этой же причине запись идёт
 * точечным UPDATE в обход сущности — слушатель, пересчитывающий индекс, при
 * таком обновлении не срабатывает.</p>
 *
 * <p>Обе операции идемпотентны: уже обезличенная запись пропускается, уже
 * открытая — тоже.</p>
 */
@Service
public class DepersonalizationService {

    private static final Logger log = LoggerFactory.getLogger(DepersonalizationService.class);

    private final GoldenRecordRepository goldenRecordRepository;
    private final GoldenRecordMapper mapper;
    private final TokenizationService tokenizationService;
    private final EntityManager entityManager;
    private final Clock clock;

    public DepersonalizationService(
            GoldenRecordRepository goldenRecordRepository,
            GoldenRecordMapper mapper,
            TokenizationService tokenizationService,
            EntityManager entityManager,
            Clock clock
    ) {
        this.goldenRecordRepository = goldenRecordRepository;
        this.mapper = mapper;
        this.tokenizationService = tokenizationService;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /**
     * Заменяет персональные значения записи токенами.
     *
     * @return {@code true}, если запись была обезличена этим вызовом
     */
    @Transactional
    public boolean depersonalize(GoldenRecord record) {
        if (record.isTokenized()) {
            return false;
        }
        String clientId = record.getGrClientId();
        UUID batch = tokenizationService.openBatch(clientId);

        Map<String, String> tokens = new HashMap<>();
        for (String field : PersonalDataFields.NAMES) {
            Object value = mapper.readFromEntity(record, field);
            if (value == null) {
                continue;
            }
            tokens.put(field, tokenizationService.tokenize(batch, clientId, field, value.toString()));
        }
        if (tokens.isEmpty()) {
            log.info("Nothing to tokenise for {}", clientId);
            return false;
        }
        write(record, tokens, LocalDateTime.now(clock));
        log.info("Depersonalised {}: {} fields tokenised", clientId, tokens.size());
        return true;
    }

    /**
     * Возвращает открытые значения на место.
     *
     * @return {@code true}, если запись была раскрыта этим вызовом
     */
    @Transactional
    public boolean restore(GoldenRecord record) {
        if (!record.isTokenized()) {
            return false;
        }
        String clientId = record.getGrClientId();
        Map<String, String> plaintext = new HashMap<>();
        for (PdToken token : tokenizationService.tokensOf(clientId)) {
            // Хранилище помнит и прежние токены этого клиента; в карточке стоит
            // только последний, по нему и восстанавливаем.
            Object current = mapper.readFromEntity(record, token.getFieldName());
            if (current != null && current.toString().equals(token.getToken())) {
                plaintext.put(token.getFieldName(), tokenizationService.detokenize(token.getToken()));
            }
        }
        if (plaintext.isEmpty()) {
            log.warn("No stored values found for {}; record left as is", clientId);
            return false;
        }
        write(record, plaintext, null);
        log.info("Restored {}: {} fields back in the clear", clientId, plaintext.size());
        return true;
    }

    /**
     * Одной записью: значения всех персональных полей и отметка обезличивания.
     *
     * <p>Запрос переписывает все перечисленные колонки, поэтому отправляются и
     * те поля, которых изменение не касалось: их текущие значения. Иначе
     * неразрешённое поле затёрлось бы пустым значением.</p>
     */
    private void write(GoldenRecord record, Map<String, String> changed, LocalDateTime tokenizedAt) {
        List<String> ordered = PersonalDataFields.NAMES;
        String[] v = new String[ordered.size()];
        for (int i = 0; i < ordered.size(); i++) {
            String field = ordered.get(i);
            if (changed.containsKey(field)) {
                v[i] = changed.get(field);
            } else {
                Object current = mapper.readFromEntity(record, field);
                v[i] = current == null ? null : current.toString();
            }
        }
        goldenRecordRepository.updatePersonalData(
                record.getGrClientId(), v[0], v[1], v[2], v[3], v[4], v[5], v[6],
                v[7], v[8], v[9], v[10], v[11], v[12], tokenizedAt);
        // Массовый UPDATE идёт мимо контекста персистентности: без этого
        // переданная сущность осталась бы со старыми значениями, и вызывающий
        // код работал бы с тем, чего в базе уже нет.
        entityManager.refresh(record);
    }
}
