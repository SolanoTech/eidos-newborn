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

package com.solano.eidoscore.entity.legal;

import com.solano.shared.dto.LegalEntityGoldenRecordDto.Founder;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Общие бизнес-поля Золотой записи юрлица (60 штук) для сущностей-«снимков»:
 * {@link LegalEntityArchive} и {@link TentativeLegalEntity}. Сам
 * {@link LegalEntityRecord} от этого класса <b>не наследуется</b> — у него
 * собственное объявление со строгой nullability и {@code unique = true} на ИНН.
 *
 * <p>Имена колонок совпадают с {@link LegalEntityRecord}, чтобы
 * {@code BeanUtils.copyProperties} копировал поле-в-поле без явных mapping'ов.</p>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class LegalEntitySnapshotFields {

    @Column(name = "gr_inn", length = 9)
    private String grInn;

    @Column(name = "gr_full_name", length = 500)
    private String grFullName;

    @Column(name = "gr_short_name", length = 255)
    private String grShortName;

    @Column(name = "gr_opf_code", length = 15)
    private String grOpfCode;

    @Column(name = "gr_opf_name", length = 500)
    private String grOpfName;

    @Column(name = "gr_opf_name_uz", length = 500)
    private String grOpfNameUz;

    @Column(name = "gr_registration_date")
    private LocalDate grRegistrationDate;

    @Column(name = "gr_registration_number", length = 15)
    private String grRegistrationNumber;

    @Column(name = "gr_registration_authority", length = 500)
    private String grRegistrationAuthority;

    @Column(name = "gr_statutory_fund", precision = 17, scale = 2)
    private BigDecimal grStatutoryFund;

    @Column(name = "gr_is_small_business")
    private Boolean grIsSmallBusiness;

    @Column(name = "gr_activity_state_code")
    private Short grActivityStateCode;

    @Column(name = "gr_activity_state_detail_id")
    private Short grActivityStateDetailId;

    @Column(name = "gr_activity_state_name", length = 255)
    private String grActivityStateName;

    @Column(name = "gr_is_active")
    private Boolean grIsActive;

    @Column(name = "gr_is_bankrupt")
    private Boolean grIsBankrupt;

    @Column(name = "gr_oked_code", length = 15)
    private String grOkedCode;

    @Column(name = "gr_oked_name", length = 500)
    private String grOkedName;

    @Column(name = "gr_oked_name_uz", length = 500)
    private String grOkedNameUz;

    @Column(name = "gr_soogu_code", length = 5)
    private String grSooguCode;

    @Column(name = "gr_soogu_name", length = 500)
    private String grSooguName;

    @Column(name = "gr_kfs_code")
    private Short grKfsCode;

    @Column(name = "gr_kfs_name", length = 255)
    private String grKfsName;

    @Column(name = "gr_business_type_id")
    private Short grBusinessTypeId;

    @Column(name = "gr_business_type_name", length = 255)
    private String grBusinessTypeName;

    @Column(name = "gr_address_full", length = 500)
    private String grAddressFull;

    @Column(name = "gr_soato_code", length = 15)
    private String grSoatoCode;

    @Column(name = "gr_soato_name", length = 500)
    private String grSoatoName;

    @Column(name = "gr_region_code")
    private Integer grRegionCode;

    @Column(name = "gr_region_name", length = 255)
    private String grRegionName;

    @Column(name = "gr_district_code")
    private Integer grDistrictCode;

    @Column(name = "gr_district_name", length = 255)
    private String grDistrictName;

    @Column(name = "gr_village_code")
    private Integer grVillageCode;

    @Column(name = "gr_village_name", length = 255)
    private String grVillageName;

    @Column(name = "gr_street_name", length = 500)
    private String grStreetName;

    @Column(name = "gr_house", length = 255)
    private String grHouse;

    @Column(name = "gr_flat", length = 255)
    private String grFlat;

    @Column(name = "gr_postcode", length = 255)
    private String grPostcode;

    @Column(name = "gr_email", length = 255)
    private String grEmail;

    @Column(name = "gr_email_status")
    private Short grEmailStatus;

    @Convert(converter = StringListConverter.class)
    @Column(name = "gr_phones", columnDefinition = "text")
    private List<String> grPhones;

    @Column(name = "gr_director_name", length = 500)
    private String grDirectorName;

    @Column(name = "gr_director_uuid", length = 64)
    private String grDirectorUuid;

    @Convert(converter = FounderListConverter.class)
    @Column(name = "gr_founders", columnDefinition = "text")
    private List<Founder> grFounders;

    @Column(name = "gr_founders_count")
    private Short grFoundersCount;

    @Column(name = "gr_tax_mode")
    private Short grTaxMode;

    @Column(name = "gr_vat_number", length = 20)
    private String grVatNumber;

    @Column(name = "gr_is_vat_payer")
    private Boolean grIsVatPayer;

    @Column(name = "gr_trust_rating", length = 10)
    private String grTrustRating;

    @Column(name = "gr_trust_score")
    private Short grTrustScore;

    @Column(name = "gr_is_vat_abuser")
    private Boolean grIsVatAbuser;

    @Column(name = "gr_is_dishonest_executor")
    private Boolean grIsDishonestExecutor;

    @Column(name = "gr_is_supplier")
    private Boolean grIsSupplier;

    @Column(name = "gr_courts_total")
    private Integer grCourtsTotal;

    @Column(name = "gr_connections_total")
    private Integer grConnectionsTotal;

    @Column(name = "gr_licenses_total")
    private Integer grLicensesTotal;

    @Column(name = "gr_deals_customer_total")
    private Integer grDealsCustomerTotal;

    @Column(name = "gr_deals_provider_total")
    private Integer grDealsProviderTotal;

    @Column(name = "gr_buildings_total")
    private Integer grBuildingsTotal;

    @Column(name = "gr_cadastres_total")
    private Integer grCadastresTotal;
}
