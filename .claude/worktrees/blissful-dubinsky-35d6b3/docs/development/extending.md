# Расширение платформы

## Новое поле Золотой записи

Состав Золотой записи задан кодом, а не настройкой: новое поле требует
изменений в библиотеке контракта и ядре. После этого поле автоматически
появляется в каталоге полей, конструкторе контракта и проверках `eidos-stage`.

Пример — поле `GR_Inps` (номер ИНПС) у физлица.

### 1. shared-library

Добавьте поле в `GoldenRecordDto` с JSON-именем и проверками:

```java
@Pattern(regexp = "^\\d{14}$", message = "INPS must be exactly 14 digits")
@JsonProperty("GR_Inps")
private String grInps;
```

Каталог полей строится из этого класса автоматически: имя, тип,
обязательность и выражение берутся из аннотаций. Обязательным новое поле
делайте, только если все подключённые источники смогут его передать: иначе их
карточки начнут отклоняться.

### 2. eidos-core: сущности

Добавьте поле с колонкой в:

- `GoldenRecord` — сама запись;
- `GoldenRecordSnapshotFields` — общая часть архива и незавершённых записей.

```java
@Column(name = "gr_inps", length = 14)
private String grInps;
```

### 3. eidos-core: участие в слиянии

Зарегистрируйте поле в `GoldenRecordMapper.buildAccessors()`. Без этого поле
не копируется при создании и не участвует в слиянии, происхождении и разборе
конфликтов:

```java
m.put("grInps", acc(GoldenRecordDto::getGrInps,
        GoldenRecord::getGrInps, GoldenRecord::setGrInps));
```

### 4. eidos-core: миграция

Ядро проверяет схему при старте, поэтому новая колонка нужна во всех трёх
таблицах. Создайте миграцию со следующим номером, например
`V5__golden_record_inps.sql`:

```sql
ALTER TABLE golden_record           ADD COLUMN gr_inps varchar(14);
ALTER TABLE golden_record_archive   ADD COLUMN gr_inps varchar(14);
ALTER TABLE tentative_golden_record ADD COLUMN gr_inps varchar(14);

-- Происхождение для уже существующих записей: слияние перебирает строки
-- происхождения, и без них новое поле у старых записей не заполнится никогда.
-- Поле закрепляется за источником, который владеет ПИНФЛ записи.
INSERT INTO golden_record_field_meta (field_name, gr_client_id, source_id, updated_at)
SELECT 'grInps', m.gr_client_id, m.source_id, now()
FROM golden_record_field_meta m
WHERE m.field_name = 'grPinfl'
ON CONFLICT DO NOTHING;
```

!!! warning "Не забудьте происхождение"
    Слияние проходит только по полям, у которых у записи есть строка
    происхождения. Без вставки строк из примера поле у записей, созданных до
    обновления, останется пустым навсегда.

### 5. Остальное

- **`eidos-stage`, `eidos-gateway`** — изменений кода не требуют; пересоберите
  `eidos-stage` с новой версией библиотеки.
- **Консоли** — если поле нужно показать в карточке, добавьте его в
  `RecordCardPage.tsx` и `C360Page.tsx`.
- **Документация** — добавьте описание в словарь
  `tools/docs/generate_field_reference.py` и выполните его.
- **Тесты** — `GoldenRecordMapperTest`, тесты слияния.

Для юрлица порядок тот же: `LegalEntityGoldenRecordDto`, `LegalEntityRecord`,
`LegalEntitySnapshotFields`, `LegalEntityMapper`, миграция таблиц
`legal_entity_record`, `legal_entity_archive`, `tentative_legal_entity` и
строк `legal_entity_field_meta`.

### Поле для сопоставления

Если поле должно участвовать в сопоставлении, изменений больше: новая
комбинация слепого индекса в `BlindIndex`, колонка и индекс в
`golden_record`, пересчёт в `BlindIndexListener` и `BlindIndexBackfill`,
стратегия в `GoldenRecordSaveService.accurateSearch`. Обсудите такое изменение
в issue до начала работы: оно меняет, какие записи считаются одним человеком.

## Новый тип согласия

1. Добавьте значение в `ConsentType` с кодом и сроком в
   `SaveConsentService.calculateEndDate`.
2. Обновите check-ограничение колонки `type` в существующих базах — Hibernate
   этого не делает, см. [Базы данных](../operations/databases.md#сервисы-на-hibernate-ddl-autoupdate).
3. Добавьте подпись типа в консоли (`CONSENT_LABELS` в `C360Page.tsx`).

## Оформление консолей

Палитра, шрифты, скругления и тени заданы CSS-переменными в
`eidos-cdi-ui/packages/ui-kit/src/styles.css` отдельно для светлой и тёмной
темы. Чтобы привести консоли к фирменному стилю, переопределите эти переменные
и пересоберите образы.

Распространяя изменённую сборку, соблюдайте правила использования товарного
знака: сборку под собственным именем нельзя называть «Eidos» — см.
[Лицензия и товарный знак](../legal.md).
