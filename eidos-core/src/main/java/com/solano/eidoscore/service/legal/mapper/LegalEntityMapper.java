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

package com.solano.eidoscore.service.legal.mapper;

import com.solano.eidoscore.engine.RecordFieldAccess;
import com.solano.eidoscore.entity.legal.LegalEntityRecord;
import com.solano.shared.dto.LegalEntityGoldenRecordDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Реестр аксессоров полей записи юрлица — зеркало {@code GoldenRecordMapper}
 * для второй вертикали. Единственное место, где перечислены конкретные поля
 * контракта юрлица; движок ходит сюда только через
 * {@link RecordFieldAccess}.
 *
 * <p>Ключ карты — Java-имя поля ({@code grInn}), оно же попадает в
 * {@code field_meta} и в ответы провенанса.</p>
 */
@Component
public class LegalEntityMapper
        implements RecordFieldAccess<LegalEntityGoldenRecordDto, LegalEntityRecord> {

    private static final Map<String, FieldAccessor<?>> ACCESSORS = buildAccessors();

    @Override
    public boolean knowsField(String fieldName) {
        return ACCESSORS.containsKey(fieldName);
    }

    @Override
    public Set<String> knownFieldNames() {
        return Collections.unmodifiableSet(ACCESSORS.keySet());
    }

    @Override
    public List<String> copyToEntity(LegalEntityGoldenRecordDto dto, LegalEntityRecord entity) {
        ACCESSORS.values().forEach(a -> a.copy(dto, entity));
        return List.copyOf(ACCESSORS.keySet());
    }

    @Override
    public void copyField(LegalEntityGoldenRecordDto dto, LegalEntityRecord entity, String fieldName) {
        FieldAccessor<?> a = ACCESSORS.get(fieldName);
        if (a != null) {
            a.copy(dto, entity);
        }
    }

    @Override
    public Object readFromDto(LegalEntityGoldenRecordDto dto, String fieldName) {
        FieldAccessor<?> a = ACCESSORS.get(fieldName);
        return a == null ? null : a.fromDto(dto);
    }

    @Override
    public Object readFromEntity(LegalEntityRecord entity, String fieldName) {
        FieldAccessor<?> a = ACCESSORS.get(fieldName);
        return a == null ? null : a.fromEntity(entity);
    }

    @Override
    public void copyEntityField(LegalEntityRecord from, LegalEntityRecord to, String fieldName) {
        FieldAccessor<?> a = ACCESSORS.get(fieldName);
        if (a != null) {
            a.copyBetweenEntities(from, to);
        }
    }

    private record FieldAccessor<T>(
            Function<LegalEntityGoldenRecordDto, T> dtoGet,
            Function<LegalEntityRecord, T> entityGet,
            BiConsumer<LegalEntityRecord, T> entitySet
    ) {
        void copy(LegalEntityGoldenRecordDto dto, LegalEntityRecord entity) {
            entitySet.accept(entity, dtoGet.apply(dto));
        }

        void copyBetweenEntities(LegalEntityRecord from, LegalEntityRecord to) {
            entitySet.accept(to, entityGet.apply(from));
        }

        T fromDto(LegalEntityGoldenRecordDto dto) {
            return dtoGet.apply(dto);
        }

        T fromEntity(LegalEntityRecord entity) {
            return entityGet.apply(entity);
        }
    }

    private static <T> FieldAccessor<T> acc(
            Function<LegalEntityGoldenRecordDto, T> dtoGet,
            Function<LegalEntityRecord, T> entityGet,
            BiConsumer<LegalEntityRecord, T> entitySet
    ) {
        return new FieldAccessor<>(dtoGet, entityGet, entitySet);
    }

    private static Map<String, FieldAccessor<?>> buildAccessors() {
        Map<String, FieldAccessor<?>> m = new LinkedHashMap<>();

        m.put("grInn", acc(LegalEntityGoldenRecordDto::getGrInn,
                LegalEntityRecord::getGrInn, LegalEntityRecord::setGrInn));
        m.put("grFullName", acc(LegalEntityGoldenRecordDto::getGrFullName,
                LegalEntityRecord::getGrFullName, LegalEntityRecord::setGrFullName));
        m.put("grShortName", acc(LegalEntityGoldenRecordDto::getGrShortName,
                LegalEntityRecord::getGrShortName, LegalEntityRecord::setGrShortName));
        m.put("grOpfCode", acc(LegalEntityGoldenRecordDto::getGrOpfCode,
                LegalEntityRecord::getGrOpfCode, LegalEntityRecord::setGrOpfCode));
        m.put("grOpfName", acc(LegalEntityGoldenRecordDto::getGrOpfName,
                LegalEntityRecord::getGrOpfName, LegalEntityRecord::setGrOpfName));
        m.put("grOpfNameUz", acc(LegalEntityGoldenRecordDto::getGrOpfNameUz,
                LegalEntityRecord::getGrOpfNameUz, LegalEntityRecord::setGrOpfNameUz));
        m.put("grRegistrationDate", acc(LegalEntityGoldenRecordDto::getGrRegistrationDate,
                LegalEntityRecord::getGrRegistrationDate, LegalEntityRecord::setGrRegistrationDate));
        m.put("grRegistrationNumber", acc(LegalEntityGoldenRecordDto::getGrRegistrationNumber,
                LegalEntityRecord::getGrRegistrationNumber, LegalEntityRecord::setGrRegistrationNumber));
        m.put("grRegistrationAuthority", acc(LegalEntityGoldenRecordDto::getGrRegistrationAuthority,
                LegalEntityRecord::getGrRegistrationAuthority, LegalEntityRecord::setGrRegistrationAuthority));
        m.put("grStatutoryFund", acc(LegalEntityGoldenRecordDto::getGrStatutoryFund,
                LegalEntityRecord::getGrStatutoryFund, LegalEntityRecord::setGrStatutoryFund));
        m.put("grIsSmallBusiness", acc(LegalEntityGoldenRecordDto::getGrIsSmallBusiness,
                LegalEntityRecord::getGrIsSmallBusiness, LegalEntityRecord::setGrIsSmallBusiness));
        m.put("grActivityStateCode", acc(LegalEntityGoldenRecordDto::getGrActivityStateCode,
                LegalEntityRecord::getGrActivityStateCode, LegalEntityRecord::setGrActivityStateCode));
        m.put("grActivityStateDetailId", acc(LegalEntityGoldenRecordDto::getGrActivityStateDetailId,
                LegalEntityRecord::getGrActivityStateDetailId, LegalEntityRecord::setGrActivityStateDetailId));
        m.put("grActivityStateName", acc(LegalEntityGoldenRecordDto::getGrActivityStateName,
                LegalEntityRecord::getGrActivityStateName, LegalEntityRecord::setGrActivityStateName));
        m.put("grIsActive", acc(LegalEntityGoldenRecordDto::getGrIsActive,
                LegalEntityRecord::getGrIsActive, LegalEntityRecord::setGrIsActive));
        m.put("grIsBankrupt", acc(LegalEntityGoldenRecordDto::getGrIsBankrupt,
                LegalEntityRecord::getGrIsBankrupt, LegalEntityRecord::setGrIsBankrupt));
        m.put("grOkedCode", acc(LegalEntityGoldenRecordDto::getGrOkedCode,
                LegalEntityRecord::getGrOkedCode, LegalEntityRecord::setGrOkedCode));
        m.put("grOkedName", acc(LegalEntityGoldenRecordDto::getGrOkedName,
                LegalEntityRecord::getGrOkedName, LegalEntityRecord::setGrOkedName));
        m.put("grOkedNameUz", acc(LegalEntityGoldenRecordDto::getGrOkedNameUz,
                LegalEntityRecord::getGrOkedNameUz, LegalEntityRecord::setGrOkedNameUz));
        m.put("grSooguCode", acc(LegalEntityGoldenRecordDto::getGrSooguCode,
                LegalEntityRecord::getGrSooguCode, LegalEntityRecord::setGrSooguCode));
        m.put("grSooguName", acc(LegalEntityGoldenRecordDto::getGrSooguName,
                LegalEntityRecord::getGrSooguName, LegalEntityRecord::setGrSooguName));
        m.put("grKfsCode", acc(LegalEntityGoldenRecordDto::getGrKfsCode,
                LegalEntityRecord::getGrKfsCode, LegalEntityRecord::setGrKfsCode));
        m.put("grKfsName", acc(LegalEntityGoldenRecordDto::getGrKfsName,
                LegalEntityRecord::getGrKfsName, LegalEntityRecord::setGrKfsName));
        m.put("grBusinessTypeId", acc(LegalEntityGoldenRecordDto::getGrBusinessTypeId,
                LegalEntityRecord::getGrBusinessTypeId, LegalEntityRecord::setGrBusinessTypeId));
        m.put("grBusinessTypeName", acc(LegalEntityGoldenRecordDto::getGrBusinessTypeName,
                LegalEntityRecord::getGrBusinessTypeName, LegalEntityRecord::setGrBusinessTypeName));
        m.put("grAddressFull", acc(LegalEntityGoldenRecordDto::getGrAddressFull,
                LegalEntityRecord::getGrAddressFull, LegalEntityRecord::setGrAddressFull));
        m.put("grSoatoCode", acc(LegalEntityGoldenRecordDto::getGrSoatoCode,
                LegalEntityRecord::getGrSoatoCode, LegalEntityRecord::setGrSoatoCode));
        m.put("grSoatoName", acc(LegalEntityGoldenRecordDto::getGrSoatoName,
                LegalEntityRecord::getGrSoatoName, LegalEntityRecord::setGrSoatoName));
        m.put("grRegionCode", acc(LegalEntityGoldenRecordDto::getGrRegionCode,
                LegalEntityRecord::getGrRegionCode, LegalEntityRecord::setGrRegionCode));
        m.put("grRegionName", acc(LegalEntityGoldenRecordDto::getGrRegionName,
                LegalEntityRecord::getGrRegionName, LegalEntityRecord::setGrRegionName));
        m.put("grDistrictCode", acc(LegalEntityGoldenRecordDto::getGrDistrictCode,
                LegalEntityRecord::getGrDistrictCode, LegalEntityRecord::setGrDistrictCode));
        m.put("grDistrictName", acc(LegalEntityGoldenRecordDto::getGrDistrictName,
                LegalEntityRecord::getGrDistrictName, LegalEntityRecord::setGrDistrictName));
        m.put("grVillageCode", acc(LegalEntityGoldenRecordDto::getGrVillageCode,
                LegalEntityRecord::getGrVillageCode, LegalEntityRecord::setGrVillageCode));
        m.put("grVillageName", acc(LegalEntityGoldenRecordDto::getGrVillageName,
                LegalEntityRecord::getGrVillageName, LegalEntityRecord::setGrVillageName));
        m.put("grStreetName", acc(LegalEntityGoldenRecordDto::getGrStreetName,
                LegalEntityRecord::getGrStreetName, LegalEntityRecord::setGrStreetName));
        m.put("grHouse", acc(LegalEntityGoldenRecordDto::getGrHouse,
                LegalEntityRecord::getGrHouse, LegalEntityRecord::setGrHouse));
        m.put("grFlat", acc(LegalEntityGoldenRecordDto::getGrFlat,
                LegalEntityRecord::getGrFlat, LegalEntityRecord::setGrFlat));
        m.put("grPostcode", acc(LegalEntityGoldenRecordDto::getGrPostcode,
                LegalEntityRecord::getGrPostcode, LegalEntityRecord::setGrPostcode));
        m.put("grEmail", acc(LegalEntityGoldenRecordDto::getGrEmail,
                LegalEntityRecord::getGrEmail, LegalEntityRecord::setGrEmail));
        m.put("grEmailStatus", acc(LegalEntityGoldenRecordDto::getGrEmailStatus,
                LegalEntityRecord::getGrEmailStatus, LegalEntityRecord::setGrEmailStatus));
        m.put("grPhones", acc(LegalEntityGoldenRecordDto::getGrPhones,
                LegalEntityRecord::getGrPhones, LegalEntityRecord::setGrPhones));
        m.put("grDirectorName", acc(LegalEntityGoldenRecordDto::getGrDirectorName,
                LegalEntityRecord::getGrDirectorName, LegalEntityRecord::setGrDirectorName));
        m.put("grDirectorUuid", acc(LegalEntityGoldenRecordDto::getGrDirectorUuid,
                LegalEntityRecord::getGrDirectorUuid, LegalEntityRecord::setGrDirectorUuid));
        m.put("grFounders", acc(LegalEntityGoldenRecordDto::getGrFounders,
                LegalEntityRecord::getGrFounders, LegalEntityRecord::setGrFounders));
        m.put("grFoundersCount", acc(LegalEntityGoldenRecordDto::getGrFoundersCount,
                LegalEntityRecord::getGrFoundersCount, LegalEntityRecord::setGrFoundersCount));
        m.put("grTaxMode", acc(LegalEntityGoldenRecordDto::getGrTaxMode,
                LegalEntityRecord::getGrTaxMode, LegalEntityRecord::setGrTaxMode));
        m.put("grVatNumber", acc(LegalEntityGoldenRecordDto::getGrVatNumber,
                LegalEntityRecord::getGrVatNumber, LegalEntityRecord::setGrVatNumber));
        m.put("grIsVatPayer", acc(LegalEntityGoldenRecordDto::getGrIsVatPayer,
                LegalEntityRecord::getGrIsVatPayer, LegalEntityRecord::setGrIsVatPayer));
        m.put("grTrustRating", acc(LegalEntityGoldenRecordDto::getGrTrustRating,
                LegalEntityRecord::getGrTrustRating, LegalEntityRecord::setGrTrustRating));
        m.put("grTrustScore", acc(LegalEntityGoldenRecordDto::getGrTrustScore,
                LegalEntityRecord::getGrTrustScore, LegalEntityRecord::setGrTrustScore));
        m.put("grIsVatAbuser", acc(LegalEntityGoldenRecordDto::getGrIsVatAbuser,
                LegalEntityRecord::getGrIsVatAbuser, LegalEntityRecord::setGrIsVatAbuser));
        m.put("grIsDishonestExecutor", acc(LegalEntityGoldenRecordDto::getGrIsDishonestExecutor,
                LegalEntityRecord::getGrIsDishonestExecutor, LegalEntityRecord::setGrIsDishonestExecutor));
        m.put("grIsSupplier", acc(LegalEntityGoldenRecordDto::getGrIsSupplier,
                LegalEntityRecord::getGrIsSupplier, LegalEntityRecord::setGrIsSupplier));
        m.put("grCourtsTotal", acc(LegalEntityGoldenRecordDto::getGrCourtsTotal,
                LegalEntityRecord::getGrCourtsTotal, LegalEntityRecord::setGrCourtsTotal));
        m.put("grConnectionsTotal", acc(LegalEntityGoldenRecordDto::getGrConnectionsTotal,
                LegalEntityRecord::getGrConnectionsTotal, LegalEntityRecord::setGrConnectionsTotal));
        m.put("grLicensesTotal", acc(LegalEntityGoldenRecordDto::getGrLicensesTotal,
                LegalEntityRecord::getGrLicensesTotal, LegalEntityRecord::setGrLicensesTotal));
        m.put("grDealsCustomerTotal", acc(LegalEntityGoldenRecordDto::getGrDealsCustomerTotal,
                LegalEntityRecord::getGrDealsCustomerTotal, LegalEntityRecord::setGrDealsCustomerTotal));
        m.put("grDealsProviderTotal", acc(LegalEntityGoldenRecordDto::getGrDealsProviderTotal,
                LegalEntityRecord::getGrDealsProviderTotal, LegalEntityRecord::setGrDealsProviderTotal));
        m.put("grBuildingsTotal", acc(LegalEntityGoldenRecordDto::getGrBuildingsTotal,
                LegalEntityRecord::getGrBuildingsTotal, LegalEntityRecord::setGrBuildingsTotal));
        m.put("grCadastresTotal", acc(LegalEntityGoldenRecordDto::getGrCadastresTotal,
                LegalEntityRecord::getGrCadastresTotal, LegalEntityRecord::setGrCadastresTotal));

        return Collections.unmodifiableMap(m);
    }
}
