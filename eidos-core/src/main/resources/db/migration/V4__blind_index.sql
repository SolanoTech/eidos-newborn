-- Слепой индекс: HMAC над теми же комбинациями полей, по которым сейчас идёт
-- поиск. Позволит искать и сопоставлять записи после того, как сами значения
-- станут токенами — равенство HMAC заменит равенство значений.
--
-- Заполняется для ВСЕХ записей, независимо от согласия: иначе поиск зависел бы
-- от состояния клиента и записи «то находились, то нет».
--
-- Комбинации повторяют существующие правила один в один:
--   identity      = фамилия + имя + дата рождения + ПИНФЛ   (основной матчинг)
--   identity_doc  = фамилия + имя + дата рождения + паспорт (откат)
--   pinfl         = ПИНФЛ                                    (точный поиск)
ALTER TABLE golden_record
    ADD COLUMN pd_bi_identity     varchar(64),
    ADD COLUMN pd_bi_identity_doc varchar(64),
    ADD COLUMN pd_bi_pinfl        varchar(64),
    ADD COLUMN pd_bi_key_version  integer;

CREATE INDEX ix_gr_bi_identity     ON golden_record (pd_bi_identity);
CREATE INDEX ix_gr_bi_identity_doc ON golden_record (pd_bi_identity_doc);
CREATE INDEX ix_gr_bi_pinfl        ON golden_record (pd_bi_pinfl);
-- Незаполненные и устаревшие по версии ключа строки ищет дозаполнение.
CREATE INDEX ix_gr_bi_key_version  ON golden_record (pd_bi_key_version);

-- Ключ слепого индекса. Хранится завёрнутым тем же KEK, что и ключи данных,
-- и версионируется: ротация ключа означает пересчёт индекса, а пересчитать
-- десять тысяч строк мгновенно нельзя — обе версии какое-то время сосуществуют.
CREATE TABLE vault.pd_blind_index_key (
    key_version integer   PRIMARY KEY,
    wrapped_key bytea     NOT NULL,
    kek_name    varchar(64) NOT NULL,
    created_at  timestamp NOT NULL
);

COMMENT ON COLUMN golden_record.pd_bi_key_version IS
    'Версия ключа, которым посчитан индекс. NULL — индекс ещё не заполнен.';
