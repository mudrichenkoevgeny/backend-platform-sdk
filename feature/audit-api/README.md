# feature/audit-api

HTTP routing and orchestration for reading audit events (management APIs) on top of `core/audit`.

## What it provides

- `AuditRouter` as the aggregate router for audit HTTP endpoints.
- `ManagementAuditRouter` — authenticated staff/admin list and get-by-id flows with permission-aware masking.
- [AuditManager] and use cases that load events from [AuditEventRepository] in `core/audit` and map responses for the wire format.

## Usage

- Add a Gradle dependency on the `:feature:audit-api` project (it already depends on `:core:audit`, `:core:common`, and `:feature:user`).
- Include [AuditModules] from `core/audit` **and** [AuditApiModules] from this module in your Dagger component so persistence, [AuditManager], and routers are wired.
- Register `AuditRouter` in your Ktor `routing { }` block.

## Notes

- This module exposes HTTP APIs and feature-level orchestration only.
- Fire-and-forget logging, SQL, and masking helpers remain in `core/audit`.

[AuditManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/manager/AuditManager.kt
[AuditApiModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/di/AuditApiModules.kt
[AuditModules]: ../core/audit/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/di/AuditModules.kt
[AuditEventRepository]: ../core/audit/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/database/repository/AuditEventRepository.kt
