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
import com.solano.shared.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Data contract for the Golden Record. Strict variant: only the fields the SCV
 * core actually stores, with bean-validation guarding required values and
 * three format constraints (phone / PINFL / passport).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(alphabetic = true)
public class GoldenRecordDto {

    @NotBlank
    @Pattern(regexp = "^998\\d{9}$", message = "must match format 998XXXXXXXXX (12 digits, starts with 998)")
    @JsonProperty("GR_mobilePhoneMain")
    private String grMobilePhoneMain;

    @NotBlank
    @JsonProperty("GR_FirstName")
    private String grFirstName;

    @JsonProperty("GR_MiddleName")
    private String grMiddleName;

    @NotBlank
    @JsonProperty("GR_LastName")
    private String grLastName;

    @NotBlank
    @Pattern(regexp = "^\\d{14}$", message = "PINFL must be exactly 14 digits")
    @JsonProperty("GR_Pinfl")
    private String grPinfl;

    @NotNull
    @JsonProperty("GR_Gender")
    private Gender grGender;

    @JsonProperty("GR_BirthPlace")
    private String grBirthPlace;

    @JsonProperty("GR_BirthCountry")
    private String grBirthCountry;

    @JsonProperty("GR_BirthCountryId")
    private String grBirthCountryId;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("GR_BirthDate")
    private LocalDate grBirthDate;

    @JsonProperty("GR_Nationality")
    private String grNationality;

    @JsonProperty("GR_NationalityId")
    private String grNationalityId;

    @NotBlank
    @JsonProperty("GR_Citizenship")
    private String grCitizenship;

    @NotBlank
    @JsonProperty("GR_CitizenshipId")
    private String grCitizenshipId;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}\\d{7}$", message = "passport must match format AA1234567 (2 uppercase letters + 7 digits)")
    @JsonProperty("GR_docPassData")
    private String grDocPassData;

    @NotBlank
    @JsonProperty("GR_docIssuedBy")
    private String grDocIssuedBy;

    @NotBlank
    @JsonProperty("GR_docIssuedById")
    private String grDocIssuedById;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("GR_docIssuedDate")
    private LocalDate grDocIssuedDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("GR_docExpiryDate")
    private LocalDate grDocExpiryDate;

    @JsonProperty("GR_contactsEmail")
    private String grContactsEmail;

    @JsonProperty("GR_addrPermanentAddress")
    private String grAddrPermanentAddress;

    @JsonProperty("GR_addrTemporaryAddress")
    private String grAddrTemporaryAddress;

    @JsonProperty("GR_addrPermRegMfy")
    private String grAddrPermRegMfy;

    @JsonProperty("GR_addrPermRegMfyId")
    private String grAddrPermRegMfyId;

    @JsonProperty("GR_addrPermRegRegion")
    private String grAddrPermRegRegion;

    @JsonProperty("GR_addrPermRegAddress")
    private String grAddrPermRegAddress;

    @JsonProperty("GR_addrPermRegCountry")
    private String grAddrPermRegCountry;

    @JsonProperty("GR_addrPermRegCadastre")
    private String grAddrPermRegCadastre;

    @JsonProperty("GR_addrPermRegDistrict")
    private String grAddrPermRegDistrict;

    @JsonProperty("GR_addrPermRegRegionId")
    private String grAddrPermRegRegionId;

    @JsonProperty("GR_addrPermRegCountryId")
    private String grAddrPermRegCountryId;

    @JsonProperty("GR_addrPermRegDistrictId")
    private String grAddrPermRegDistrictId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("GR_addrPermRegRegistrationDate")
    private LocalDate grAddrPermRegRegistrationDate;

    @JsonProperty("GR_addrTempRegMfy")
    private String grAddrTempRegMfy;

    @JsonProperty("GR_addrTempRegMfyId")
    private String grAddrTempRegMfyId;

    @JsonProperty("GR_addrTempRegRegion")
    private String grAddrTempRegRegion;

    @JsonProperty("GR_addrTempRegAddress")
    private String grAddrTempRegAddress;

    @JsonProperty("GR_addrTempRegDistrict")
    private String grAddrTempRegDistrict;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("GR_addrTempRegDateFrom")
    private LocalDate grAddrTempRegDateFrom;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("GR_addrTempRegDateTill")
    private LocalDate grAddrTempRegDateTill;

    @JsonProperty("GR_addrTempRegRegionId")
    private String grAddrTempRegRegionId;

    @JsonProperty("GR_addrTempRegDistrictId")
    private String grAddrTempRegDistrictId;
}
