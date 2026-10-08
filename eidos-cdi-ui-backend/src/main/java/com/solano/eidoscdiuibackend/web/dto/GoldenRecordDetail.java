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

package com.solano.eidoscdiuibackend.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Детальная карточка Golden Record для экрана профиля. Курируемый набор полей,
 * маппится из полной сущности eidos-core; остальные поля игнорируются.
 * Провенанс и история (GoldenRecordFieldMeta / GoldenRecordArchive) появятся
 * отдельными эндпоинтами в фазе развития core.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoldenRecordDetail(
        String grClientId,
        Integer version,
        String grFirstName,
        String grMiddleName,
        String grLastName,
        String grGender,
        String grBirthDate,
        String grBirthPlace,
        String grBirthCountry,
        String grMobilePhoneMain,
        String grPinfl,
        String grDocPassData,
        String grDocIssuedDate,
        String grCitizenship,
        String grContactsEmail,
        String grAddrPermanentAddress,
        String createdAt
) {
}
