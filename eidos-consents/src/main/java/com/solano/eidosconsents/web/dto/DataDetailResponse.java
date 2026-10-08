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

package com.solano.eidosconsents.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Конверт ответа {@code {"data": ..., "detail": "..."}}: полезная нагрузка и
 * человекочитаемое пояснение рядом.
 *
 * @param <T> тип полезной нагрузки
 */
public record DataDetailResponse<T>(
        @JsonProperty("data") T data,
        @JsonProperty("detail") String detail
) {

    public static <T> DataDetailResponse<T> of(T data, String detail) {
        return new DataDetailResponse<>(data, detail);
    }
}
