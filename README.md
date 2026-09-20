# backend-platform-sdk

A modular foundational SDK for building scalable Kotlin/Ktor microservices. Provides pre-configured core infrastructure for observability, database management, security, system settings, audit logging, and shared business features.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mudrichenkoevgeny/backend-platform-sdk-bom)](https://central.sonatype.com/artifact/io.github.mudrichenkoevgeny/backend-platform-sdk-bom)

## Installation

Use the BOM and add only the modules you need:

```kotlin
// build.gradle.kts
dependencies {
    implementation(platform("io.github.mudrichenkoevgeny:backend-platform-sdk-bom:0.0.18"))
    implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-common")
    implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-database")
    implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-security")
    implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-feature-user")
    // ... add other backend-platform-sdk modules as required
}
```

With a version catalog: declare BOM and module aliases in `gradle/libs.versions.toml`, then use `implementation(platform(libs.backend.platform.sdk.bom))` and `implementation(libs.backend.platform.sdk.core.common)` (and other aliases as needed).

## Modules

Published artifacts (versions aligned via the BOM):

- **core-common** — Base for all: Dual-connector Ktor setup (API + Management), DTO validation, error handling, localization, data masking, and logging ([module README](core/common/README.md)).
- **core-database** — PostgreSQL (Exposed ORM), Redis (Lettuce), Flyway migrations, JSONB support, and Pub/Sub capabilities ([module README](core/database/README.md)).
- **core-observability** — OpenTelemetry tracing and Micrometer/Prometheus metrics integration ([module README](core/observability/README.md)).
- **core-security** — Security primitives: Argon2 hashing, AES-256-GCM encryption, TOTP (RFC 6238), and MFA state management ([module README](core/security/README.md)).
- **core-settings** — DB-backed system settings with Redis-based cache synchronization across instances ([module README](core/settings/README.md)).
- **core-audit** — Infrastructure for background audit logging with visibility filtering and error parsing ([module README](core/audit/README.md)).
- **core-storage** — Object storage abstraction supporting S3 (AWS/MinIO) and Local Filesystem ([module README](core/storage/README.md)).
- **core-events** — Event publishing/subscribing via Kafka or In-Memory bus ([module README](core/events/README.md)).
- **feature-user** — Advanced IAM: Multi-method auth (Email, Phone, OAuth), JWT/Refresh sessions (with reuse detection), 2FA/TOTP, device login detection, lockouts, global/security settings APIs, audit log management, and WebSocket sync ([module README](feature/user/README.md)).
- **bom** — Dependency constraints for the modules above.

## Documentation & Architecture

- **[ARCHITECTURE.md](ARCHITECTURE.md)** — High-level architecture, module dependency graph, lifecycle state machines, and links to detailed sequence diagrams for runtime flows (authentication, MFA/security, WebSockets).
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — Build instructions, project health checks, and Maven Central publishing guidelines.
- **[AGENTS.MD](AGENTS.MD)** — Project standards, module map, KDoc requirements, and architectural rules for contributors and AI coding assistants.

## Integration Steps

1. **AppInfo & Scope** — Implement `AppInfo` (app name, version) and provide a `BackgroundScope` (`CoroutineScope`) in your Dagger graph. These are required for configuration, background tasks, and Redis sync.
2. **Common Setup** — Install `CommonModules` and bootstrap with `KtorServer.create(commonConfig)`. Configure observability using `telemetryProvider` and register your feature routers within the application routing block.
3. **Database & Migrations** — If using `core/database`, provide DB and Redis connection secrets. Include Flyway migration paths for all used modules (e.g., `db/migration/core/audit`, `db/migration/feature/user`). Alternatively, use the standalone `db-migrator` Docker container for running migrations.
4. **System Initialization** — On application startup, initialize the settings cache and seed default values.
5. **Real-time Sync** — For modules using WebSockets, ensure `feature:user` is installed to enable inter-service synchronization via Redis Pub/Sub.

For a full wiring example, see the **[sample](sample)** application.

## License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.
