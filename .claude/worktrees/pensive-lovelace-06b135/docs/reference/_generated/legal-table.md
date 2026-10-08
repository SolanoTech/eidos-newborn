| Поле контракта | Свойство в API | Значение | Обяз. | Ограничения | Тип правила | Описание |
|---|---|---|---|---|---|---|
| `GR_Inn` | `grInn` | строка | да | `^\d{9}$` | `STRING` | ИНН |
| `GR_FullName` | `grFullName` | строка | да | до 500 символов | `STRING` | Полное наименование |
| `GR_ShortName` | `grShortName` | строка |  | до 255 символов | `STRING` | Краткое наименование, бренд |
| `GR_OpfCode` | `grOpfCode` | строка |  | до 15 символов | `STRING` | Код организационно-правовой формы |
| `GR_OpfName` | `grOpfName` | строка |  | до 500 символов | `STRING` | Организационно-правовая форма |
| `GR_OpfNameUz` | `grOpfNameUz` | строка |  | до 500 символов | `STRING` | Организационно-правовая форма на узбекском |
| `GR_RegistrationDate` | `grRegistrationDate` | дата `yyyy-MM-dd` |  |  | `DATE` | Дата государственной регистрации |
| `GR_RegistrationNumber` | `grRegistrationNumber` | строка |  | до 15 символов | `STRING` | Регистрационный номер |
| `GR_RegistrationAuthority` | `grRegistrationAuthority` | строка |  | до 500 символов | `STRING` | Регистрирующий орган |
| `GR_StatutoryFund` | `grStatutoryFund` | десятичное число |  | до 15 цифр в целой части и 2 в дробной | `STRING` | Уставный фонд |
| `GR_IsSmallBusiness` | `grIsSmallBusiness` | логическое |  |  | `BOOLEAN` | Субъект малого бизнеса |
| `GR_ActivityStateCode` | `grActivityStateCode` | целое, −32768…32767 |  |  | `INTEGER` | Код состояния деятельности |
| `GR_ActivityStateDetailId` | `grActivityStateDetailId` | целое, −32768…32767 |  |  | `INTEGER` | Код уточнения состояния деятельности |
| `GR_ActivityStateName` | `grActivityStateName` | строка |  | до 255 символов | `STRING` | Состояние деятельности |
| `GR_IsActive` | `grIsActive` | логическое | да |  | `BOOLEAN` | Юрлицо действует |
| `GR_IsBankrupt` | `grIsBankrupt` | логическое | да |  | `BOOLEAN` | Юрлицо признано банкротом |
| `GR_OkedCode` | `grOkedCode` | строка | да | до 15 символов | `STRING` | Код вида деятельности по ОКЭД |
| `GR_OkedName` | `grOkedName` | строка | да | до 500 символов | `STRING` | Вид деятельности по ОКЭД |
| `GR_OkedNameUz` | `grOkedNameUz` | строка |  | до 500 символов | `STRING` | Вид деятельности по ОКЭД на узбекском |
| `GR_SooguCode` | `grSooguCode` | строка |  | до 5 символов | `STRING` | Код органа управления по СООГУ |
| `GR_SooguName` | `grSooguName` | строка |  | до 500 символов | `STRING` | Орган управления по СООГУ |
| `GR_KfsCode` | `grKfsCode` | целое, −32768…32767 |  |  | `INTEGER` | Код формы собственности по КФС |
| `GR_KfsName` | `grKfsName` | строка |  | до 255 символов | `STRING` | Форма собственности по КФС |
| `GR_BusinessTypeId` | `grBusinessTypeId` | целое, −32768…32767 |  |  | `INTEGER` | Код типа бизнеса |
| `GR_BusinessTypeName` | `grBusinessTypeName` | строка |  | до 255 символов | `STRING` | Тип бизнеса |
| `GR_AddressFull` | `grAddressFull` | строка |  | до 500 символов | `STRING` | Полный адрес |
| `GR_SoatoCode` | `grSoatoCode` | строка |  | до 15 символов | `STRING` | Код территории по СОАТО |
| `GR_SoatoName` | `grSoatoName` | строка |  | до 500 символов | `STRING` | Территория по СОАТО |
| `GR_RegionCode` | `grRegionCode` | целое |  |  | `INTEGER` | Код региона |
| `GR_RegionName` | `grRegionName` | строка |  | до 255 символов | `STRING` | Регион |
| `GR_DistrictCode` | `grDistrictCode` | целое |  |  | `INTEGER` | Код района |
| `GR_DistrictName` | `grDistrictName` | строка |  | до 255 символов | `STRING` | Район |
| `GR_VillageCode` | `grVillageCode` | целое |  |  | `INTEGER` | Код населённого пункта |
| `GR_VillageName` | `grVillageName` | строка |  | до 255 символов | `STRING` | Населённый пункт |
| `GR_StreetName` | `grStreetName` | строка |  | до 500 символов | `STRING` | Улица |
| `GR_House` | `grHouse` | строка |  | до 255 символов | `STRING` | Дом |
| `GR_Flat` | `grFlat` | строка |  | до 255 символов | `STRING` | Квартира или офис |
| `GR_Postcode` | `grPostcode` | строка |  | до 255 символов | `STRING` | Почтовый индекс |
| `GR_Email` | `grEmail` | строка |  | до 255 символов | `STRING` | Адрес электронной почты |
| `GR_EmailStatus` | `grEmailStatus` | целое, −32768…32767 |  |  | `INTEGER` | Код статуса адреса электронной почты |
| `GR_Phones` | `grPhones` | массив строк |  |  | `ARRAY` | Телефоны |
| `GR_DirectorName` | `grDirectorName` | строка |  | до 500 символов | `STRING` | Руководитель |
| `GR_DirectorUuid` | `grDirectorUuid` | строка |  | до 64 символов | `STRING` | Идентификатор руководителя в источнике |
| `GR_Founders` | `grFounders` | массив структур |  |  | `STRUCT` | Учредители — см. [структуру учредителя](#учредитель) |
| `GR_FoundersCount` | `grFoundersCount` | целое, −32768…32767 |  |  | `INTEGER` | Число учредителей |
| `GR_TaxMode` | `grTaxMode` | целое, −32768…32767 |  |  | `INTEGER` | Код режима налогообложения |
| `GR_VatNumber` | `grVatNumber` | строка |  | до 20 символов | `STRING` | Регистрационный номер плательщика НДС |
| `GR_IsVatPayer` | `grIsVatPayer` | логическое |  |  | `BOOLEAN` | Плательщик НДС |
| `GR_TrustRating` | `grTrustRating` | строка |  | до 10 символов | `STRING` | Рейтинг надёжности |
| `GR_TrustScore` | `grTrustScore` | целое, −32768…32767 |  |  | `INTEGER` | Балл надёжности |
| `GR_IsVatAbuser` | `grIsVatAbuser` | логическое |  |  | `BOOLEAN` | Признак нарушений по НДС |
| `GR_IsDishonestExecutor` | `grIsDishonestExecutor` | логическое |  |  | `BOOLEAN` | Признак недобросовестного исполнителя |
| `GR_IsSupplier` | `grIsSupplier` | логическое |  |  | `BOOLEAN` | Признак поставщика |
| `GR_CourtsTotal` | `grCourtsTotal` | целое |  |  | `INTEGER` | Число судебных дел |
| `GR_ConnectionsTotal` | `grConnectionsTotal` | целое |  |  | `INTEGER` | Число связанных лиц |
| `GR_LicensesTotal` | `grLicensesTotal` | целое |  |  | `INTEGER` | Число лицензий |
| `GR_DealsCustomerTotal` | `grDealsCustomerTotal` | целое |  |  | `INTEGER` | Число сделок в роли заказчика |
| `GR_DealsProviderTotal` | `grDealsProviderTotal` | целое |  |  | `INTEGER` | Число сделок в роли поставщика |
| `GR_BuildingsTotal` | `grBuildingsTotal` | целое |  |  | `INTEGER` | Число зданий |
| `GR_CadastresTotal` | `grCadastresTotal` | целое |  |  | `INTEGER` | Число кадастровых объектов |
