# sample

Reference host application that demonstrates how to wire **backend-platform-sdk** modules together
with Dagger and run a Ktor server.

## What it provides

- **Entrypoint**: [MainKt] starts the server and installs shutdown handling.
- **Dagger wiring**:
  - [AppComponent] aggregates all SDK modules + [AppModule].
  - [AppModule] binds sample-specific app metadata ([SampleAppInfo]) and shutdown hook ([AppShutdownHook]).
- **Ktor application module**: [module] installs SDK configuration and registers routes:
  - HTTP defaults, serialization, status pages, global rate limit, WebSockets
  - observability (OpenTelemetry + metrics) and `/metrics` endpoint
  - Swagger endpoints in non-PROD environments
  - feature routers (settings, security, user) and authenticated WebSocket router
- **Bootstrap**: [AppBootstrap] initializes database, verifies critical health, warms up Redis and telemetry.
- **Graceful shutdown**: [AppShutdownHookImpl] stops the server, waits (best-effort) for audit persistence, then
  shuts down database and Redis.

## Environment variables

The sample module itself does not define its own env keys. It relies on configuration from SDK modules installed
in [AppComponent], for example:

- `core:common` (server/ports/cors/etc.)
- `core:database` (DB URL/credentials, Flyway migration locations, Redis URL)
- `core:settings` (global settings seed values)
- `core:security`, `core:events`, `core:observability`, `core:storage`, `core:crosscutting`
- `feature:user` (auth/user feature settings)

See the README files of those modules for the complete list of required variables.

## Usage

The reference wiring lives in:

- [MainKt] — creates the Dagger component, runs bootstrap, starts Ktor server.
- [module] — installs Ktor plugins and registers routes/seeders.

### Startup seeding

On `ApplicationStarted`, the sample app triggers:

- non-critical health checks
- seeding use cases (admin accounts, global settings, security settings, auth settings)

See: [module].

## Migrations

Database schema is created by Flyway migrations provided by installed modules. Ensure the Flyway migration
locations include the relevant paths, for example:

- `db/migration/core/settings/`
- `db/migration/core/audit/`

The exact list depends on the modules you include in the host application.

[MainKt]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/Main.kt
[module]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/Module.kt

[AppComponent]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/di/AppComponent.kt
[AppModule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/di/AppModule.kt

[AppBootstrap]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/appbootstrap/AppBootstrap.kt

[SampleAppInfo]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/SampleAppInfo.kt

[AppShutdownHook]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/lifecycle/AppShutdownHook.kt
[AppShutdownHookImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/sample/lifecycle/AppShutdownHookImpl.kt
