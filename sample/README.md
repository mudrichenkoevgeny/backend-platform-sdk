# sample

Reference host application that demonstrates how to wire **backend-platform-sdk** modules together
with Dagger and run a Ktor server.

## What it provides

- **Entrypoint**: [MainKt] sets UTC timezone, configures global holders ([PathResolverConfigHolder], [AppErrorParserConfigHolder]), builds Dagger component, and starts the server with shutdown handling.
- **Dagger wiring**:
    - [AppComponent] aggregates all SDK modules, including core infrastructure and **feature API modules** (`SettingsApi`, `SecurityApi`, `AuditApi`).
    - [AppModule] binds app metadata ([SampleAppInfo]) and shutdown logic ([AppShutdownHook]).
    - [AuditParsersModule] registers resource, action, and metadata parsers for all included features.
- **Ktor application module**: [module] installs SDK configuration and registers routes based on **AppInstanceMode**:
    - **PUBLIC**: Registers open routers for settings and user features.
    - **MANAGEMENT**: Registers management/admin routers for audit, security, and users.
    - **FULL**: Combines both public and management access.
    - Includes: HTTP defaults, serialization, status pages, global rate limit, WebSockets, and Swagger (in non-PROD).
- **Bootstrap**: [AppBootstrap] initializes database, verifies critical health, and warms up Redis and telemetry.
- **Graceful shutdown**: [AppShutdownHookImpl] stops the server, waits (best-effort) for audit persistence, then shuts down database and Redis.

## Environment variables

The sample module itself does not define its own env keys. It relies on configuration from SDK modules installed
in [AppComponent], for example:

- `core:common` (server/ports/cors/etc.)
- `core:database` (DB URL/credentials, Flyway migration locations, Redis URL)
- `core:settings` and `feature:settingsapi` (global settings seed values and sync)
- `core:security` and `feature:securityapi` (security policies and MFA)
- `feature:user` (auth, JWT, and user lifecycle settings)
- `core:audit` and `feature:auditapi` (audit persistence and management)

See the README files of those modules for the complete list of required variables.

## Usage

The reference wiring lives in:

- [MainKt] — creates the Dagger component, runs bootstrap, starts Ktor server.
- [module] — installs Ktor plugins and registers routers based on the instance mode.

### Startup seeding

On `ApplicationStarted`, the sample app triggers:

- **Health checks**: Non-critical health verification.
- **Data Seeding**: If not in `PUBLIC` mode, triggers use cases for admin accounts (with full permissions), global settings, security, and auth settings.

See: [module].

## Migrations

Database schema is created by Flyway migrations provided by installed modules. Ensure the Flyway migration
locations include the relevant paths, for example:

- `db/migration/core/settings/`
- `db/migration/core/audit/`
- `db/migration/feature/user/`

The exact list depends on the modules you include in the host application.

[MainKt]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/Main.kt
[module]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/Module.kt

[AppComponent]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/di/AppComponent.kt
[AppModule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/di/AppModule.kt
[AuditParsersModule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/di/AuditParsersModule.kt

[AppBootstrap]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/appbootstrap/AppBootstrap.kt

[SampleAppInfo]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/SampleAppInfo.kt

[AppShutdownHook]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/lifecycle/AppShutdownHook.kt
[AppShutdownHookImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/lifecycle/AppShutdownHookImpl.kt