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

package com.solano.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data contract for the legal-entity Golden Record (юридические лица /
 * мерчанты). Same conventions as {@link GoldenRecordDto}: bean validation
 * guards required values, JSON names follow the registry field spec.
 *
 * <p>Identity: {@code GR_Inn} — самодостаточный точный ключ юрлица (9 цифр);
 * матчинг существующей записи в core идёт по external id, затем по ИНН.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
public class LegalEntityGoldenRecordDto {

    // ---------- Идентификация и наименования ----------

    @NotBlank
    @Pattern(regexp = "^\\d{9}$", message = "INN must be exactly 9 digits")
    @JsonProperty("GR_Inn")
    private String grInn;

    @NotBlank
    @Size(max = 500)
    @JsonProperty("GR_FullName")
    private String grFullName;

    @Size(max = 255)
    @JsonProperty("GR_ShortName")
    private String grShortName;

    // ---------- Организационно-правовая форма ----------

    @Size(max = 15)
    @JsonProperty("GR_OpfCode")
    private String grOpfCode;

    @Size(max = 500)
    @JsonProperty("GR_OpfName")
    private String grOpfName;

    @Size(max = 500)
    @JsonProperty("GR_OpfNameUz")
    private String grOpfNameUz;

    // ---------- Регистрация ----------

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("GR_RegistrationDate")
    private LocalDate grRegistrationDate;

    @Size(max = 15)
    @JsonProperty("GR_RegistrationNumber")
    private String grRegistrationNumber;

    @Size(max = 500)
    @JsonProperty("GR_RegistrationAuthority")
    private String grRegistrationAuthority;

    @Digits(integer = 15, fraction = 2)
    @JsonProperty("GR_StatutoryFund")
    private BigDecimal grStatutoryFund;

    @JsonProperty("GR_IsSmallBusiness")
    private Boolean grIsSmallBusiness;

    // ---------- Состояние деятельности ----------

    @JsonProperty("GR_ActivityStateCode")
    private Short grActivityStateCode;

    @JsonProperty("GR_ActivityStateDetailId")
    private Short grActivityStateDetailId;

    @Size(max = 255)
    @JsonProperty("GR_ActivityStateName")
    private String grActivityStateName;

    @NotNull
    @JsonProperty("GR_IsActive")
    private Boolean grIsActive;

    @NotNull
    @JsonProperty("GR_IsBankrupt")
    private Boolean grIsBankrupt;

    // ---------- Классификаторы ----------

    @NotBlank
    @Size(max = 15)
    @JsonProperty("GR_OkedCode")
    private String grOkedCode;

    @NotBlank
    @Size(max = 500)
    @JsonProperty("GR_OkedName")
    private String grOkedName;

    @Size(max = 500)
    @JsonProperty("GR_OkedNameUz")
    private String grOkedNameUz;

    @Size(max = 5)
    @JsonProperty("GR_SooguCode")
    private String grSooguCode;

    @Size(max = 500)
    @JsonProperty("GR_SooguName")
    private String grSooguName;

    @JsonProperty("GR_KfsCode")
    private Short grKfsCode;

    @Size(max = 255)
    @JsonProperty("GR_KfsName")
    private String grKfsName;

    @JsonProperty("GR_BusinessTypeId")
    private Short grBusinessTypeId;

    @Size(max = 255)
    @JsonProperty("GR_BusinessTypeName")
    private String grBusinessTypeName;

    // ---------- Адрес ----------

    @Size(max = 500)
    @JsonProperty("GR_AddressFull")
    private String grAddressFull;

    @Size(max = 15)
    @JsonProperty("GR_SoatoCode")
    private String grSoatoCode;

    @Size(max = 500)
    @JsonProperty("GR_SoatoName")
    private String grSoatoName;

    @JsonProperty("GR_RegionCode")
    private Integer grRegionCode;

    @Size(max = 255)
    @JsonProperty("GR_RegionName")
    private String grRegionName;

    @JsonProperty("GR_DistrictCode")
    private Integer grDistrictCode;

    @Size(max = 255)
    @JsonProperty("GR_DistrictName")
    private String grDistrictName;

