# feature/securityapi

HTTP routing and orchestration for security settings (password policies, authentication windows, etc.) and real-time policy synchronization via WebSockets.

## What it provides

- **Routing:**
    - [OpenSecuritySettingsRouter] — public endpoints for reading current security policies (e.g., password requirements).
    - [ManagementSecuritySettingsRouter] — authenticated (STAFF/ADMIN) routes for updating global security parameters.
- **Orchestration:**
    - **UpdateSecuritySettings:** [UpdateSecuritySettingsUseCase] for persisting settings, logging administrative audit events, and broadcasting updates via WebSockets.
    - **GetSecuritySettings:** [GetSecuritySettingsUseCase] for retrieving the effective security snapshot.
- **Real-time Sync:** [SecurityWebSocketMessageHandler] — handles security-related WebSocket frames, specifically acknowledging policy updates to ensure immediate enforcement across clients.
- **Swagger:** [SecuritySwaggerTags] for consistent categorization of security endpoints in OpenAPI docs.

## Usage

### 1. DI Configuration
Include **[SecurityApiModules]** in your Dagger component.
- It contributes the WebSocket handler into the global `Set<WebSocketMessageHandler>` via multibindings.
- The module requires [SecurityModules] from `core/security` to be present for [SecuritySettingsProvider] and other core security logic.

### 2. Route Registration
Inject [OpenSecuritySettingsRouter] and [ManagementSecuritySettingsRouter], then register them within your Ktor `routing { }` block.

---

[SecurityApiModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/securityapi/api/di/SecurityApiModules.kt
[OpenSecuritySettingsRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/securityapi/api/route/open/OpenSecuritySettingsRouter.kt
[ManagementSecuritySettingsRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/securityapi/api/route/management/ManagementSecuritySettingsRouter.kt
[UpdateSecuritySettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/securityapi/api/usecase/management/settings/UpdateSecuritySettingsUseCase.kt
[GetSecuritySettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/securityapi/api/usecase/open/settings/GetSecuritySettingsUseCase.kt
[SecurityWebSocketMessageHandler]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/securityapi/api/network/websockets/messagehandler/SecurityWebSocketMessageHandler.kt
[SecuritySwaggerTags]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/securityapi/api/route/SecuritySwaggerTags.kt