# Handoff: unified source registry (work in progress, not committed)

Worktree: this directory. Module worktrees on branch `claude/pensive-lovelace-06b135`
live inside it: `eidos-stage/`, `eidos-gateway/`, `eidos-core/`, `eidos-cdi-ui/`.
Build with JDK 26: `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home`
(`./mvnw test` in gateway/core, `mvn test` in stage — stage has no wrapper).
Baseline before changes: all three suites green.

`docs/`, `mkdocs.yml`, `tools/` were COPIED from the main checkout, where they are
uncommitted. Pristine copy: scratchpad `docs-baseline/`. Deliver doc edits as a
patch against that baseline; ask the user before applying it to the main checkout.
To build, drop `docs/run-with-docker.md` (deleted in main) from a staging copy, or
`nav.omitted_files` fails `--strict`.

## Done — gateway enforces `enabled` (tests pass, 38/38)
- `TokenAuthService.authorize`: unknown/missing token → 401; known token whose
  source is not explicitly `enabled=true` → new `ForbiddenException` → **403**
  `{"detail":"Source '<code>' is disabled"}`. Rationale: the source is identified,
  so rotating the token won't help; 403 tells the integrator to contact the operator.
  Applies to every external route (client-data, legal-data, consents, external GR API).
- `SourceTokenAuthFilter` writes 403 (detail JSON-escaped); `GlobalExceptionHandler` maps it.
- Kafka: `GatewayKafkaConsumer.onMessage` skips 401/403 failures immediately (no
  container retries); messages of a disabled source are dropped, must be resent.
  Not counted in reject stats (rejected = failed validation).

## To do — core reads the registry (single source of truth)
- Flyway `V5__source_registry_link.sql`: `ALTER TABLE source ADD COLUMN registry_id bigint`
  + unique constraint. Local rows are never deleted → FKs/provenance intact.
- Registry DataSource in core: `@Qualifier("registry") @Bean(defaultCandidate = false)`
  (keeps Boot's primary DataSource/JPA/Flyway auto-config) + JdbcTemplate reader of
  `eidos_registry.source(id, code, trust_level)`. Props `eidos.registry.datasource.*`;
  compose env `EIDOS_REGISTRY_DATASOURCE_URL/USERNAME/PASSWORD` for eidos-core.
- Sync (startup = backfill, then every `eidos.registry.sync-interval`, default 10 s,
  needs `@EnableScheduling`): pass 1 — rows linked by `registry_id`: update name
  (renames) and trust; pass 2 — unlinked registry rows: link local row by
  `source_name = code` or insert. Local rows whose registry row vanished → set
  `registry_id = NULL`. One transaction per row; failures logged, retried next round.
  Registry unreachable/table missing → WARN, keep last state, core still starts.
- Resolver used by both save flows + `ExternalIdService`: fast path = local row
  linked to registry; else targeted registry lookup by code → adopt in
  `REQUIRES_NEW` → re-read; not in registry → existing UNKNOWN_SOURCE + 404;
  registry error → 503, no tentative. Check linkage with an exists-query so a stale
  entity is not cached in the caller's persistence context.
- Endpoints: `GET /api/v1/internal/sources`, `POST /api/v1/internal/sources/sync`.
- Upgrade note: registry trust now wins over old `eidos_core.source` values (log each change).

## To do — stage: retire/delete
- `DELETE /internal/api/v1/sources/{id}`: 409 while enabled ("disable first"); when
  disabled, delete its contracts (fields cascade), then the source. Keep ingest_stats.
- New `DELETE /internal/api/v1/sources/{id}/contract?entityType=…` → 204 / 404.
- `ConflictException` → 409 in stage `GlobalExceptionHandler`; fix `Source` javadoc.
- Console `SourcesPage.tsx`: disable "Удалить" for active sources, confirm text says
  contracts are deleted too.

## To do — loadtest, docs
- `setup.py`: drop `ensure_core_sources()`; add `--trust` (merge tests expect payme=10);
  call core `POST /api/v1/internal/sources/sync` and print what core sees. Delete
  `seed_core_sources.sql`; update loadtest README.
- Docs (Russian): the six pages requested, plus every page linking
  `registration.md#регистрация-в-ядре` (automation, external-ids, troubleshooting,
  sources/errors, console/conflicts), concepts/conflicts, ingestion, initial-load,
  onboarding, monitoring, reference/api/core + stage, reference/errors + enums,
  configuration, databases, decisions (ADR "Отдельная таблица источников в ядре").
  Quickstart merge-and-conflict §4: replace "unknown to core" with "disabled source → 403".
  Verify: `NO_MKDOCS_2_WARNING=true mkdocs build --strict` in a venv.
