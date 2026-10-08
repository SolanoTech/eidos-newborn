-- Хранилище токенов: отображение «токен → зашифрованное значение».
--
-- Вынесено в отдельную схему: компрометация основной схемы не должна сама по
-- себе давать доступ к отображению на персональные данные. Следующий шаг в этом
-- направлении — отдельный пользователь БД с правами только на vault, а в целевой
-- картине и вовсе отдельный сервис.
CREATE SCHEMA IF NOT EXISTS vault;

-- Ключ шифрования данных. Один на владельца на прогон деперсонализации, а не на
-- поле: каждая обёртка DEK — обращение к хранилищу ключей, и при ключе на поле
-- их число умножалось бы на число полей, а восстановление владельца переставало
-- быть одной операцией.
--
-- wrapped_key — keyset Tink, зашифрованный KEK. В открытом виде DEK существует
-- только в памяти процесса на время операции.
CREATE TABLE vault.pd_dek (
    dek_id      uuid        PRIMARY KEY,
    owner_id    varchar(64) NOT NULL,
    wrapped_key bytea       NOT NULL,
    kek_name    varchar(64) NOT NULL,
    created_at  timestamp   NOT NULL
);
CREATE INDEX ix_pd_dek_owner ON vault.pd_dek (owner_id);

-- Токен — то, что встанет в профильную таблицу вместо значения.
CREATE TABLE vault.pd_token (
    token      varchar(64) PRIMARY KEY,
    dek_id     uuid        NOT NULL REFERENCES vault.pd_dek (dek_id),
    owner_id   varchar(64) NOT NULL,
    field_name varchar(64) NOT NULL,
    ciphertext bytea       NOT NULL,
    created_at timestamp   NOT NULL
);
CREATE INDEX ix_pd_token_owner ON vault.pd_token (owner_id);

COMMENT ON TABLE vault.pd_dek IS 'Ключи шифрования данных, завёрнутые KEK из внешнего хранилища.';
COMMENT ON TABLE vault.pd_token IS 'Отображение токена на зашифрованное персональное значение.';
