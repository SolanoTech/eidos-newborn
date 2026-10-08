-- Состояние согласия на обработку персональных данных, каким его знает core.
--
-- Заполняется потребителем событий consent-events и пока никем не читается:
-- это опора для последующей деперсонализации, а не действующая логика.
--
-- Колонки namespace'ятся префиксом pd_ (personal data), чтобы не смешиваться
-- с полями дата-контракта, у которых префикс gr_.
ALTER TABLE golden_record
    ADD COLUMN pd_consent_active     boolean,
    ADD COLUMN pd_consent_valid_to   date,
    ADD COLUMN pd_consent_checked_at timestamp;

COMMENT ON COLUMN golden_record.pd_consent_active IS
    'Есть ли у клиента действующее согласие на обработку ПДн. NULL — core ещё не проверял.';
COMMENT ON COLUMN golden_record.pd_consent_valid_to IS
    'Последний день действия согласия: позволяет вычислить истечение локально, без обращения к сервису согласий.';
COMMENT ON COLUMN golden_record.pd_consent_checked_at IS
    'Когда core в последний раз сверялся с сервисом согласий.';
