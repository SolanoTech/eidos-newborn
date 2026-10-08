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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Общие бизнес-поля Golden Record (43 штуки) для сущностей-«снимков»:
 * {@link GoldenRecordArchive} и {@link TentativeGoldenRecord}. Сам
 * {@link GoldenRecord} от этого класса <b>не наследуется</b> — у него
 * собственное объявление с более строгой nullability и {@code unique = true}
 * на PINFL. Здесь все поля сделаны nullable, потому что:
 * <ul>
 *   <li>в Tentative запись может прийти из неизвестного источника с любым
 *       набором заполненных полей;</li>
 *   <li>Archive хранит снимки в той форме, в которой они были на момент
 *       снимка — структура заведомо совпадает с GR, но БД не должен
 *       сопротивляться, если когда-то в будущем GR смягчит constraints.</li>
 * </ul>
 *
 * <p>Имена колонок совпадают с {@link GoldenRecord}, чтобы
 * {@code BeanUtils.copyProperties} мог копировать поле-в-поле без явных
 * mapping'ов.</p>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class GoldenRecordSnapshotFields {

    // ===== Контактные данные =====

    @Column(name = "gr_mobile_phone_main", length = 12)
    private String grMobilePhoneMain;

    @Column(name = "gr_contacts_email")
    private String grContactsEmail;

    // ===== ФИО =====

    @Column(name = "gr_first_name")
    private String grFirstName;

    @Column(name = "gr_middle_name")
    private String grMiddleName;

    @Column(name = "gr_last_name")
    private String grLastName;

    // ===== Идентификация =====

    @Column(name = "gr_pinfl", length = 14)
    private String grPinfl;

    @Enumerated(EnumType.STRING)
    @Column(name = "gr_gender", length = 8)
    private Gender grGender;

    // ===== Рождение =====

    @Column(name = "gr_birth_date")
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

    @Column(name = "gr_citizenship")
    private String grCitizenship;

    @Column(name = "gr_citizenship_id")
    private String grCitizenshipId;

    // ===== Документ =====

    @Column(name = "gr_doc_pass_data", length = 9)
    private String grDocPassData;

    @Column(name = "gr_doc_issued_by")
    private String grDocIssuedBy;

    @Column(name = "gr_doc_issued_by_id")
    private String grDocIssuedById;

    @Column(name = "gr_doc_issued_date")
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
