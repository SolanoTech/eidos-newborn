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

package com.solano.eidoscore.service.legal;

import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.entity.TentativeReason;
import com.solano.eidoscore.entity.legal.LegalEntityFieldMeta;
import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.eidoscore.entity.legal.TentativeLegalEntity;
import com.solano.eidoscore.exception.NotFoundException;
import com.solano.eidoscore.exception.UnprocessableException;
import com.solano.eidoscore.repository.SourceRepository;
import com.solano.eidoscore.repository.legal.LegalEntityFieldMetaRepository;
import com.solano.eidoscore.repository.legal.LegalEntityRecordRepository;
import com.solano.eidoscore.repository.legal.TentativeLegalEntityRepository;
import com.solano.eidoscore.service.legal.mapper.LegalEntityMapper;
import com.solano.eidoscore.web.dto.TentativeConflictField;
import com.solano.eidoscore.web.dto.TentativeLegalItemView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Очередь «неопределённых» записей юрлиц и их разрешение оператором —
 * зеркало {@code TentativeService} для второй вертикали. Семантика та же:
 * grey-zone хранит ВХОДЯЩИЙ снимок конфликтующего источника, текущее
 * состояние живёт в основной таблице.
 */
@Service
public class LegalEntityTentativeService {

    public enum ResolveAction { KEEP_CURRENT, ACCEPT_INCOMING, REJECT }

    private static final Logger log = LoggerFactory.getLogger(LegalEntityTentativeService.class);
    private static final int PAGE_SIZE = 20;

    private final TentativeLegalEntityRepository tentativeRepository;
    private final LegalEntityRecordRepository recordRepository;
    private final LegalEntityFieldMetaRepository fieldMetaRepository;
    private final SourceRepository sourceRepository;
    private final LegalEntityMapper mapper;
    private final LegalEntitySnapshotter snapshotter;

    public LegalEntityTentativeService(
            TentativeLegalEntityRepository tentativeRepository,
            LegalEntityRecordRepository recordRepository,
            LegalEntityFieldMetaRepository fieldMetaRepository,
            SourceRepository sourceRepository,
            LegalEntityMapper mapper,
            LegalEntitySnapshotter snapshotter
    ) {
        this.tentativeRepository = tentativeRepository;
        this.recordRepository = recordRepository;
        this.fieldMetaRepository = fieldMetaRepository;
        this.sourceRepository = sourceRepository;
        this.mapper = mapper;
        this.snapshotter = snapshotter;
    }

