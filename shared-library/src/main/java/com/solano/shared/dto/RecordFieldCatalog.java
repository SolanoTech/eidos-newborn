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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Каталог полей Golden Record DTO, построенный рефлексией, — единый источник
 * истины о целевых полях записи: какие существуют, как называются в JSON
 * ({@code GR_*}), обязательны ли, какой у них логический тип и есть ли
 * встроенный формат ({@code @Pattern}).
 *
 * <p>Параметризован классом контракта: {@code RecordFieldCatalog.of(GoldenRecordDto.class)}
 * — физлица, {@code of(LegalEntityGoldenRecordDto.class)} — юрлица. Каталог
 * каждого класса вычисляется один раз и кэшируется.</p>
 *
 * <p>Используется конструктором дата-контрактов (eidos-stage): валидировать,
 * что целевое поле существует, и отдавать список доступных полей в UI.</p>
 */
public final class RecordFieldCatalog {

    /** Логический тип целевого поля записи. */
    public enum FieldType {
        STRING,
        DATE,
        /** Пол физлица: источники присылают код, value-map приводит к M/F. */
        GENDER,
        BOOLEAN,
        /** Целочисленные поля (Short/Integer/Long). */
        INTEGER,
        /** Денежные/дробные значения (BigDecimal). */
        DECIMAL,
        /** Массив строк, например телефоны юрлица. */
        STRING_ARRAY,
        /** Массив вложенных структур, например учредители юрлица. */
        STRUCT_ARRAY
    }

    /**
     * Метаданные одного поля записи.
     *
     * @param jsonName  имя в JSON, напр. {@code GR_FirstName}
     * @param javaName  имя Java-поля, напр. {@code grFirstName}
     * @param type      логический тип
     * @param required  обязательно ли ({@code @NotBlank}/{@code @NotNull})
     * @param pattern   встроенный regex ({@code @Pattern}) или {@code null}
     */
    public record FieldDescriptor(
            String jsonName,
            String javaName,
            FieldType type,
            boolean required,
            String pattern
    ) {
    }

    private static final Map<Class<?>, RecordFieldCatalog> CACHE = new ConcurrentHashMap<>();

    private final Map<String, FieldDescriptor> byJsonName;

    private RecordFieldCatalog(Class<?> dtoClass) {
        this.byJsonName = build(dtoClass);
    }

    /** Каталог полей для класса контракта (вычисляется однажды на класс). */
    public static RecordFieldCatalog of(Class<?> dtoClass) {
        return CACHE.computeIfAbsent(dtoClass, RecordFieldCatalog::new);
    }

    /** Все поля записи в порядке объявления в DTO. */
    public List<FieldDescriptor> fields() {
        return List.copyOf(byJsonName.values());
    }

    /** Описание поля по его JSON-имени ({@code GR_*}), либо {@code null}. */
    public FieldDescriptor byJsonName(String jsonName) {
        return byJsonName.get(jsonName);
    }

    /** Существует ли поле записи с таким JSON-именем. */
    public boolean isValidTarget(String jsonName) {
        return byJsonName.containsKey(jsonName);
    }

    private static Map<String, FieldDescriptor> build(Class<?> dtoClass) {
        Map<String, FieldDescriptor> result = new LinkedHashMap<>();
        for (Field field : dtoClass.getDeclaredFields()) {
            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            if (jsonProperty == null) {
                continue;
            }
            String jsonName = jsonProperty.value();
            boolean required = field.isAnnotationPresent(NotBlank.class)
                    || field.isAnnotationPresent(NotNull.class);
            Pattern pattern = field.getAnnotation(Pattern.class);
            FieldDescriptor descriptor = new FieldDescriptor(
                    jsonName,
                    field.getName(),
                    resolveType(field),
                    required,
                    pattern == null ? null : pattern.regexp()
            );
            result.put(jsonName, descriptor);
        }
        return result;
    }

    private static FieldType resolveType(Field field) {
        Class<?> javaType = field.getType();
        if (javaType.equals(LocalDate.class)) {
            return FieldType.DATE;
        }
        if (javaType.isEnum()) {
            // Единственный enum в контрактах — Gender физлица.
            return FieldType.GENDER;
        }
        if (javaType.equals(Boolean.class) || javaType.equals(boolean.class)) {
            return FieldType.BOOLEAN;
        }
        if (javaType.equals(Short.class) || javaType.equals(Integer.class)
                || javaType.equals(Long.class)
                || javaType.equals(short.class) || javaType.equals(int.class)
                || javaType.equals(long.class)) {
            return FieldType.INTEGER;
        }
        if (javaType.equals(BigDecimal.class)) {
            return FieldType.DECIMAL;
        }
        if (List.class.isAssignableFrom(javaType)) {
            return listElementType(field) == String.class
                    ? FieldType.STRING_ARRAY
                    : FieldType.STRUCT_ARRAY;
        }
        return FieldType.STRING;
    }

    private static Class<?> listElementType(Field field) {
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType parameterized) {
            Type arg = parameterized.getActualTypeArguments()[0];
            if (arg instanceof Class<?> cls) {
                return cls;
            }
        }
        return Object.class;
    }
}
