# feature/settingsapi

HTTP routing and orchestration for global platform settings and real-time configuration synchronization via WebSockets.

## What it provides

- **Routing:**
    - [OpenGlobalSettingsRouter] — public endpoints for reading platform-wide settings available to all clients.
    - [ManagementGlobalSettingsRouter] — authenticated (STAFF/ADMIN) routes for administrative updates to global configuration.
- **Orchestration:**
    - **UpdateGlobalSettings:** [UpdateGlobalSettingsUseCase] for persisting global settings, logging management audit events, and broadcasting updates to all connected clients.
    - **GetGlobalSettings:** [GetGlobalSettingsUseCase] for retrieving the current effective settings snapshot.
- **Real-time Sync:** [SettingsWebSocketMessageHandler] — handles settings-related WebSocket frames, acknowledging update events to ensure immediate configuration synchronization across sessions.
- **Swagger:** [SettingsSwaggerTags] for consistent categorization of global settings endpoints in OpenAPI docs.

## Usage

### 1. DI Configuration
Include **[SettingsApiModules]** in your Dagger component.
- It contributes the [SettingsWebSocketMessageHandler] into the global multibound set of WebSocket handlers.
- The module requires [SettingsModules] from `core/settings` to be present in the graph to provide [GlobalSettingsProvider] and persistence logic.

### 2. Route Registration
Inject [OpenGlobalSettingsRouter] and [ManagementGlobalSettingsRouter], then register them within your Ktor `routing { }` block using their `register()` methods.

---

[SettingsApiModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/settingsapi/api/di/SettingsApiModules.kt
[OpenGlobalSettingsRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/settingsapi/api/route/open/OpenGlobalSettingsRouter.kt
[ManagementGlobalSettingsRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/settingsapi/api/route/management/ManagementGlobalSettingsRouter.kt
[UpdateGlobalSettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/settingsapi/api/usecase/management/globalsettings/UpdateGlobalSettingsUseCase.kt
[GetGlobalSettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/settingsapi/api/usecase/open/globalsettings/GetGlobalSettingsUseCase.kt
[SettingsWebSocketMessageHandler]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/settingsapi/api/network/websockets/messagehandler/SettingsWebSocketMessageHandler.kt
[SettingsSwaggerTags]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/feature/settingsapi/api/route/SettingsSwaggerTags.kt