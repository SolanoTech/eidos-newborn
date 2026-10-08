-- Регистрация источников в eidos-core.
-- Core держит СОБСТВЕННУЮ таблицу source (trust_level для merge Золотой записи),
-- отдельную от реестра eidos_registry (консолидация отложена). Неизвестный core
-- источник отклоняется при приёме («Unknown source … rejecting»). Таблицу создаёт
-- Hibernate при старте core, поэтому сидинг выполняется ПОСЛЕ подъёма сервисов,
-- а не в postgres-init.
--
-- Запуск: docker compose exec -T postgres psql -U postgres -d eidos_core -f - < loadtest/seed_core_sources.sql
INSERT INTO source (source_name, trust_level) VALUES
  ('loadtest', 8),
  ('psp-a', 9),
  ('abc', 5),
  ('bank-1', 10),
  ('bank-2', 1)
ON CONFLICT (source_name) DO UPDATE SET trust_level = EXCLUDED.trust_level;
