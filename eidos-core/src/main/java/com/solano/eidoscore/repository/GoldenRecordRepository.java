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

package com.solano.eidoscore.repository;

import com.solano.eidoscore.entity.GoldenRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface GoldenRecordRepository extends JpaRepository<GoldenRecord, String> {

    Optional<GoldenRecord> findByGrPinfl(String pinfl);

    /**
     * Записи, чей слепой индекс не посчитан или посчитан старым ключом.
     * Используется дозаполнением; оно же обслуживает будущую ротацию ключа.
     */
    Page<GoldenRecord> findByPdBiKeyVersionIsNullOrPdBiKeyVersionNot(Integer keyVersion, Pageable pageable);

    long countByPdBiKeyVersionIsNullOrPdBiKeyVersionNot(Integer keyVersion);

    /**
     * Записывает слепой индекс, не трогая остальную карточку.
     *
     * <p>Как и у состояния согласия — точечным UPDATE, чтобы не двигать
     * {@code version} и {@code updated_at}. Дозаполнение индекса не изменение
     * данных клиента, и выглядеть как массовая правка всех карточек за один
     * день оно не должно.</p>
     */
    @Transactional
    @Modifying
    @Query("""
            update GoldenRecord g
               set g.pdBiIdentity     = :identity,
                   g.pdBiIdentityDoc  = :identityDoc,
                   g.pdBiPinfl        = :pinfl,
                   g.pdBiKeyVersion   = :keyVersion
             where g.grClientId = :clientId
            """)
    int updateBlindIndex(@Param("clientId") String clientId,
                         @Param("identity") String identity,
                         @Param("identityDoc") String identityDoc,
                         @Param("pinfl") String pinfl,
                         @Param("keyVersion") Integer keyVersion);

    /**
     * Точечно обновляет состояние согласия, не трогая остальную карточку.
     *
     * <p>Намеренно bulk-запросом, а не через сущность: так не инкрементится
     * {@code version} и не переписывается {@code updated_at}. Смена согласия —
     * внешний факт о клиенте, а не изменение Золотой записи, и в её истории
     * версий ей делать нечего.</p>
     *
     * @return сколько строк обновлено: 0 означает, что записи с таким id нет
     */
    @Modifying
    @Query("""
            update GoldenRecord g
               set g.pdConsentActive    = :active,
                   g.pdConsentValidTo   = :validTo,
                   g.pdConsentCheckedAt = :checkedAt
             where g.grClientId = :clientId
            """)
    int updateConsentState(@Param("clientId") String clientId,
                           @Param("active") Boolean active,
                           @Param("validTo") LocalDate validTo,
                           @Param("checkedAt") LocalDateTime checkedAt);

    /**
     * Точный матчинг: фамилия + имя + дата рождения + ПИНФЛ.
     *
     * <p>Сравнение идёт по слепому индексу, а не по самим значениям. Правило
     * то же и результат тот же — равенство индексов равносильно равенству
     * четвёрки, — но работать оно продолжит и после того, как значения станут
     * токенами.</p>
     */
    /**
     * Записывает персональные поля разом, не трогая остальную карточку.
     *
     * <p>Служит обоим направлениям: обезличиванию — токены и отметка времени, —
     * и обратному раскрытию, где приходят открытые значения, а отметка
     * обнуляется.</p>
     *
     * <p>Как и у состояния согласия с индексом, это точечный UPDATE: смена
     * формы хранения не изменение данных клиента. Версия не двигается, архив не
     * пишется — иначе процесс, прячущий данные, сам сохранил бы их открытую
     * копию в истории.</p>
     */
    @Transactional
    @Modifying
    @Query("""
            update GoldenRecord g
               set g.grLastName = :grLastName,
                   g.grFirstName = :grFirstName,
                   g.grMiddleName = :grMiddleName,
                   g.grPinfl = :grPinfl,
                   g.grDocPassData = :grDocPassData,
                   g.grDocIssuedBy = :grDocIssuedBy,
                   g.grMobilePhoneMain = :grMobilePhoneMain,
                   g.grContactsEmail = :grContactsEmail,
                   g.grBirthPlace = :grBirthPlace,
                   g.grAddrPermanentAddress = :grAddrPermanentAddress,
                   g.grAddrTemporaryAddress = :grAddrTemporaryAddress,
                   g.grAddrPermRegAddress = :grAddrPermRegAddress,
                   g.grAddrTempRegAddress = :grAddrTempRegAddress,
                   g.pdTokenizedAt = :tokenizedAt
             where g.grClientId = :clientId
            """)
    int updatePersonalData(@Param("clientId") String clientId,
                             @Param("grLastName") String grLastName,
                             @Param("grFirstName") String grFirstName,
                             @Param("grMiddleName") String grMiddleName,
                             @Param("grPinfl") String grPinfl,
                             @Param("grDocPassData") String grDocPassData,
                             @Param("grDocIssuedBy") String grDocIssuedBy,
                             @Param("grMobilePhoneMain") String grMobilePhoneMain,
                             @Param("grContactsEmail") String grContactsEmail,
                             @Param("grBirthPlace") String grBirthPlace,
                             @Param("grAddrPermanentAddress") String grAddrPermanentAddress,
                             @Param("grAddrTemporaryAddress") String grAddrTemporaryAddress,
                             @Param("grAddrPermRegAddress") String grAddrPermRegAddress,
                             @Param("grAddrTempRegAddress") String grAddrTempRegAddress,
                             @Param("tokenizedAt") LocalDateTime tokenizedAt);

    /** Записи, потерявшие согласие и ещё не обезличенные. */
    Page<GoldenRecord> findByPdConsentActiveFalseAndPdTokenizedAtIsNull(Pageable pageable);

    /** Обезличенные записи, которым согласие вернули. */
    Page<GoldenRecord> findByPdConsentActiveTrueAndPdTokenizedAtIsNotNull(Pageable pageable);

    Optional<GoldenRecord> findByPdBiIdentity(String identityIndex);

    /** Откат точного матчинга: фамилия + имя + дата рождения + паспорт. */
    Optional<GoldenRecord> findByPdBiIdentityDoc(String identityDocIndex);

    @Query("""
            SELECT g FROM GoldenRecord g
            WHERE g.grLastName LIKE CONCAT('%', :search, '%')
               OR g.grFirstName LIKE CONCAT('%', :search, '%')
               OR g.grDocPassData LIKE CONCAT('%', :search, '%')
               OR g.grPinfl LIKE CONCAT('%', :search, '%')
            """)
    Page<GoldenRecord> searchByPattern(@Param("search") String search, Pageable pageable);

    /**
     * Структурный поиск по реквизитам. Пустая строка параметра = критерий не
     * задан. Имена сравниваются без регистра, паспорт — без пробелов в верхнем
     * регистре, телефон — только цифры (нормализация в сервисе).
     *
     * <p>ПИНФЛ сравнивается по слепому индексу и потому только целиком; для
     * него признак «критерий не задан» — {@code null}, а не пустая строка.
     * Остальные критерии остались подстрочными и индексом не закрываются.</p>
     */
    @Query("""
            SELECT g FROM GoldenRecord g
            WHERE (:pinflIndex IS NULL OR g.pdBiPinfl = :pinflIndex)
              AND (:passport = '' OR UPPER(g.grDocPassData) LIKE CONCAT('%', :passport, '%'))
              AND (:lastName = '' OR LOWER(g.grLastName) LIKE CONCAT('%', :lastName, '%'))
              AND (:firstName = '' OR LOWER(g.grFirstName) LIKE CONCAT('%', :firstName, '%'))
              AND (:middleName = '' OR LOWER(g.grMiddleName) LIKE CONCAT('%', :middleName, '%'))
              AND (:phone = '' OR g.grMobilePhoneMain LIKE CONCAT('%', :phone, '%'))
            """)
    Page<GoldenRecord> structuredSearch(
            @Param("pinflIndex") String pinflIndex,
            @Param("passport") String passport,
            @Param("lastName") String lastName,
            @Param("firstName") String firstName,
            @Param("middleName") String middleName,
            @Param("phone") String phone,
            Pageable pageable);

    // ---- Метрики качества (PostgreSQL) ----

    @Query(value = "SELECT count(*) FROM golden_record WHERE gr_mobile_phone_main ~ '^998\\d{9}$'", nativeQuery = true)
    long countValidPhones();

    @Query(value = "SELECT count(*) FROM golden_record WHERE gr_pinfl ~ '^\\d{14}$'", nativeQuery = true)
    long countValidPinfl();

    @Query(value = "SELECT count(*) FROM golden_record WHERE gr_middle_name IS NOT NULL AND gr_middle_name <> ''", nativeQuery = true)
    long countWithMiddleName();

    @Query(value = "SELECT count(*) FROM golden_record WHERE updated_at > now() - interval '30 days'", nativeQuery = true)
    long countFresh30d();
}
