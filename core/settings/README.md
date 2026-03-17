# core/settings

DB-backed system settings and a public "global settings" API for SDK-based applications.

## What it provides

- **Config**: [SettingsConfig] built by [SettingsConfigFactory] from env via [SettingsEnvKeys].
- **DB-backed settings**:
  - Exposed table: [SystemSettingsTable] (schema via Flyway).
  - Persistence contract: [SystemSettingRepository] ([SystemSettingRepositoryImpl]).
  - Transaction-bound access: [SystemSettingsManager] ([SystemSettingsManagerImpl]).
  - In-memory cached access with typed getters: [SystemSettingsService] ([SystemSettingsServiceImpl]).
- **Global settings (public)**:
  - Provider: [GlobalSettingsProvider] ([GlobalSettingsProviderImpl]) reading/writing DB-backed keys.
  - Use cases: [GetGlobalSettingsUseCase], [SeedGlobalSettingsUseCase].
  - HTTP routes: [GlobalSettingsRouter] composed by [SettingsFeatureRouter].
- **WebSockets**: [SettingsWebSocketMessageHandler] contributed into the app-wide handler set.
- **DI wiring**: [SettingsModules] aggregates all settings-related Dagger modules.

## Environment variables

The default config factory ([SettingsConfigFactoryImpl]) reads:

- `PRIVACY_POLICY_URL` — URL to a privacy policy page (optional).
- `TERMS_OF_SERVICE_URL` — URL to a terms of service page (optional).
- `CONTACT_SUPPORT_EMAIL` — support email address exposed to clients (optional).

See: [SettingsEnvKeys].

## Usage

- Add dependency on `core:settings`. Depends on `core:common` and `core:database`.
- Install [SettingsModules] in your Dagger component.
- Ensure your Flyway migrator is configured to include settings migrations (see below).

### Minimal wiring (Dagger + Ktor routing)

Register settings routes from your application’s routing block:

```kotlin
fun Routing.installSettings(settingsFeatureRouter: SettingsFeatureRouter) {
    settingsFeatureRouter.register(this)
}
```

### Startup seeding (recommended)

On application startup, seed defaults from env into the DB (if present) and initialize the in-memory cache:

```kotlin
suspend fun seedSettings(
    systemSettingsService: SystemSettingsService,
    seedGlobalSettingsUseCase: SeedGlobalSettingsUseCase
) {
    systemSettingsService.initialize()
    seedGlobalSettingsUseCase.execute()
}
```

## Migrations

Settings tables are created by Flyway migrations in `db/migration/core/settings/`.
The app must include this path in its Flyway migration locations.

[SettingsEnvKeys]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/config/envkeys/SettingsEnvKeys.kt
[SettingsConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/config/model/SettingsConfig.kt
[SettingsConfigFactory]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/config/factory/SettingsConfigFactory.kt
[SettingsConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/config/factory/SettingsConfigFactoryImpl.kt

[SystemSettingsTable]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/database/table/SystemSettingsTable.kt
[SystemSettingRepository]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/database/repository/SystemSettingsRepository.kt
[SystemSettingRepositoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/database/repository/SystemSettingsRepositoryImpl.kt
[SystemSettingsManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/manager/SystemSettingsManager.kt
[SystemSettingsManagerImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/manager/SystemSettingsManagerImpl.kt
[SystemSettingsService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/service/SystemSettingsService.kt
[SystemSettingsServiceImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/service/SystemSettingsServiceImpl.kt

[GlobalSettingsProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/global/provider/GlobalSettingsProvider.kt
[GlobalSettingsProviderImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/global/provider/GlobalSettingsProviderImpl.kt
[GetGlobalSettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/global/usecase/GetGlobalSettingsUseCase.kt
[SeedGlobalSettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/global/usecase/SeedGlobalSettingsUseCase.kt
[GlobalSettingsRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/global/route/GlobalSettingsRouter.kt
[SettingsFeatureRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/route/SettingsFeatureRouter.kt

[SettingsWebSocketMessageHandler]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/network/websockets/messagehandler/SettingsWebSocketMessageHandler.kt

[SettingsModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/di/SettingsModules.kt

