#!/usr/bin/env python3
# Copyright 2026 LLC SOLANOTECH
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Генерирует таблицы справочника полей Золотой записи из описаний записей в
shared-library: имена полей, типы, обязательность и ограничения берутся из
кода, описания — из словарей ниже.

Запуск из корня репозитория, после изменения GoldenRecordDto или
LegalEntityGoldenRecordDto:

    python3 tools/docs/generate_field_reference.py

Результат — docs/reference/_generated/*.md; страницы справочника включают их.
Новое поле без описания в словаре остановит генерацию: допишите описание.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DTO_DIR = ROOT / "shared-library/src/main/java/com/solano/shared/dto"

PERSON_DESC = {
    "GR_mobilePhoneMain": "Основной мобильный телефон",
    "GR_FirstName": "Имя",
    "GR_MiddleName": "Отчество",
    "GR_LastName": "Фамилия",
    "GR_Pinfl": "ПИНФЛ",
    "GR_Gender": "Пол",
    "GR_BirthPlace": "Место рождения",
    "GR_BirthCountry": "Страна рождения",
    "GR_BirthCountryId": "Код страны рождения",
    "GR_BirthDate": "Дата рождения",
    "GR_Nationality": "Национальность",
    "GR_NationalityId": "Код национальности",
    "GR_Citizenship": "Гражданство",
    "GR_CitizenshipId": "Код гражданства",
    "GR_docPassData": "Серия и номер паспорта",
    "GR_docIssuedBy": "Кем выдан документ",
    "GR_docIssuedById": "Код органа, выдавшего документ",
    "GR_docIssuedDate": "Дата выдачи документа",
    "GR_docExpiryDate": "Дата окончания действия документа",
    "GR_contactsEmail": "Адрес электронной почты",
    "GR_addrPermanentAddress": "Адрес постоянного проживания строкой",
    "GR_addrTemporaryAddress": "Адрес временного проживания строкой",
    "GR_addrPermRegMfy": "Постоянная регистрация: махалля",
    "GR_addrPermRegMfyId": "Постоянная регистрация: код махалли",
    "GR_addrPermRegRegion": "Постоянная регистрация: регион",
    "GR_addrPermRegAddress": "Постоянная регистрация: адрес",
    "GR_addrPermRegCountry": "Постоянная регистрация: страна",
    "GR_addrPermRegCadastre": "Постоянная регистрация: кадастровый номер",
    "GR_addrPermRegDistrict": "Постоянная регистрация: район",
    "GR_addrPermRegRegionId": "Постоянная регистрация: код региона",
    "GR_addrPermRegCountryId": "Постоянная регистрация: код страны",
    "GR_addrPermRegDistrictId": "Постоянная регистрация: код района",
    "GR_addrPermRegRegistrationDate": "Постоянная регистрация: дата регистрации",
    "GR_addrTempRegMfy": "Временная регистрация: махалля",
    "GR_addrTempRegMfyId": "Временная регистрация: код махалли",
    "GR_addrTempRegRegion": "Временная регистрация: регион",
    "GR_addrTempRegAddress": "Временная регистрация: адрес",
    "GR_addrTempRegDistrict": "Временная регистрация: район",
    "GR_addrTempRegDateFrom": "Временная регистрация: действует с",
    "GR_addrTempRegDateTill": "Временная регистрация: действует по",
    "GR_addrTempRegRegionId": "Временная регистрация: код региона",
    "GR_addrTempRegDistrictId": "Временная регистрация: код района",
}

LEGAL_DESC = {
    "GR_Inn": "ИНН",
    "GR_FullName": "Полное наименование",
    "GR_ShortName": "Краткое наименование, бренд",
    "GR_OpfCode": "Код организационно-правовой формы",
    "GR_OpfName": "Организационно-правовая форма",
    "GR_OpfNameUz": "Организационно-правовая форма на узбекском",
    "GR_RegistrationDate": "Дата государственной регистрации",
    "GR_RegistrationNumber": "Регистрационный номер",
    "GR_RegistrationAuthority": "Регистрирующий орган",
    "GR_StatutoryFund": "Уставный фонд",
    "GR_IsSmallBusiness": "Субъект малого бизнеса",
    "GR_ActivityStateCode": "Код состояния деятельности",
    "GR_ActivityStateDetailId": "Код уточнения состояния деятельности",
    "GR_ActivityStateName": "Состояние деятельности",
    "GR_IsActive": "Юрлицо действует",
    "GR_IsBankrupt": "Юрлицо признано банкротом",
    "GR_OkedCode": "Код вида деятельности по ОКЭД",
    "GR_OkedName": "Вид деятельности по ОКЭД",
    "GR_OkedNameUz": "Вид деятельности по ОКЭД на узбекском",
    "GR_SooguCode": "Код органа управления по СООГУ",
    "GR_SooguName": "Орган управления по СООГУ",
    "GR_KfsCode": "Код формы собственности по КФС",
    "GR_KfsName": "Форма собственности по КФС",
    "GR_BusinessTypeId": "Код типа бизнеса",
    "GR_BusinessTypeName": "Тип бизнеса",
    "GR_AddressFull": "Полный адрес",
    "GR_SoatoCode": "Код территории по СОАТО",
    "GR_SoatoName": "Территория по СОАТО",
    "GR_RegionCode": "Код региона",
    "GR_RegionName": "Регион",
    "GR_DistrictCode": "Код района",
    "GR_DistrictName": "Район",
    "GR_VillageCode": "Код населённого пункта",
    "GR_VillageName": "Населённый пункт",
    "GR_StreetName": "Улица",
    "GR_House": "Дом",
    "GR_Flat": "Квартира или офис",
    "GR_Postcode": "Почтовый индекс",
    "GR_Email": "Адрес электронной почты",
    "GR_EmailStatus": "Код статуса адреса электронной почты",
    "GR_Phones": "Телефоны",
    "GR_DirectorName": "Руководитель",
    "GR_DirectorUuid": "Идентификатор руководителя в источнике",
    "GR_Founders": "Учредители — см. [структуру учредителя](#учредитель)",
    "GR_FoundersCount": "Число учредителей",
    "GR_TaxMode": "Код режима налогообложения",
    "GR_VatNumber": "Регистрационный номер плательщика НДС",
    "GR_IsVatPayer": "Плательщик НДС",
    "GR_TrustRating": "Рейтинг надёжности",
    "GR_TrustScore": "Балл надёжности",
    "GR_IsVatAbuser": "Признак нарушений по НДС",
    "GR_IsDishonestExecutor": "Признак недобросовестного исполнителя",
    "GR_IsSupplier": "Признак поставщика",
    "GR_CourtsTotal": "Число судебных дел",
    "GR_ConnectionsTotal": "Число связанных лиц",
    "GR_LicensesTotal": "Число лицензий",
    "GR_DealsCustomerTotal": "Число сделок в роли заказчика",
    "GR_DealsProviderTotal": "Число сделок в роли поставщика",
    "GR_BuildingsTotal": "Число зданий",
    "GR_CadastresTotal": "Число кадастровых объектов",
}

FOUNDER_DESC = {
    "uuid": "Идентификатор учредителя в источнике",
    "name": "Наименование или ФИО учредителя",
    "inn": "ИНН учредителя-юрлица",
    "pinfl": "ПИНФЛ учредителя-физлица",
    "sharePercent": "Доля в уставном фонде, %",
}

TYPES = {
    "String": ("строка", "STRING", "`STRING`"),
    "LocalDate": ("дата `yyyy-MM-dd`", "DATE", "`DATE`"),
    "Gender": ("`M` или `F`", "GENDER", "`GENDER`"),
    "Boolean": ("логическое", "BOOLEAN", "`BOOLEAN`"),
    "Short": ("целое, −32768…32767", "INTEGER", "`INTEGER`"),
    "Integer": ("целое", "INTEGER", "`INTEGER`"),
    "BigDecimal": ("десятичное число", "DECIMAL", "`STRING`"),
    "List<String>": ("массив строк", "STRING_ARRAY", "`ARRAY`"),
    "List<Founder>": ("массив структур", "STRUCT_ARRAY", "`STRUCT`"),
}

def parse(block: str):
    """Собирает аннотации построчно: в сообщениях валидации бывают скобки."""
    fields = []
    annotations = []
    for line in block.splitlines():
        stripped = line.strip()
        if stripped.startswith("@"):
            annotations.append(stripped)
            continue
        match = re.match(r"private\s+([\w<>]+)\s+(\w+);", stripped)
        if match:
            text = "\n".join(annotations)
            annotations = []
            json_name = re.search(r'@JsonProperty\("([^"]+)"\)', text)
            if not json_name:
                continue
            java_type, java_name = match.groups()
            required = "@NotBlank" in text or "@NotNull" in text
            constraints = []
            pattern = re.search(r'@Pattern\(regexp = "((?:[^"\\]|\\.)*)"', text)
            if pattern:
                constraints.append("`" + pattern.group(1).replace("\\\\", "\\") + "`")
            size = re.search(r"@Size\(max = (\d+)\)", text)
            if size:
                constraints.append(f"до {size.group(1)} символов")
            digits = re.search(r"@Digits\(integer = (\d+), fraction = (\d+)\)", text)
            if digits:
                constraints.append(f"до {digits.group(1)} цифр в целой части и {digits.group(2)} в дробной")
            fields.append((json_name.group(1), java_name, java_type, required, constraints))
        elif stripped and not stripped.startswith(("*", "/")):
            annotations = []
    return fields


def table(fields, desc, with_contract=True):
    head = "| Поле контракта | Свойство в API | Значение | Обяз. | Ограничения | Тип правила | Описание |\n"
    head += "|---|---|---|---|---|---|---|\n"
    rows = []
    for json_name, java_name, java_type, required, constraints in fields:
        kind, _catalog, rule = TYPES[java_type]
        rows.append(
            f"| `{json_name}` | `{java_name}` | {kind} | {'да' if required else ''} | "
            f"{'; '.join(constraints)} | {rule} | {desc[json_name]} |")
    return head + "\n".join(rows) + "\n"


def founder_table(fields):
    head = "| Ключ | Значение | Ограничения | Описание |\n|---|---|---|---|\n"
    rows = []
    for json_name, _java, java_type, _req, constraints in fields:
        kind = TYPES[java_type][0]
        rows.append(f"| `{json_name}` | {kind} | {'; '.join(constraints)} | {FOUNDER_DESC[json_name]} |")
    return head + "\n".join(rows) + "\n"


def main():
    person_src = (DTO_DIR / "GoldenRecordDto.java").read_text()
    legal_src = (DTO_DIR / "LegalEntityGoldenRecordDto.java").read_text()
    legal_main, founder_src = legal_src.split("public static class Founder")
    person = parse(person_src)
    legal = parse(legal_main)
    founder = parse(founder_src)
    for name, fields, desc in (("GoldenRecordDto", person, PERSON_DESC),
                               ("LegalEntityGoldenRecordDto", legal, LEGAL_DESC),
                               ("Founder", founder, FOUNDER_DESC)):
        missing = [f[0] for f in fields if f[0] not in desc]
        if missing:
            sys.exit(f"{name}: нет описания для {', '.join(missing)}")
    out = ROOT / "docs/reference/_generated"
    out.mkdir(parents=True, exist_ok=True)
    (out / "person-table.md").write_text(table(person, PERSON_DESC))
    (out / "legal-table.md").write_text(table(legal, LEGAL_DESC))
    (out / "founder-table.md").write_text(founder_table(founder))
    print(f"Полей: физлицо {len(person)}, юрлицо {len(legal)}, учредитель {len(founder)}")


if __name__ == "__main__":
    main()
