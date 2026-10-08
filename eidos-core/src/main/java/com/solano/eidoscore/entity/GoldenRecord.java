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

package com.solano.eidoscore.entity;

import com.solano.shared.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Golden Record — итоговая сущность SCV. Поля и колонки строго по дата-контракту
 * shared-library; в БД хранится snake_case. Технические поля: {@link Version}
 * для оптимистической блокировки, {@code created_at} / {@code updated_at}
 * управляются Hibernate.
 */
@Entity
@Table(name = "golden_record")
@EntityListeners(BlindIndexListener.class)
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldenRecord {

    @Id
    @Column(name = "gr_client_id", nullable = false, updatable = false)
    private String grClientId;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Состояние согласия на обработку ПДн, каким его знает core: заполняется
     * потребителем событий сервиса согласий. {@code null} — ещё не проверяли.
     *
     * <p>Пишется в обход {@link Version} и {@code updated_at} — отдельным
     * UPDATE (см. {@code GoldenRecordRepository.updateConsentState}): смена
     * согласия не бизнес-изменение карточки и не должна двигать её версию.</p>
     */
    @Column(name = "pd_consent_active")
    private Boolean pdConsentActive;

    /** Последний день действия согласия — позволяет считать истечение локально. */
    @Column(name = "pd_consent_valid_to")
    private LocalDate pdConsentValidTo;

    /** Когда core последний раз сверялся с сервисом согласий. */
    @Column(name = "pd_consent_checked_at")
    private LocalDateTime pdConsentCheckedAt;

    /**
     * Слепой индекс: HMAC над теми же комбинациями полей, по которым идёт
     * поиск и матчинг. Заполняется всегда — см. {@link BlindIndexListener}.
     */
    @Column(name = "pd_bi_identity", length = 64)
    private String pdBiIdentity;

    @Column(name = "pd_bi_identity_doc", length = 64)
    private String pdBiIdentityDoc;

    @Column(name = "pd_bi_pinfl", length = 64)
    private String pdBiPinfl;

    /** Версия ключа, которым посчитан индекс: ротация требует пересчёта. */
    @Column(name = "pd_bi_key_version")
    private Integer pdBiKeyVersion;

    /**
     * Когда персональные значения заменены токенами. {@code null} — значения
     * хранятся открыто.
     */
    @Column(name = "pd_tokenized_at")
    private LocalDateTime pdTokenizedAt;

    /** Обезличена ли запись: значения полей заменены токенами хранилища. */
    public boolean isTokenized() {
        return pdTokenizedAt != null;
    }

    // ===== Контактные данные =====

    @Column(name = "gr_mobile_phone_main", nullable = false, length = 64)
    private String grMobilePhoneMain;

    @Column(name = "gr_contacts_email")
    private String grContactsEmail;

    // ===== ФИО =====

    @Column(name = "gr_first_name", nullable = false)
    private String grFirstName;

    @Column(name = "gr_middle_name")
    private String grMiddleName;

    @Column(name = "gr_last_name", nullable = false)
    private String grLastName;

    // ===== Идентификация =====

    @Column(name = "gr_pinfl", nullable = false, length = 64, unique = true)
    private String grPinfl;

    @Enumerated(EnumType.STRING)
    @Column(name = "gr_gender", nullable = false, length = 8)
    private Gender grGender;

    // ===== Рождение =====

    @Column(name = "gr_birth_date", nullable = false)
    private LocalDate grBirthDate;

    @Column(name = "gr_birth_place")
    private String grBirthPlace;

    @Column(name = "gr_birth_country")
    private String grBirthCountry;

    @Column(name = "gr_birth_country_id")
    private String grBirthCountryId;

    // ===== Национальность / гражданство =====

    @Column(name = "gr_nationality")
    private String grNationality;

    @Column(name = "gr_nationality_id")
    private String grNationalityId;

    @Column(name = "gr_citizenship", nullable = false)
    private String grCitizenship;

    @Column(name = "gr_citizenship_id", nullable = false)
    private String grCitizenshipId;

    // ===== Документ =====

    @Column(name = "gr_doc_pass_data", nullable = false, length = 64)
    private String grDocPassData;

    @Column(name = "gr_doc_issued_by", nullable = false)
    private String grDocIssuedBy;

    @Column(name = "gr_doc_issued_by_id", nullable = false)
    private String grDocIssuedById;

    @Column(name = "gr_doc_issued_date", nullable = false)
    private LocalDate grDocIssuedDate;

    @Column(name = "gr_doc_expiry_date")
    private LocalDate grDocExpiryDate;

    // ===== Адреса (свободный текст) =====

    @Column(name = "gr_addr_permanent_address")
    private String grAddrPermanentAddress;

    @Column(name = "gr_addr_temporary_address")
    private String grAddrTemporaryAddress;

    // ===== Постоянная регистрация =====

    @Column(name = "gr_addr_perm_reg_mfy")
    private String grAddrPermRegMfy;

    @Column(name = "gr_addr_perm_reg_mfy_id")
    private String grAddrPermRegMfyId;

    @Column(name = "gr_addr_perm_reg_region")
    private String grAddrPermRegRegion;

    @Column(name = "gr_addr_perm_reg_address")
    private String grAddrPermRegAddress;

    @Column(name = "gr_addr_perm_reg_country")
    private String grAddrPermRegCountry;

    @Column(name = "gr_addr_perm_reg_cadastre")
    private String grAddrPermRegCadastre;

    @Column(name = "gr_addr_perm_reg_district")
    private String grAddrPermRegDistrict;

    @Column(name = "gr_addr_perm_reg_region_id")
    private String grAddrPermRegRegionId;

    @Column(name = "gr_addr_perm_reg_country_id")
    private String grAddrPermRegCountryId;

    @Column(name = "gr_addr_perm_reg_district_id")
    private String grAddrPermRegDistrictId;

    @Column(name = "gr_addr_perm_reg_registration_date")
    private LocalDate grAddrPermRegRegistrationDate;

    // ===== Временная регистрация =====

    @Column(name = "gr_addr_temp_reg_mfy")
    private String grAddrTempRegMfy;

    @Column(name = "gr_addr_temp_reg_mfy_id")
    private String grAddrTempRegMfyId;

    @Column(name = "gr_addr_temp_reg_region")
    private String grAddrTempRegRegion;

    @Column(name = "gr_addr_temp_reg_address")
    private String grAddrTempRegAddress;

    @Column(name = "gr_addr_temp_reg_district")
    private String grAddrTempRegDistrict;

    @Column(name = "gr_addr_temp_reg_date_from")
    private LocalDate grAddrTempRegDateFrom;

    @Column(name = "gr_addr_temp_reg_date_till")
    private LocalDate grAddrTempRegDateTill;

    @Column(name = "gr_addr_temp_reg_region_id")
    private String grAddrTempRegRegionId;

    @Column(name = "gr_addr_temp_reg_district_id")
    private String grAddrTempRegDistrictId;
}
