| Поле контракта | Свойство в API | Значение | Обяз. | Ограничения | Тип правила | Описание |
|---|---|---|---|---|---|---|
| `GR_mobilePhoneMain` | `grMobilePhoneMain` | строка | да | `^998\d{9}$` | `STRING` | Основной мобильный телефон |
| `GR_FirstName` | `grFirstName` | строка | да |  | `STRING` | Имя |
| `GR_MiddleName` | `grMiddleName` | строка |  |  | `STRING` | Отчество |
| `GR_LastName` | `grLastName` | строка | да |  | `STRING` | Фамилия |
| `GR_Pinfl` | `grPinfl` | строка | да | `^\d{14}$` | `STRING` | ПИНФЛ |
| `GR_Gender` | `grGender` | `M` или `F` | да |  | `GENDER` | Пол |
| `GR_BirthPlace` | `grBirthPlace` | строка |  |  | `STRING` | Место рождения |
| `GR_BirthCountry` | `grBirthCountry` | строка |  |  | `STRING` | Страна рождения |
| `GR_BirthCountryId` | `grBirthCountryId` | строка |  |  | `STRING` | Код страны рождения |
| `GR_BirthDate` | `grBirthDate` | дата `yyyy-MM-dd` | да |  | `DATE` | Дата рождения |
| `GR_Nationality` | `grNationality` | строка |  |  | `STRING` | Национальность |
| `GR_NationalityId` | `grNationalityId` | строка |  |  | `STRING` | Код национальности |
| `GR_Citizenship` | `grCitizenship` | строка | да |  | `STRING` | Гражданство |
| `GR_CitizenshipId` | `grCitizenshipId` | строка | да |  | `STRING` | Код гражданства |
| `GR_docPassData` | `grDocPassData` | строка | да | `^[A-Z]{2}\d{7}$` | `STRING` | Серия и номер паспорта |
| `GR_docIssuedBy` | `grDocIssuedBy` | строка | да |  | `STRING` | Кем выдан документ |
| `GR_docIssuedById` | `grDocIssuedById` | строка | да |  | `STRING` | Код органа, выдавшего документ |
| `GR_docIssuedDate` | `grDocIssuedDate` | дата `yyyy-MM-dd` | да |  | `DATE` | Дата выдачи документа |
| `GR_docExpiryDate` | `grDocExpiryDate` | дата `yyyy-MM-dd` |  |  | `DATE` | Дата окончания действия документа |
| `GR_contactsEmail` | `grContactsEmail` | строка |  |  | `STRING` | Адрес электронной почты |
| `GR_addrPermanentAddress` | `grAddrPermanentAddress` | строка |  |  | `STRING` | Адрес постоянного проживания строкой |
| `GR_addrTemporaryAddress` | `grAddrTemporaryAddress` | строка |  |  | `STRING` | Адрес временного проживания строкой |
| `GR_addrPermRegMfy` | `grAddrPermRegMfy` | строка |  |  | `STRING` | Постоянная регистрация: махалля |
| `GR_addrPermRegMfyId` | `grAddrPermRegMfyId` | строка |  |  | `STRING` | Постоянная регистрация: код махалли |
| `GR_addrPermRegRegion` | `grAddrPermRegRegion` | строка |  |  | `STRING` | Постоянная регистрация: регион |
| `GR_addrPermRegAddress` | `grAddrPermRegAddress` | строка |  |  | `STRING` | Постоянная регистрация: адрес |
| `GR_addrPermRegCountry` | `grAddrPermRegCountry` | строка |  |  | `STRING` | Постоянная регистрация: страна |
| `GR_addrPermRegCadastre` | `grAddrPermRegCadastre` | строка |  |  | `STRING` | Постоянная регистрация: кадастровый номер |
| `GR_addrPermRegDistrict` | `grAddrPermRegDistrict` | строка |  |  | `STRING` | Постоянная регистрация: район |
| `GR_addrPermRegRegionId` | `grAddrPermRegRegionId` | строка |  |  | `STRING` | Постоянная регистрация: код региона |
| `GR_addrPermRegCountryId` | `grAddrPermRegCountryId` | строка |  |  | `STRING` | Постоянная регистрация: код страны |
| `GR_addrPermRegDistrictId` | `grAddrPermRegDistrictId` | строка |  |  | `STRING` | Постоянная регистрация: код района |
| `GR_addrPermRegRegistrationDate` | `grAddrPermRegRegistrationDate` | дата `yyyy-MM-dd` |  |  | `DATE` | Постоянная регистрация: дата регистрации |
| `GR_addrTempRegMfy` | `grAddrTempRegMfy` | строка |  |  | `STRING` | Временная регистрация: махалля |
| `GR_addrTempRegMfyId` | `grAddrTempRegMfyId` | строка |  |  | `STRING` | Временная регистрация: код махалли |
| `GR_addrTempRegRegion` | `grAddrTempRegRegion` | строка |  |  | `STRING` | Временная регистрация: регион |
| `GR_addrTempRegAddress` | `grAddrTempRegAddress` | строка |  |  | `STRING` | Временная регистрация: адрес |
| `GR_addrTempRegDistrict` | `grAddrTempRegDistrict` | строка |  |  | `STRING` | Временная регистрация: район |
| `GR_addrTempRegDateFrom` | `grAddrTempRegDateFrom` | дата `yyyy-MM-dd` |  |  | `DATE` | Временная регистрация: действует с |
| `GR_addrTempRegDateTill` | `grAddrTempRegDateTill` | дата `yyyy-MM-dd` |  |  | `DATE` | Временная регистрация: действует по |
| `GR_addrTempRegRegionId` | `grAddrTempRegRegionId` | строка |  |  | `STRING` | Временная регистрация: код региона |
| `GR_addrTempRegDistrictId` | `grAddrTempRegDistrictId` | строка |  |  | `STRING` | Временная регистрация: код района |
