# feature/settings-api

HTTP routing module for global settings APIs.

## What it provides

- `SettingsRouter` as the aggregate router for settings-related HTTP endpoints.
- Public/open settings routes via `OpenGlobalSettingsRouter`.
- Management settings routes via `ManagementGlobalSettingsRouter` (JWT-protected).
- Use cases for reading and updating global settings over HTTP (seeding lives in `core/settings`).

## Usage

- Add a Gradle dependency on the `:feature:settings-api` project (it already depends on `:core:settings`).
- Include [SettingsModules] from `core/settings` in your Dagger component so system settings and [GlobalSettingsProvider] are wired. This module does not ship a separate Dagger `@Module` aggregate; HTTP routers use `@Inject` and become available once `:feature:settings-api` is on your component’s classpath.
- Register `SettingsRouter` in your Ktor `routing { }` block.

## Notes

- This module exposes HTTP APIs only.
- DB models/providers for settings are located in `core/settings`.

[SettingsModules]: ../core/settings/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/di/SettingsModules.kt
[GlobalSettingsProvider]: ../core/settings/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/global/provider/GlobalSettingsProvider.kt
