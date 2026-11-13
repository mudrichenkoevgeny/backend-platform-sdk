---
description: Project identity, module boundaries, and SDK infrastructure standards
alwaysApply: true
---

# Backend Platform SDK — Overview

## Project identity
- **Type:** Kotlin/Ktor **backend SDK (library)**. Provides pre-configured infrastructure and feature modules for scalable microservices.
- **Publishing:** Maven Central via `com.vanniktech.maven.publish` (configured via `backendPublishConvention` Gradle plugin). Versions and group are managed in the root `gradle.properties`.
- **Coordinates:** Group `io.github.mudrichenkoevgeny`:
  - SDK modules: `backend-platform-sdk-<module-name>`
  - BOM: `backend-platform-sdk-bom` (`:bom`)
  - Gradle Plugin: `backend-platform-sdk-gradle-plugin` (`:tools:gradle-plugin`)

## Tech stack & Tooling
- **Runtime:** JVM (managed via `jvmToolchain` Java 21).
- **Kotlin:** Coroutines, Kotlinx Serialization, `explicitApi()` mode.
- **Server:** Ktor (Netty) with Dual-connector setup (Public API + Management API).
- **DI:** Dagger 2 + KSP (`javax.inject`). Prefer `@Singleton` for app-scoped logic.
- **Database:** PostgreSQL (Exposed) + Redis (Lettuce) + Flyway migrations.
- **Observability:** OpenTelemetry (tracing) and Micrometer/Prometheus (metrics).
- **Storage & Events:** AWS S3 (or Local FS) and Kafka (or In-Memory bus).
- **Quality & Build Plugins:** Detekt (with custom rules in `:tools:detekt-rules`), Ktlint, and custom Gradle convention plugins in `:tools:gradle-plugin`.

## Module Structure

### Core Modules (`core/`)
*Infrastructure primitives and base configurations.*

- **`core/common`:** Foundational base module. Ktor server bootstrap, DTO validation, error handling (StatusPages), localization, data masking, and logging.
- **`core/database`:** DB & Cache layer. Exposed/HikariCP, Flyway, and Redis Pub/Sub capabilities for synchronization.
- **`core/observability`:** Tracing (OpenTelemetry) and metrics (Micrometer/Prometheus) integration for Ktor and internal SDK components.
- **`core/security`:** Crypto primitives (Argon2, AES-256-GCM) and MFA/TOTP lifecycle management.
- **`core/audit`:** Infrastructure for background audit logging with visibility filtering.
- **`core/settings`:** DB-backed system settings with Redis-based cache synchronization.
- **`core/storage` & `core/events`:** Abstractions for object storage (S3/Local) and event-driven communication (Kafka/In-Memory).

### Feature Modules (`feature/`)
*Pluggable business logic and API endpoints.*

- **`feature/user`:** Comprehensive IAM (Identity & Access Management). Auth (Email, Phone, OAuth), JWT sessions, 2FA, access control (blocklists), account security (lockouts, new device detection), global and security settings management, audit trail logs, and WebSocket synchronization.

### Tools Modules (`tools/`)
*Build-time plugins and static analysis extensions.*

- **`tools/gradle-plugin`:** Gradle convention plugins (`backend-platform-sdk-gradle-plugin`):
  - `backendAgentSync`: Synchronizes `.agent/global/backend` AI rules into consumer microservice projects.
  - `backendQuality`: Configures Detekt (with custom rules) and Ktlint.
  - `backendKotlinConvention`: Configures Kotlin compiler options and `explicitApi()` mode.
  - `backendPublishConvention`: Configures Maven Central publishing, Dokka, and POM metadata.
- **`tools:detekt-rules`:** Custom Detekt rules enforcing platform-specific coding conventions and static analysis.

### Infrastructure & Reference
- **`bom`:** Bill of Materials (`backend-platform-sdk-bom`) for dependency version alignment.
- **`sample`:** Reference host application demonstrating Dagger DI wiring, Flyway migrations, and SDK module integration. **Never move SDK or business logic into sample.**

## Boundaries & Dependencies
- **`core/common` is the foundational base:** It MUST NOT depend on any other internal `core/*` or `feature/*` modules.
- **Core-to-Core:** Modules like `core/settings` or `core/audit` depend on `core/database` and `core/common`.
- **Feature-to-Core:** Feature modules aggregate core modules (e.g., `feature/user` depends on `core/security`, `core/audit`, `core/database`, and `core/common`).
- **Tools Isolation:** `tools/*` modules are build-time infrastructure and do not depend on runtime SDK modules.

## Guidelines for AI
- All architectural and coding standards are located in the `.agent/` directory.
- Refer to `AGENTS.md` for the full list of project standards.
- **Integration Rule:** When adding features, ensure they support the Dual-connector Ktor setup (separation of public API and management ports).
- **Flyway Rule:** Always specify migration paths for used modules (e.g., `db/migration/core/audit`).
- **Explicit API Rule:** All public declarations in SDK modules must use explicit visibility modifiers and return types (`explicitApi()`).

---
*Refer to `AGENTS.md` for the full list of project standards.*
