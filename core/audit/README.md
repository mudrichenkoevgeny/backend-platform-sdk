# core/audit

Audit events: persistence, background logging, and shared masking helpers. Domain types such as `AuditEvent`, `AuditEventId`, and `AuditStatus` live in the **shared foundation** library this SDK depends on—not in this module.

## What it provides

- **[AuditEventRepository]** — insert and query `audit_events` (Exposed, [AuditEventsTable]). List queries take an **[AuditAccessFilter]** so SQL enforces which actor types and user roles the caller may see. Supports extensive filtering (actors, actions, resources, statuses) and search by message.
- **[AuditService]** — fire-and-forget logging: `log(event)` runs `createEvent` on a background scope inside `dbQuery`; `awaitAll()` for tests or shutdown.
- **[AuditLogger]** — convenience API that builds an `AuditEvent` with current timestamp and forwards it to `AuditService.log`.
- **[AuditErrorConverter]** — transforms `AppError` into audit-ready data using a chain of **[AuditErrorParser]** implementations.
- **[AuditDataMasker]** — redacts `resourceId` and metadata values based on `AuditValueSensitivity` (Email, Phone, IP, etc.) before exposing events to external APIs.

Management **HTTP** routes, permission-aware reads, and response mapping are implemented in **`feature:audit-api`**.

## Usage

- Add dependency on `core:audit`.
- Install **[AuditModules]** in your Dagger component.
- **Database:** Ensure the audit table exists. The module ships a Flyway migration under `src/main/resources/db/migration/core/audit/`.
- Inject **[AuditLogger]** or **[AuditService]** for fire-and-forget logging from routes or use cases.
- For management HTTP APIs over audit data, depend on `feature:audit-api` and install `AuditApiModules` in addition to **[AuditModules]**.

[AuditAccessFilter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/domain/model/AuditAccessFilter.kt
[AuditDataMasker]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/mask/AuditDataMasker.kt
[AuditEventRepository]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/database/repository/AuditEventRepository.kt
[AuditEventsTable]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/database/table/AuditEventsTable.kt
[AuditLogger]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/logger/AuditLogger.kt
[AuditModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/di/AuditModules.kt
[AuditService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/service/AuditService.kt
[AuditErrorConverter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/error/AuditErrorConverter.kt
[AuditErrorParser]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/error/AuditErrorParser.kt