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
import com.solano.eidoscore.entity.vault.PdDisclosure;
import com.solano.eidoscore.entity.vault.PdToken;
import com.solano.eidoscore.exception.NotFoundException;
import com.solano.eidoscore.exception.UnprocessableException;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.repository.vault.PdDisclosureRepository;
import com.solano.eidoscore.service.mapper.GoldenRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Раскрытие обезличенных значений по запросу — с основанием и следом в журнале.
 *
 * <p>Раскрытие не отменяет обезличивание: запись остаётся с токенами, значения
 * отдаются только в ответе на конкретный запрос. Это разовый просмотр, а не
 * смена формы хранения; вернуть данные в открытый вид может лишь возвращённое
 * согласие.</p>
 *
 * <p>Основание обязательно и пишется в журнал вместе с тем, кто запросил и
 * какие поля увидел. Запись в журнал идёт в той же транзакции, что и выдача:
 * раскрытия без следа не бывает.</p>
 *
 * <p>Кто вправе запрашивать, решает вызывающая сторона: у ядра нет модели
 * ролей, его внутренний API закрыт на периметре. Проверка роли живёт там, где
 * известна личность оператора.</p>
 */
@Service
public class DisclosureService {

    private static final Logger log = LoggerFactory.getLogger(DisclosureService.class);

    /** Короткая отписка основанием не является. */
    private static final int MIN_REASON_LENGTH = 10;

    private final GoldenRecordRepository goldenRecordRepository;
    private final GoldenRecordMapper mapper;
    private final TokenizationService tokenizationService;
    private final PdDisclosureRepository disclosureRepository;
    private final Clock clock;

    public DisclosureService(
            GoldenRecordRepository goldenRecordRepository,
            GoldenRecordMapper mapper,
            TokenizationService tokenizationService,
            PdDisclosureRepository disclosureRepository,
            Clock clock
    ) {
        this.goldenRecordRepository = goldenRecordRepository;
        this.mapper = mapper;
        this.tokenizationService = tokenizationService;
        this.disclosureRepository = disclosureRepository;
        this.clock = clock;
    }

    /**
     * Раскрывает запрошенные поля обезличенной записи.
     *
     * @param fields какие поля раскрыть; пусто — все персональные
     * @return поле → открытое значение
     */
    @Transactional
    public Map<String, String> reveal(String clientId, List<String> fields, String actor, String reason) {
        if (actor == null || actor.isBlank()) {
            throw new UnprocessableException("Disclosure requires an actor");
        }
        if (reason == null || reason.strip().length() < MIN_REASON_LENGTH) {
            throw new UnprocessableException(
                    "Disclosure requires a reason of at least " + MIN_REASON_LENGTH + " characters");
        }
        GoldenRecord record = goldenRecordRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Golden Record Not Found"));
        if (!record.isTokenized()) {
            throw new UnprocessableException("Record " + clientId + " is not depersonalised");
        }

        List<String> wanted = (fields == null || fields.isEmpty()) ? PersonalDataFields.NAMES : fields;
        Map<String, String> tokensByField = tokensByField(clientId, record);

        Map<String, String> revealed = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now(clock);
        for (String field : wanted) {
            String token = tokensByField.get(field);
            if (token == null) {
                continue;
            }
            revealed.put(field, tokenizationService.detokenize(token));
            disclosureRepository.save(PdDisclosure.builder()
                    .disclosureId(UUID.randomUUID())
                    .ownerId(clientId)
                    .fieldName(field)
                    .actor(actor)
                    .reason(reason.strip())
                    .disclosedAt(now)
                    .build());
        }
        log.info("Disclosed {} fields of {} to {}", revealed.size(), clientId, actor);
        return revealed;
    }

    /** Токены, которые реально стоят в карточке сейчас. */
    private Map<String, String> tokensByField(String clientId, GoldenRecord record) {
        Map<String, String> current = new LinkedHashMap<>();
        for (PdToken token : tokenizationService.tokensOf(clientId)) {
            Object value = mapper.readFromEntity(record, token.getFieldName());
            if (value != null && value.toString().equals(token.getToken())) {
                current.put(token.getFieldName(), token.getToken());
            }
        }
        return current;
    }
}
