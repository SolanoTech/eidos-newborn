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

package com.solano.eidoscore.service.mapper;

import com.solano.eidoscore.engine.RecordFieldAccess;
import com.solano.eidoscore.entity.GoldenRecord;
import com.solano.shared.dto.GoldenRecordDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Маппер DTO ↔ {@link GoldenRecord}. Дополнительно служит единым реестром
 * имён полей, которые участвуют в provenance ({@code golden_record_field_meta}).
 * Имя поля в БД — Java field name (например {@code grMobilePhoneMain}).
 *
 * <p>Типы DTO и entity совпадают (LocalDate, Gender, String), поэтому
 * конвертация не нужна — handler'ы просто вызывают getter/setter.</p>
 */
@Component
public class GoldenRecordMapper implements RecordFieldAccess<GoldenRecordDto, GoldenRecord> {

    private static final Map<String, FieldAccessor<?>> ACCESSORS = buildAccessors();

    /**
     * Копирует все известные поля из {@code dto} в {@code entity}. Возвращает
     * список имён скопированных полей — используется creator'ом для записи
     * {@code field_meta}.
     */
    public List<String> copyToEntity(GoldenRecordDto dto, GoldenRecord entity) {
        ACCESSORS.values().forEach(a -> a.copy(dto, entity));
        return List.copyOf(ACCESSORS.keySet());
    }

    /** Копирует одно поле по имени. No-op если поле не зарегистрировано. */
    public void copyField(GoldenRecordDto dto, GoldenRecord entity, String fieldName) {
        FieldAccessor<?> a = ACCESSORS.get(fieldName);
        if (a != null) {
            a.copy(dto, entity);
        }
    }

    public Object readFromDto(GoldenRecordDto dto, String fieldName) {
        FieldAccessor<?> a = ACCESSORS.get(fieldName);
        return a == null ? null : a.fromDto(dto);
    }

    public Object readFromEntity(GoldenRecord entity, String fieldName) {
        FieldAccessor<?> a = ACCESSORS.get(fieldName);
        return a == null ? null : a.fromEntity(entity);
    }

    /** Копирует одно поле между двумя entity. No-op если поле не зарегистрировано. */
    public void copyEntityField(GoldenRecord from, GoldenRecord to, String fieldName) {
        FieldAccessor<?> a = ACCESSORS.get(fieldName);
        if (a != null) {
            a.copyBetweenEntities(from, to);
        }
    }

    public boolean knowsField(String fieldName) {
        return ACCESSORS.containsKey(fieldName);
    }

    public Set<String> knownFieldNames() {
        return Collections.unmodifiableSet(ACCESSORS.keySet());
    }

    // --------------------------------------------------------------------
    // Internals
    // --------------------------------------------------------------------

    private record FieldAccessor<T>(
            Function<GoldenRecordDto, T> dtoGet,
            Function<GoldenRecord, T> entityGet,
            BiConsumer<GoldenRecord, T> entitySet
    ) {
        void copy(GoldenRecordDto dto, GoldenRecord entity) {
            entitySet.accept(entity, dtoGet.apply(dto));
        }
        void copyBetweenEntities(GoldenRecord from, GoldenRecord to) {
            entitySet.accept(to, entityGet.apply(from));
        }
        T fromDto(GoldenRecordDto dto) { return dtoGet.apply(dto); }
        T fromEntity(GoldenRecord entity) { return entityGet.apply(entity); }
    }

    private static <T> FieldAccessor<T> acc(
            Function<GoldenRecordDto, T> dtoGet,
            Function<GoldenRecord, T> entityGet,
            BiConsumer<GoldenRecord, T> entitySet
    ) {
        return new FieldAccessor<>(dtoGet, entityGet, entitySet);
    }

