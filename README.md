# backend-platform-sdk

A modular foundational SDK for building scalable Kotlin/Ktor microservices. Provides pre-configured core infrastructure for observability, database management, security, and shared business features.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mudrichenkoevgeny/backend-platform-sdk-bom)](https://central.sonatype.com/artifact/io.github.mudrichenkoevgeny/backend-platform-sdk-bom)

## Modules

| Module | Purpose |
| :--- | :--- |
| **core/common** | Base for all: Dual-connector Ktor setup (API + Management), DTO validation, error handling, localization, data masking, and logging. |
| **core/database** | PostgreSQL (Exposed), Redis (Lettuce), Flyway migrations. Provides JSONB support and Pub/Sub capabilities. |
| **core/observability** | OpenTelemetry tracing and Micrometer/Prometheus metrics integration. |
| **core/security** | Security primitives: Argon2 hashing, AES-256-GCM encryption, TOTP (RFC 6238), and MFA state management. |
| **core/settings** | DB-backed system settings with Redis-based cache synchronization across instances. |
| **core/audit** | Infrastructure for background audit logging with visibility filtering and error parsing. |
| **core/storage** | Object storage abstraction supporting S3 (AWS/MinIO) and Local Filesystem. |
| **core/events** | Event publishing/subscribing via Kafka or In-Memory bus. |
| **feature/user** | Advanced IAM: Multi-method auth (Email, Phone, OAuth), JWT/Refresh sessions, 2FA/TOTP, and full user lifecycle. |
| **feature/audit-api** | HTTP API for audit trail management with permission-aware filtering. |
| **feature/security-api** | Security policy management with real-time WebSocket synchronization. |
| **feature/settings-api** | Public and management APIs for global configuration with WebSocket sync. |

## Installation

Use the BOM and add the modules you need:

```kotlin
// build.gradle.kts
dependencies {
    implementation(platform("io.github.mudrichenkoevgeny:backend-platform-sdk-bom:0.0.16"))
    implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-common")
    implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-database")
    // ... other core modules and backend-platform-sdk-feature-user as required
}
```

## Integration Steps

1. **AppInfo & Scope** — Implement `AppInfo` (app name, version) and provide a `BackgroundScope` (CoroutineScope) in your Dagger graph. These are required for configuration, background tasks, and Redis sync.

2. **Common** — Install `CommonModules` and bootstrap with `KtorServer.create(commonConfig)`. Configure observability using the `telemetryProvider` and register your feature routers within the application routing block.

3. **Database** — If using `core/database`, provide DB and Redis connection secrets. Include Flyway migration paths for all used modules (e.g., `db/migration/core/audit`, `db/migration/feature/user`).

4. **System Initialization** — On application startup, you must initialize the settings cache and seed default values:

5. **Real-time Sync** — For modules using WebSockets (settings, security, user), ensure the respective API modules are installed to enable inter-service synchronization via Redis Pub/Sub.

For a full wiring example, see the [sample](sample) application.
