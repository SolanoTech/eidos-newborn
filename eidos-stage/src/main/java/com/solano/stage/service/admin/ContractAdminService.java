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

package com.solano.stage.service.admin;

import com.solano.shared.dto.GoldenRecordDto;
import com.solano.shared.dto.RecordFieldCatalog;
import com.solano.stage.entity.registry.FieldDataType;
import com.solano.stage.entity.registry.EntityType;
import com.solano.stage.entity.registry.Source;
import com.solano.stage.entity.registry.SourceContract;
import com.solano.stage.entity.registry.SourceField;
import com.solano.stage.exception.BadRequestException;
import com.solano.stage.exception.NotFoundException;
import com.solano.stage.repository.registry.SourceContractRepository;
import com.solano.stage.repository.registry.SourceFieldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Управление дата-контрактом источника (конструктором): мета-информация и
 * CRUD полей. Целевое поле каждого правила проверяется на принадлежность
 * Золотой записи через {@link RecordFieldCatalog}.
 */
@Service
public class ContractAdminService {

    private final SourceAdminService sourceAdminService;
    private final SourceContractRepository contractRepository;
    private final SourceFieldRepository fieldRepository;

    public ContractAdminService(
            SourceAdminService sourceAdminService,
            SourceContractRepository contractRepository,
            SourceFieldRepository fieldRepository
    ) {
        this.sourceAdminService = sourceAdminService;
        this.contractRepository = contractRepository;
        this.fieldRepository = fieldRepository;
    }

    @Transactional(readOnly = true)
    public SourceContract getBySourceId(Long sourceId, EntityType entityType) {
        SourceContract contract = contractRepository.findBySourceIdAndEntityType(sourceId, entityType)
                .orElseThrow(() -> new NotFoundException(
                        "No " + entityType + " contract for source " + sourceId));
        // Инициализируем граф внутри сессии: source (LAZY @OneToOne) и поля
        // нужны при маппинге в DTO в контроллере, где сессия уже закрыта
        // (open-in-view выключен). Иначе — LazyInitializationException.
        contract.getSource().getCode();
        contract.getFields().forEach(field -> field.getValueMap().size());
        return contract;
    }

    /** Создаёт контракт при отсутствии и обновляет мета-поле идентификатора клиента. */
    @Transactional
    public SourceContract upsertContractMeta(Long sourceId, EntityType entityType, String clientIdentifierField) {
        SourceContract contract = contractRepository
                .findBySourceIdAndEntityType(sourceId, entityType).orElse(null);
        if (contract == null) {
            Source source = sourceAdminService.findById(sourceId);
            contract = SourceContract.builder().source(source).entityType(entityType).build();
        }
        contract.setClientIdentifierField(clientIdentifierField);
        bumpVersion(contract);
        SourceContract saved = contractRepository.save(contract);
        // Инициализируем LAZY-граф до выхода из транзакции (см. getBySourceId).
        saved.getSource().getCode();
        saved.getFields().forEach(field -> field.getValueMap().size());
        return saved;
    }

    @Transactional
    public SourceField addField(Long sourceId, EntityType entityType, FieldSpec spec) {
        SourceContract contract = contractRepository.findBySourceIdAndEntityType(sourceId, entityType)
                .orElseGet(() -> {
                    Source source = sourceAdminService.findById(sourceId);
                    return SourceContract.builder().source(source).entityType(entityType).build();
                });
        validateTarget(spec.targetGrField(), entityType);

        SourceField field = toField(spec);
        contract.addField(field);
        bumpVersion(contract);
        // saveAndFlush — чтобы IDENTITY-id поля присвоился и попал в ответ.
        contractRepository.saveAndFlush(contract);
        return field;
    }

    @Transactional
    public SourceField updateField(Long fieldId, FieldSpec spec) {
        SourceField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new NotFoundException("Field " + fieldId + " not found"));
        validateTarget(spec.targetGrField(), field.getContract().getEntityType());
        applySpec(field, spec);
        bumpVersion(field.getContract());
        return fieldRepository.save(field);
    }

    @Transactional
    public void deleteField(Long fieldId) {
        SourceField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new NotFoundException("Field " + fieldId + " not found"));
        bumpVersion(field.getContract());
        fieldRepository.delete(field);
    }

    /** Инкремент версии контракта при любом изменении (null = 1). */
    private void bumpVersion(SourceContract contract) {
        if (contract != null) {
            int current = contract.getVersion() == null ? 1 : contract.getVersion();
            contract.setVersion(current + 1);
            contractRepository.save(contract);
        }
    }

    private void validateTarget(String targetGrField, EntityType entityType) {
        if (!RecordFieldCatalog.of(contractClass(entityType)).isValidTarget(targetGrField)) {
            throw new BadRequestException(
                    "Unknown Golden Record field for " + entityType + ": " + targetGrField);
        }
    }

    /** Контракт Золотой записи, поля которой доступны как целевые для типа. */
    public static Class<?> contractClass(EntityType entityType) {
        return entityType == EntityType.LEGAL_ENTITY
                ? com.solano.shared.dto.LegalEntityGoldenRecordDto.class
                : GoldenRecordDto.class;
    }

    private SourceField toField(FieldSpec spec) {
        SourceField field = new SourceField();
        applySpec(field, spec);
        return field;
    }

    private void applySpec(SourceField field, FieldSpec spec) {
        field.setSourceFieldName(spec.sourceFieldName());
        field.setTargetGrField(spec.targetGrField());
        field.setDataType(spec.dataType());
        field.setRequired(spec.required() != null && spec.required());
        field.setSourceDateFormat(spec.sourceDateFormat());
        field.setValidationRegex(spec.validationRegex());
        field.setDefaultValue(spec.defaultValue());
        field.setOrdering(spec.ordering() == null ? 0 : spec.ordering());
        field.setValueMap(spec.valueMap() == null ? Map.of() : spec.valueMap());
    }

    /** Нормализованный набор атрибутов поля, общий для create/update. */
    public record FieldSpec(
            String sourceFieldName,
            String targetGrField,
            FieldDataType dataType,
            Boolean required,
            String sourceDateFormat,
            String validationRegex,
            String defaultValue,
            Integer ordering,
            Map<String, String> valueMap
    ) {
    }
}
