-- Обезличивание записей клиентов, потерявших согласие на обработку данных.
--
-- Персональные значения заменяются токенами из хранилища vault. Токен длиннее
-- исходных значений, поэтому колонки с жёсткой длиной расширяются. Остальные
-- персональные колонки объявлены без ограничения длины и места хватает.
--
-- Уникальность ПИНФЛ сохраняется: токены случайны и не совпадают.
ALTER TABLE golden_record
    ALTER COLUMN gr_pinfl             TYPE varchar(64),
    ALTER COLUMN gr_doc_pass_data     TYPE varchar(64),
    ALTER COLUMN gr_mobile_phone_main TYPE varchar(64);

-- Отметка обезличивания. NULL — значения открыты.
ALTER TABLE golden_record
    ADD COLUMN pd_tokenized_at timestamp;

COMMENT ON COLUMN golden_record.pd_tokenized_at IS
    'Когда персональные значения записи заменены токенами. NULL — хранятся открыто.';

-- Выборка работы для обезличивания и обратного раскрытия.
CREATE INDEX ix_gr_pd_lifecycle ON golden_record (pd_consent_active, pd_tokenized_at);
