# feature/audit-api

HTTP routing and orchestration for reading audit events (management APIs) on top of `core/audit`.

## What it provides

- **Routing:** [ManagementAuditRouter] — authenticated (STAFF/ADMIN) routes for listing and retrieving audit events. Supports standard Ktor registration.
- **Orchestration:**
    - **GetAuditEvents:** [GetAuditEventsUseCase] for paginated, filtered search of the audit trail.
    - **GetAuditEvent:** [GetAuditEventUseCase] for single event retrieval by ID.
- **Management API:** [AuditManager] — aggregate interface for interacting with audit storage. Handles both creation (via `createEvent`) and complex querying (via `getEventsPage`).
- **Query Parsing:** [AuditEventsListQueryParams] and `parseAuditEventsListQueryParams` extension for type-safe extraction of audit filters from Ktor calls.
- **Security & Masking:**
    - Automatic visibility filtering based on the caller's [PermissionCode].
    - Context-aware data masking for sensitive fields in event payloads.
- **Swagger:** [AuditSwaggerTags] for consistent API documentation grouping.

## Usage

### 1. DI Configuration
Include **[AuditApiModules]** in your Dagger component. It includes `AuditApiManagersModule`, which binds the [AuditManager] implementation.
The module expects [AuditModules] from `core/audit` to be present in the graph for persistence and repository access.

### 2. Route Registration
Inject [ManagementAuditRouter] and register it within your Ktor `routing { }` block using the `register()` method.

---

[AuditManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/manager/AuditManager.kt
[AuditApiModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/di/AuditApiModules.kt
[AuditSwaggerTags]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/route/AuditSwaggerTags.kt
[ManagementAuditRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/route/management/ManagementAuditRouter.kt
[GetAuditEventsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/usecase/management/auditevent/GetAuditEventsUseCase.kt
[GetAuditEventUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/usecase/management/auditevent/GetAuditEventUseCase.kt
[AuditEventsListQueryParams]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/audit/api/network/model/AuditEventsListQueryParams.kt