    @JsonProperty("GR_VillageCode")
    private Integer grVillageCode;

    @Size(max = 255)
    @JsonProperty("GR_VillageName")
    private String grVillageName;

    @Size(max = 500)
    @JsonProperty("GR_StreetName")
    private String grStreetName;

    @Size(max = 255)
    @JsonProperty("GR_House")
    private String grHouse;

    @Size(max = 255)
    @JsonProperty("GR_Flat")
    private String grFlat;

    @Size(max = 255)
    @JsonProperty("GR_Postcode")
    private String grPostcode;

    // ---------- Контакты ----------

    @Size(max = 255)
    @JsonProperty("GR_Email")
    private String grEmail;

    @JsonProperty("GR_EmailStatus")
    private Short grEmailStatus;

    @JsonProperty("GR_Phones")
    private List<String> grPhones;

    // ---------- Руководство и учредители ----------

    @Size(max = 500)
    @JsonProperty("GR_DirectorName")
    private String grDirectorName;

    @Size(max = 64)
    @JsonProperty("GR_DirectorUuid")
    private String grDirectorUuid;

    @JsonProperty("GR_Founders")
    private List<Founder> grFounders;

    @JsonProperty("GR_FoundersCount")
    private Short grFoundersCount;

    // ---------- Налоги ----------

    @JsonProperty("GR_TaxMode")
    private Short grTaxMode;

    @Size(max = 20)
    @JsonProperty("GR_VatNumber")
    private String grVatNumber;

    @JsonProperty("GR_IsVatPayer")
    private Boolean grIsVatPayer;

    // ---------- Благонадёжность ----------

    @Size(max = 10)
    @JsonProperty("GR_TrustRating")
    private String grTrustRating;

    @JsonProperty("GR_TrustScore")
    private Short grTrustScore;

    @JsonProperty("GR_IsVatAbuser")
    private Boolean grIsVatAbuser;

    @JsonProperty("GR_IsDishonestExecutor")
    private Boolean grIsDishonestExecutor;

    @JsonProperty("GR_IsSupplier")
    private Boolean grIsSupplier;

    // ---------- Счётчики связанных объектов ----------

    @JsonProperty("GR_CourtsTotal")
    private Integer grCourtsTotal;

    @JsonProperty("GR_ConnectionsTotal")
    private Integer grConnectionsTotal;

    @JsonProperty("GR_LicensesTotal")
    private Integer grLicensesTotal;

    @JsonProperty("GR_DealsCustomerTotal")
    private Integer grDealsCustomerTotal;

    @JsonProperty("GR_DealsProviderTotal")
    private Integer grDealsProviderTotal;

    @JsonProperty("GR_BuildingsTotal")
    private Integer grBuildingsTotal;

    @JsonProperty("GR_CadastresTotal")
    private Integer grCadastresTotal;

    /**
     * Учредитель юрлица (элемент {@code GR_Founders}).
     *
     * <p>ПРЕДВАРИТЕЛЬНАЯ структура — в спецификации состав STRUCT не расписан.
     * Поля подобраны по типовому набору реестра: уточнить по фактическому
     * контракту источника и при необходимости расширить.</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Founder {

        /** Идентификатор учредителя в системе-источнике. */
        @Size(max = 64)
        @JsonProperty("uuid")
        private String uuid;

        @Size(max = 500)
        @JsonProperty("name")
        private String name;

        /** ИНН учредителя-юрлица (9 цифр), если учредитель — организация. */
        @Pattern(regexp = "^\\d{9}$", message = "founder INN must be exactly 9 digits")
        @JsonProperty("inn")
        private String inn;

        /** ПИНФЛ учредителя-физлица (14 цифр), если учредитель — человек. */
        @Pattern(regexp = "^\\d{14}$", message = "founder PINFL must be exactly 14 digits")
        @JsonProperty("pinfl")
        private String pinfl;

        /** Доля в уставном фонде, процент. */
        @Digits(integer = 3, fraction = 2)
        @JsonProperty("sharePercent")
        private BigDecimal sharePercent;
    }
}
