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
 * Краткая карточка Golden Record для списка результатов поиска. Маппится из
 * полной сущности eidos-core; лишние поля игнорируются.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoldenRecordSummary(
        String grClientId,
        String grFirstName,
        String grMiddleName,
        String grLastName,
        String grMobilePhoneMain,
        String grPinfl,
        String grBirthDate,
        String grDocPassData
) {
}