    @Transactional(readOnly = true)
    public Page<TentativeLegalItemView> list(TentativeReason reason, int page) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TentativeLegalEntity> src = reason == null
                ? tentativeRepository.findAll(pageable)
                : tentativeRepository.findByReason(reason, pageable);
        return src.map(this::toView);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> counts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("total", tentativeRepository.count());
        counts.put("greyZone", tentativeRepository.countByReason(TentativeReason.GREY_ZONE_CONFLICT));
        counts.put("unknownSource", tentativeRepository.countByReason(TentativeReason.UNKNOWN_SOURCE));
        return counts;
    }

    @Transactional
    public void resolve(Long id, ResolveAction action) {
        TentativeLegalEntity tentative = tentativeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tentative legal entity " + id + " not found"));

        if (action == ResolveAction.ACCEPT_INCOMING) {
            acceptIncoming(tentative);
        }
        tentativeRepository.delete(tentative);
        log.info("Tentative legal entity {} resolved with action={} (reason={}, source={})",
                id, action, tentative.getReason(), tentative.getSourceName());
    }

    private void acceptIncoming(TentativeLegalEntity tentative) {
        if (tentative.getGrLegalEntityId() == null) {
            throw new UnprocessableException(
                    "Tentative record has no legal entity to apply to; register the source and resend");
        }
        LegalEntityRecord record = recordRepository.findById(tentative.getGrLegalEntityId())
                .orElseThrow(() -> new NotFoundException("Legal entity not found"));
        Source incomingSource = sourceRepository.findBySourceName(tentative.getSourceName())
                .orElseThrow(() -> new UnprocessableException(
                        "Source '" + tentative.getSourceName() + "' is not registered in core"));

        LegalEntityRecord scratch = scratchFrom(tentative);
        Map<String, LegalEntityFieldMeta> metas = fieldMetaRepository
                .findByIdGrLegalEntityId(record.getGrLegalEntityId()).stream()
                .collect(Collectors.toMap(m -> m.getId().getFieldName(), Function.identity()));

        List<LegalEntityFieldMeta> touched = new ArrayList<>();
        boolean changed = false;
        for (String field : mapper.knownFieldNames()) {
            Object incoming = mapper.readFromEntity(scratch, field);
            Object current = mapper.readFromEntity(record, field);
            if (incoming != null && !Objects.equals(incoming, current)) {
                mapper.copyEntityField(scratch, record, field);
                LegalEntityFieldMeta meta = metas.get(field);
                if (meta != null) {
                    meta.setSource(incomingSource);
                    touched.add(meta);
                }
                changed = true;
            }
        }
        if (changed) {
            // saveAndFlush: архив должен увидеть уже инкрементированный @Version.
            LegalEntityRecord saved = recordRepository.saveAndFlush(record);
            fieldMetaRepository.saveAll(touched);
            snapshotter.archive(saved);
        }
    }

    private TentativeLegalItemView toView(TentativeLegalEntity tentative) {
        LegalEntityRecord scratch = scratchFrom(tentative);
        Integer sourceTrust = sourceRepository.findBySourceName(tentative.getSourceName())
                .map(Source::getTrustLevel).orElse(null);

        LegalEntityRecord record = tentative.getGrLegalEntityId() == null
                ? null
                : recordRepository.findById(tentative.getGrLegalEntityId()).orElse(null);

        LegalEntityRecord display = record != null ? record : scratch;
        List<TentativeConflictField> conflicts = List.of();
        if (record != null) {
            Map<String, LegalEntityFieldMeta> metas = fieldMetaRepository
                    .findByIdGrLegalEntityId(record.getGrLegalEntityId()).stream()
                    .collect(Collectors.toMap(m -> m.getId().getFieldName(), Function.identity()));
            conflicts = mapper.knownFieldNames().stream()
                    .map(field -> {
                        Object incoming = mapper.readFromEntity(scratch, field);
                        Object current = mapper.readFromEntity(record, field);
                        if (incoming == null || Objects.equals(incoming, current)) {
                            return null;
                        }
                        LegalEntityFieldMeta meta = metas.get(field);
                        String curSource = null;
                        Integer curTrust = null;
                        if (meta != null) {
                            curSource = meta.getSource().getSourceName();
                            curTrust = meta.getSource().getTrustLevel();
                        }
                        return new TentativeConflictField(field, str(current), str(incoming), curSource, curTrust);
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

        Map<String, String> snapshot = new LinkedHashMap<>();
        for (String field : mapper.knownFieldNames()) {
            Object v = mapper.readFromEntity(scratch, field);
            if (v != null) {
                snapshot.put(field, str(v));
            }
        }

        return new TentativeLegalItemView(
                tentative.getId(),
                tentative.getReason().name(),
                tentative.getGrLegalEntityId(),
                tentative.getSourceName(),
                sourceTrust,
                merchantName(display),
                display.getGrInn(),
                tentative.getCreatedAt() == null ? null : tentative.getCreatedAt().toString(),
                conflicts,
                snapshot);
    }

    private LegalEntityRecord scratchFrom(TentativeLegalEntity tentative) {
        LegalEntityRecord scratch = new LegalEntityRecord();
        BeanUtils.copyProperties(tentative, scratch,
                "grLegalEntityId", "version", "createdAt", "updatedAt", "nameNormalized");
        return scratch;
    }

    private static String merchantName(LegalEntityRecord record) {
        String full = record.getGrFullName();
        if (full != null && !full.isBlank()) {
            return full;
        }
        String shortName = record.getGrShortName();
        return shortName == null || shortName.isBlank() ? null : shortName;
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
