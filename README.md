# backend-platform-sdk

A modular foundational SDK for building scalable Kotlin/Ktor microservices. Provides pre-configured core infrastructure for observability, database management, security, and shared business features.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mudrichenkoevgeny/backend-platform-sdk-bom)](https://central.sonatype.com/artifact/io.github.mudrichenkoevgeny/backend-platform-sdk-bom)

## Modules

| Module | Purpose |
|--------|--------|
| **core/common** | Base for all: Ktor server setup, `BaseRouter`, `CommonConfig`, error handling, serialization, Swagger, WebSockets, env/config, logging. Most other modules depend on it. |
| **core/database** | Exposed, PostgreSQL, Hikari, Flyway, Redis (Lettuce). Used by settings, security, audit, feature/user. |
| **core/observability** | OpenTelemetry, Micrometer/Prometheus, Ktor metrics. |
| **core/security** | Password validation, security settings (use cases, routes). |
| **core/settings** | Global/system settings, DB-backed. `GlobalSettingsRouter`, `SettingsFeatureRouter`. |
| **core/audit** | Audit events model and persistence. |
| **core/storage** | S3 (AWS SDK); file/blob storage abstraction. |
| **core/events** | Event publishing/subscribing (e.g. Kafka). `EventPublisher`, `EventSubscriber`. |
| **core/crosscutting** | Cross-cutting concerns (e.g. rate limiting). Uses common, security, audit. |
| **feature/user** | User and auth: registration, login (email + external), JWT + refresh tokens, password reset, sessions, user CRUD. Exposes auth routes, `UserFeatureRouter`. Depends on core: common, database, security, audit, settings, crosscutting. |

Depend only on what you need. Use the BOM for version alignment. Per-module details: [core/README.md](core/README.md), [core/common/README.md](core/common/README.md), [core/audit/README.md](core/audit/README.md).

## Installation

Use the BOM and add the modules you need:

```kotlin
// build.gradle.kts
dependencies {
    implementation(platform("io.github.mudrichenkoevgeny:backend-platform-sdk-bom:0.0.15"))
    implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-common")
    implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-database")
    // ... other core modules and backend-platform-sdk-feature-user as required
}
```

With a version catalog: declare the BOM and library aliases in `libs.versions.toml`, then `implementation(platform(libs.backend.sdk.bom))` and `implementation(libs.backend.sdk.core.common)` etc.

## Integration Steps

1. **AppInfo** — Implement `AppInfo` (app name, version) and bind it in your Dagger graph. The SDK needs it for config and responses.

2. **Common** — Install `CommonModules` in your app component. Provide `BackgroundScope` (e.g. from your app’s root scope) in the component; common does not create it. Bootstrap with `KtorServer.create(commonConfig) { module(applicationModule) }` and register your routes (including feature routers) inside the application module.

3. **Database** — If you use **core/database**, provide DB config (url, user, password) and include your Flyway migration paths (e.g. from each core module that has migrations, such as **core/audit** — see [core/audit/README.md](core/audit/README.md)).

4. **Feature/user** — If you use **feature/user**, install its Dagger modules, register `UserFeatureRouter` (and optionally `SettingsFeatureRouter`, `SecurityFeatureRouter`) in your `routing { }` block, and run the migrations for the user/settings tables.

For a full wiring example, see the [sample](sample) application.
