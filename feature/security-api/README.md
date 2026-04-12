# feature/security-api

HTTP routing module for security settings APIs (password policy, recent-authentication window, etc.).

## What it provides

- `SecurityRouter` as the aggregate router for security-related HTTP endpoints.
- Public/open routes via `OpenSecuritySettingsRouter` (e.g. password policy for clients).
- Management routes via `ManagementSecuritySettingsRouter` (JWT-protected updates).
- Use cases that delegate to `core/security` providers and services.

## Usage

- Add a Gradle dependency on the `:feature:security-api` project (it already depends on `:core:security`, `:core:settings`, `:core:audit`, and `:feature:user`).
- Include [SecurityModules] from `core/security` in your Dagger component so [SecuritySettingsProvider] and related types are bound. This module does not ship a separate Dagger `@Module` aggregate; HTTP routers use `@Inject` and become available once `:feature:security-api` is on your component’s classpath.
- Register `SecurityRouter` in your Ktor `routing { }` block.

## Notes

- This module exposes HTTP APIs only.
- Persisted security settings and seeding use cases live in `core/security` (and system settings storage from `core/settings`).

[SecurityModules]: ../core/security/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/di/SecurityModules.kt
[SecuritySettingsProvider]: ../core/security/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/provider/SecuritySettingsProvider.kt