    private static Map<String, FieldAccessor<?>> buildAccessors() {
        Map<String, FieldAccessor<?>> m = new LinkedHashMap<>();

        // contacts
        m.put("grMobilePhoneMain", acc(GoldenRecordDto::getGrMobilePhoneMain,
                GoldenRecord::getGrMobilePhoneMain, GoldenRecord::setGrMobilePhoneMain));
        m.put("grContactsEmail", acc(GoldenRecordDto::getGrContactsEmail,
                GoldenRecord::getGrContactsEmail, GoldenRecord::setGrContactsEmail));

        // name
        m.put("grFirstName", acc(GoldenRecordDto::getGrFirstName,
                GoldenRecord::getGrFirstName, GoldenRecord::setGrFirstName));
        m.put("grMiddleName", acc(GoldenRecordDto::getGrMiddleName,
                GoldenRecord::getGrMiddleName, GoldenRecord::setGrMiddleName));
        m.put("grLastName", acc(GoldenRecordDto::getGrLastName,
                GoldenRecord::getGrLastName, GoldenRecord::setGrLastName));

        // identification
        m.put("grPinfl", acc(GoldenRecordDto::getGrPinfl,
                GoldenRecord::getGrPinfl, GoldenRecord::setGrPinfl));
        m.put("grGender", acc(GoldenRecordDto::getGrGender,
                GoldenRecord::getGrGender, GoldenRecord::setGrGender));

        // birth
        m.put("grBirthDate", acc(GoldenRecordDto::getGrBirthDate,
                GoldenRecord::getGrBirthDate, GoldenRecord::setGrBirthDate));
        m.put("grBirthPlace", acc(GoldenRecordDto::getGrBirthPlace,
                GoldenRecord::getGrBirthPlace, GoldenRecord::setGrBirthPlace));
        m.put("grBirthCountry", acc(GoldenRecordDto::getGrBirthCountry,
                GoldenRecord::getGrBirthCountry, GoldenRecord::setGrBirthCountry));
        m.put("grBirthCountryId", acc(GoldenRecordDto::getGrBirthCountryId,
                GoldenRecord::getGrBirthCountryId, GoldenRecord::setGrBirthCountryId));

        // nationality / citizenship
        m.put("grNationality", acc(GoldenRecordDto::getGrNationality,
                GoldenRecord::getGrNationality, GoldenRecord::setGrNationality));
        m.put("grNationalityId", acc(GoldenRecordDto::getGrNationalityId,
                GoldenRecord::getGrNationalityId, GoldenRecord::setGrNationalityId));
        m.put("grCitizenship", acc(GoldenRecordDto::getGrCitizenship,
                GoldenRecord::getGrCitizenship, GoldenRecord::setGrCitizenship));
        m.put("grCitizenshipId", acc(GoldenRecordDto::getGrCitizenshipId,
                GoldenRecord::getGrCitizenshipId, GoldenRecord::setGrCitizenshipId));

        // document
        m.put("grDocPassData", acc(GoldenRecordDto::getGrDocPassData,
                GoldenRecord::getGrDocPassData, GoldenRecord::setGrDocPassData));
        m.put("grDocIssuedBy", acc(GoldenRecordDto::getGrDocIssuedBy,
                GoldenRecord::getGrDocIssuedBy, GoldenRecord::setGrDocIssuedBy));
        m.put("grDocIssuedById", acc(GoldenRecordDto::getGrDocIssuedById,
                GoldenRecord::getGrDocIssuedById, GoldenRecord::setGrDocIssuedById));
        m.put("grDocIssuedDate", acc(GoldenRecordDto::getGrDocIssuedDate,
                GoldenRecord::getGrDocIssuedDate, GoldenRecord::setGrDocIssuedDate));
        m.put("grDocExpiryDate", acc(GoldenRecordDto::getGrDocExpiryDate,
                GoldenRecord::getGrDocExpiryDate, GoldenRecord::setGrDocExpiryDate));

        // free-text addresses
        m.put("grAddrPermanentAddress", acc(GoldenRecordDto::getGrAddrPermanentAddress,
                GoldenRecord::getGrAddrPermanentAddress, GoldenRecord::setGrAddrPermanentAddress));
        m.put("grAddrTemporaryAddress", acc(GoldenRecordDto::getGrAddrTemporaryAddress,
                GoldenRecord::getGrAddrTemporaryAddress, GoldenRecord::setGrAddrTemporaryAddress));

        // permanent registration
        m.put("grAddrPermRegMfy", acc(GoldenRecordDto::getGrAddrPermRegMfy,
                GoldenRecord::getGrAddrPermRegMfy, GoldenRecord::setGrAddrPermRegMfy));
        m.put("grAddrPermRegMfyId", acc(GoldenRecordDto::getGrAddrPermRegMfyId,
                GoldenRecord::getGrAddrPermRegMfyId, GoldenRecord::setGrAddrPermRegMfyId));
        m.put("grAddrPermRegRegion", acc(GoldenRecordDto::getGrAddrPermRegRegion,
                GoldenRecord::getGrAddrPermRegRegion, GoldenRecord::setGrAddrPermRegRegion));
        m.put("grAddrPermRegAddress", acc(GoldenRecordDto::getGrAddrPermRegAddress,
                GoldenRecord::getGrAddrPermRegAddress, GoldenRecord::setGrAddrPermRegAddress));
        m.put("grAddrPermRegCountry", acc(GoldenRecordDto::getGrAddrPermRegCountry,
                GoldenRecord::getGrAddrPermRegCountry, GoldenRecord::setGrAddrPermRegCountry));
        m.put("grAddrPermRegCadastre", acc(GoldenRecordDto::getGrAddrPermRegCadastre,
                GoldenRecord::getGrAddrPermRegCadastre, GoldenRecord::setGrAddrPermRegCadastre));
        m.put("grAddrPermRegDistrict", acc(GoldenRecordDto::getGrAddrPermRegDistrict,
                GoldenRecord::getGrAddrPermRegDistrict, GoldenRecord::setGrAddrPermRegDistrict));
        m.put("grAddrPermRegRegionId", acc(GoldenRecordDto::getGrAddrPermRegRegionId,
                GoldenRecord::getGrAddrPermRegRegionId, GoldenRecord::setGrAddrPermRegRegionId));
        m.put("grAddrPermRegCountryId", acc(GoldenRecordDto::getGrAddrPermRegCountryId,
                GoldenRecord::getGrAddrPermRegCountryId, GoldenRecord::setGrAddrPermRegCountryId));
        m.put("grAddrPermRegDistrictId", acc(GoldenRecordDto::getGrAddrPermRegDistrictId,
                GoldenRecord::getGrAddrPermRegDistrictId, GoldenRecord::setGrAddrPermRegDistrictId));
        m.put("grAddrPermRegRegistrationDate", acc(
                GoldenRecordDto::getGrAddrPermRegRegistrationDate,
                GoldenRecord::getGrAddrPermRegRegistrationDate,
                GoldenRecord::setGrAddrPermRegRegistrationDate));

        // temporary registration
        m.put("grAddrTempRegMfy", acc(GoldenRecordDto::getGrAddrTempRegMfy,
                GoldenRecord::getGrAddrTempRegMfy, GoldenRecord::setGrAddrTempRegMfy));
        m.put("grAddrTempRegMfyId", acc(GoldenRecordDto::getGrAddrTempRegMfyId,
                GoldenRecord::getGrAddrTempRegMfyId, GoldenRecord::setGrAddrTempRegMfyId));
        m.put("grAddrTempRegRegion", acc(GoldenRecordDto::getGrAddrTempRegRegion,
                GoldenRecord::getGrAddrTempRegRegion, GoldenRecord::setGrAddrTempRegRegion));
        m.put("grAddrTempRegAddress", acc(GoldenRecordDto::getGrAddrTempRegAddress,
                GoldenRecord::getGrAddrTempRegAddress, GoldenRecord::setGrAddrTempRegAddress));
        m.put("grAddrTempRegDistrict", acc(GoldenRecordDto::getGrAddrTempRegDistrict,
                GoldenRecord::getGrAddrTempRegDistrict, GoldenRecord::setGrAddrTempRegDistrict));
        m.put("grAddrTempRegDateFrom", acc(
                GoldenRecordDto::getGrAddrTempRegDateFrom,
                GoldenRecord::getGrAddrTempRegDateFrom,
                GoldenRecord::setGrAddrTempRegDateFrom));
        m.put("grAddrTempRegDateTill", acc(
                GoldenRecordDto::getGrAddrTempRegDateTill,
                GoldenRecord::getGrAddrTempRegDateTill,
                GoldenRecord::setGrAddrTempRegDateTill));
        m.put("grAddrTempRegRegionId", acc(GoldenRecordDto::getGrAddrTempRegRegionId,
                GoldenRecord::getGrAddrTempRegRegionId, GoldenRecord::setGrAddrTempRegRegionId));
        m.put("grAddrTempRegDistrictId", acc(GoldenRecordDto::getGrAddrTempRegDistrictId,
                GoldenRecord::getGrAddrTempRegDistrictId, GoldenRecord::setGrAddrTempRegDistrictId));

        return Collections.unmodifiableMap(m);
    }
}
