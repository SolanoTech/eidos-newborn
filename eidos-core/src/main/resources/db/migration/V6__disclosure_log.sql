-- Журнал раскрытий: кто, что и на каком основании увидел в открытом виде.
--
-- Раскрытие обезличенных данных — исключение, а не рядовая операция, и должно
-- оставлять след. Журнал лежит в схеме vault рядом с токенами: тот, кто имеет
-- доступ к отображению на персональные данные, оставляет запись об этом там же.
CREATE TABLE vault.pd_disclosure (
    disclosure_id uuid        PRIMARY KEY,
    owner_id      varchar(64) NOT NULL,
    field_name    varchar(64) NOT NULL,
    actor         varchar(128) NOT NULL,
    reason        text        NOT NULL,
    disclosed_at  timestamp   NOT NULL
);
CREATE INDEX ix_pd_disclosure_owner ON vault.pd_disclosure (owner_id);
CREATE INDEX ix_pd_disclosure_actor ON vault.pd_disclosure (actor, disclosed_at);

COMMENT ON TABLE vault.pd_disclosure IS
    'Каждое раскрытие обезличенного значения: кто, какое поле, чьё и на каком основании.';
