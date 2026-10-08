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

package com.solano.eidoscore.service;

import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.eidoscore.entity.GoldenRecordFieldMeta;
import com.solano.eidoscore.entity.Source;
import com.solano.eidoscore.entity.TentativeGoldenRecord;
import com.solano.eidoscore.entity.TentativeReason;
import com.solano.eidoscore.exception.NotFoundException;
import com.solano.eidoscore.exception.UnprocessableException;
import com.solano.eidoscore.repository.GoldenRecordFieldMetaRepository;
import com.solano.eidoscore.repository.GoldenRecordRepository;
import com.solano.eidoscore.repository.SourceRepository;
import com.solano.eidoscore.repository.TentativeGoldenRecordRepository;
import com.solano.eidoscore.service.mapper.GoldenRecordMapper;
import com.solano.eidoscore.web.dto.TentativeConflictField;
import com.solano.eidoscore.web.dto.TentativeItemView;
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
 * Очередь «неопределённых» записей и их разрешение оператором.
 *
 * <p>Grey-zone: tentative хранит ВХОДЯЩИЙ снимок конфликтующего источника,
 * текущее состояние — в {@code golden_record}. Разрешение:</p>
 * <ul>
 *   <li>{@code KEEP_CURRENT} — оставить текущие значения, запись из очереди
 *       удаляется;</li>
 *   <li>{@code ACCEPT_INCOMING} — применить отличающиеся значения входящего
 *       снимка к Golden Record (c переключением provenance на источник
 *       снимка и архивной ревизией);</li>
 *   <li>{@code REJECT} — отклонить запись (единственное действие для
 *       {@code UNKNOWN_SOURCE}, кроме регистрации источника и переотправки).</li>
 * </ul>
 */
@Service
public class TentativeService {

    public enum ResolveAction { KEEP_CURRENT, ACCEPT_INCOMING, REJECT }

    private static final Logger log = LoggerFactory.getLogger(TentativeService.class);
    private static final int PAGE_SIZE = 20;

    private final TentativeGoldenRecordRepository tentativeRepository;
    private final GoldenRecordRepository goldenRecordRepository;
    private final GoldenRecordFieldMetaRepository fieldMetaRepository;
    private final SourceRepository sourceRepository;
    private final GoldenRecordMapper mapper;
    private final GoldenRecordSnapshotter snapshotter;

    public TentativeService(
            TentativeGoldenRecordRepository tentativeRepository,
            GoldenRecordRepository goldenRecordRepository,
            GoldenRecordFieldMetaRepository fieldMetaRepository,
            SourceRepository sourceRepository,
            GoldenRecordMapper mapper,
            GoldenRecordSnapshotter snapshotter
    ) {
        this.tentativeRepository = tentativeRepository;
        this.goldenRecordRepository = goldenRecordRepository;
        this.fieldMetaRepository = fieldMetaRepository;
        this.sourceRepository = sourceRepository;
        this.mapper = mapper;
        this.snapshotter = snapshotter;
    }

    @Transactional(readOnly = true)
    public Page<TentativeItemView> list(TentativeReason reason, int page) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TentativeGoldenRecord> src = reason == null
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
        TentativeGoldenRecord tentative = tentativeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tentative record " + id + " not found"));

        if (action == ResolveAction.ACCEPT_INCOMING) {
            acceptIncoming(tentative);
        }
        // KEEP_CURRENT / REJECT / успешный ACCEPT_INCOMING — запись покидает очередь.
        tentativeRepository.delete(tentative);
        log.info("Tentative {} resolved with action={} (reason={}, source={})",
                id, action, tentative.getReason(), tentative.getSourceName());
    }

    private void acceptIncoming(TentativeGoldenRecord tentative) {
        if (tentative.getReason() == TentativeReason.UNKNOWN_SOURCE || tentative.getGrClientId() == null) {
            throw new UnprocessableException(
                    "Unknown source records cannot be merged: register the source and resend the data");
        }
        GoldenRecord gr = goldenRecordRepository.findById(tentative.getGrClientId())
                .orElseThrow(() -> new NotFoundException("Golden Record Not Found"));
        Source incomingSource = sourceRepository.findBySourceName(tentative.getSourceName())
                .orElseThrow(() -> new UnprocessableException(
                        "Source '" + tentative.getSourceName() + "' is not registered in core"));

        GoldenRecord scratch = scratchFrom(tentative);
        Map<String, GoldenRecordFieldMeta> metas = fieldMetaRepository
                .findByIdGrClientId(gr.getGrClientId()).stream()
                .collect(Collectors.toMap(m -> m.getId().getFieldName(), Function.identity()));

        List<GoldenRecordFieldMeta> touched = new ArrayList<>();
        boolean changed = false;
        for (String field : mapper.knownFieldNames()) {
            Object incoming = mapper.readFromEntity(scratch, field);
            Object current = mapper.readFromEntity(gr, field);
            if (incoming != null && !Objects.equals(incoming, current)) {
                mapper.copyEntityField(scratch, gr, field);
                GoldenRecordFieldMeta meta = metas.get(field);
                if (meta != null) {
                    meta.setSource(incomingSource);
                    touched.add(meta);
                }
                changed = true;
            }
        }
        if (changed) {
            // saveAndFlush: архив должен увидеть уже инкрементированный @Version.
            GoldenRecord saved = goldenRecordRepository.saveAndFlush(gr);
            fieldMetaRepository.saveAll(touched);
            snapshotter.archive(saved);
        }
    }

    private TentativeItemView toView(TentativeGoldenRecord tentative) {
        GoldenRecord scratch = scratchFrom(tentative);
        Integer sourceTrust = sourceRepository.findBySourceName(tentative.getSourceName())
                .map(Source::getTrustLevel).orElse(null);

        GoldenRecord gr = tentative.getGrClientId() == null
                ? null
                : goldenRecordRepository.findById(tentative.getGrClientId()).orElse(null);

        String personName = personName(gr != null ? gr : scratch);
        List<TentativeConflictField> conflicts = List.of();
        if (gr != null) {
            Map<String, GoldenRecordFieldMeta> metas = fieldMetaRepository
                    .findByIdGrClientId(gr.getGrClientId()).stream()
                    .collect(Collectors.toMap(m -> m.getId().getFieldName(), Function.identity()));
            conflicts = mapper.knownFieldNames().stream()
                    .map(field -> {
                        Object incoming = mapper.readFromEntity(scratch, field);
                        Object current = mapper.readFromEntity(gr, field);
                        if (incoming == null || Objects.equals(incoming, current)) {
                            return null;
                        }
                        GoldenRecordFieldMeta meta = metas.get(field);
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

        return new TentativeItemView(
                tentative.getId(),
                tentative.getReason().name(),
                tentative.getGrClientId(),
                tentative.getSourceName(),
                sourceTrust,
                personName,
                tentative.getCreatedAt() == null ? null : tentative.getCreatedAt().toString(),
                conflicts,
                snapshot);
    }

    private GoldenRecord scratchFrom(TentativeGoldenRecord tentative) {
        GoldenRecord scratch = new GoldenRecord();
        BeanUtils.copyProperties(tentative, scratch, "grClientId", "version", "createdAt", "updatedAt");
        return scratch;
    }

    private static String personName(GoldenRecord gr) {
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{gr.getGrLastName(), gr.getGrFirstName(), gr.getGrMiddleName()}) {
            if (part != null && !part.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(part);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
