# core/settings

DB-backed system settings and the **core** global-settings provider used by HTTP features and other modules.

## What it provides

- **Config**: [SettingsConfig] built by [SettingsConfigFactory] from env via [SettingsEnvKeys].
- **DB-backed settings**:
    - Exposed table: [SystemSettingsTable] (schema via Flyway).
    - Persistence contract: [SystemSettingRepository] ([SystemSettingRepositoryImpl]).
    - Transaction-bound access: [SystemSettingsManager] ([SystemSettingsManagerImpl]).
    - In-memory cached access with typed getters and Redis-based synchronization: [SystemSettingsService] ([SystemSettingsServiceImpl]).
- **Global settings (domain provider)**:
    - [GlobalSettingsProvider] ([GlobalSettingsProviderImpl]) reading/writing DB-backed keys.
    - System seeding: [SeedGlobalSettingsUseCase] (bootstrap / env defaults into the DB).
- **DI wiring**: [SettingsModules] aggregates all settings-related Dagger modules.

**HTTP** read/update of global settings (open and management routes) is implemented in **`feature/settingsapi`** on top of [GlobalSettingsProvider].

## Environment variables

The default config factory ([SettingsConfigFactoryImpl]) reads:

- `PRIVACY_POLICY_URL` — URL to a privacy policy page (optional).
- `TERMS_OF_SERVICE_URL` — URL to a terms of service page (optional).
- `CONTACT_SUPPORT_EMAIL` — support email address exposed to clients (optional).

See: [SettingsEnvKeys].

## Usage

- Add dependency on `core:settings`. Depends on `core:common`, `core:database`, and `shared:foundation`.
- Install [SettingsModules] in your Dagger component.
- Ensure your Flyway migrator is configured to include settings migrations (see below).

### Startup seeding (recommended)

On application startup, initialize the in-memory cache and seed defaults from environment variables into the database:
```kotlin
suspend fun seedSettings(
    systemSettingsService: SystemSettingsService,
    seedGlobalSettingsUseCase: SeedGlobalSettingsUseCase
) {
    // 1. Load existing settings into cache and subscribe to updates
    systemSettingsService.initialize()

    // 2. Seed defaults from ENV if they are not yet in DB
    seedGlobalSettingsUseCase()
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
[SystemSettingRepository]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/database/repository/SystemSettingRepository.kt
[SystemSettingRepositoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/database/repository/SystemSettingRepositoryImpl.kt
[SystemSettingsManager]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/manager/SystemSettingsManager.kt
[SystemSettingsManagerImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/manager/SystemSettingsManagerImpl.kt
[SystemSettingsService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/service/SystemSettingsService.kt
[SystemSettingsServiceImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/service/SystemSettingsServiceImpl.kt

[GlobalSettingsProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/global/provider/GlobalSettingsProvider.kt
[GlobalSettingsProviderImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/global/provider/GlobalSettingsProviderImpl.kt
[SeedGlobalSettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/usecase/system/globalsettings/SeedGlobalSettingsUseCase.kt

[SettingsModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/di/SettingsModules.kt