# core/audit

Audit events: persistence, background logging, and shared masking helpers. Domain types such as `AuditEvent`, `AuditEventId`, and `AuditStatus` live in the **shared foundation** library this SDK depends on—not in this module.

## What it provides

- **[AuditEventRepository]** — insert and query `audit_events` (Exposed, [AuditEventsTable]). List queries take an **[AuditAccessFilter]** so SQL enforces which actor types and user roles the caller may see.
- **[AuditService]** — fire-and-forget logging: `log(event)` runs `createEvent` on a background scope inside `dbQuery`; `awaitAll()` for tests or shutdown.
- **[AuditLogger]** / **[AuditLoggerImpl]** — convenience API that builds an `AuditEvent` and forwards it to `AuditService.log`.
- **[AuditDataMasker]** — masks `resourceId` and metadata values using `AuditValueSensitivity` / metadata key sensitivity; used when exposing events with reduced visibility (e.g. from management APIs in `feature:audit-api`).

Management **HTTP** routes, permission-aware reads, and response mapping are implemented in **`feature:audit-api`**. Wire **[AuditModules]** for this core stack; add **[AuditApiModules]** from `feature/audit-api` when you need those HTTP endpoints and [AuditManager].

Persistence uses a single table; list ordering follows the repository API (e.g. `created_at` and sort parameters).

## Usage

- Add dependency on `core:audit`.
- Install [AuditModules] in your Dagger component so [AuditEventRepository], [AuditService], and [AuditLogger] are available.
- **Database:** Ensure the audit table exists. The module ships a Flyway migration under `src/main/resources/db/migration/core/audit/`; add that location to your app’s Flyway config. The module does not run migrations by itself.
- Inject [AuditService] or [AuditLogger] for fire-and-forget logging from routes or use cases.
- For management HTTP APIs over audit data, depend on `feature:audit-api` and install [AuditApiModules] in addition to [AuditModules].

Used by `core:crosscutting`, `feature:user`, `feature:audit-api`, and other modules that log or read audit data.

[AuditAccessFilter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/domain/model/AuditAccessFilter.kt
[AuditApiModules]: ../../feature/audit-api/src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/di/AuditApiModules.kt
[AuditDataMasker]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/mask/AuditDataMasker.kt
[AuditEventRepository]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/database/repository/AuditEventRepository.kt
[AuditEventsTable]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/database/table/AuditEventsTable.kt
[AuditLogger]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/logger/AuditLogger.kt
[AuditLoggerImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/logger/AuditLoggerImpl.kt
[AuditManager]: ../../feature/audit-api/src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/manager/AuditManager.kt
[AuditModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/di/AuditModules.kt
[AuditService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/service/AuditService.kt
