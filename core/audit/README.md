# core/audit

Audit events: model, persistence, and query API.

## What it provides

- **Model:** [AuditEvent], [AuditEventId], [AuditStatus] — who did what, on which resource, with which outcome.
- **AuditService** — fire-and-forget logging: `log(event)` schedules persistence in the background; `awaitAll()` for tests or shutdown.
- **AuditManager** — synchronous API: `createEvent`, `getEventById`, `getEventsList` (with optional filters), `getEventsByActor`. Used when you need the result or listing.

Persistence uses Exposed and a single table (see [AuditEventsTable]); ordering is by `createdAt` descending.

## Usage

- Add dependency on `core:audit`.
- Install [AuditModules] in your Dagger component so [AuditService] and [AuditManager] are available.
- **Database:** Ensure the audit table exists. The module ships a Flyway migration; see [AuditEventsTable] KDoc for the migration path. Add it to your app’s Flyway locations; the module does not run migrations itself.
- Inject [AuditService] for fire-and-forget logging from routes or use cases; inject [AuditManager] when you need to create/query events with a result.

Used by `core:crosscutting` and `feature:user` (e.g. login/session audit).

[AuditEvent]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/model/AuditEvent.kt
[AuditEventId]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/model/AuditEventId.kt
[AuditStatus]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/model/AuditStatus.kt
[AuditEventsTable]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/database/table/AuditEventsTable.kt
[AuditModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/di/AuditModules.kt
[AuditService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/service/AuditService.kt
[AuditManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/manager/AuditManager.kt
