---
description: Project identity, module boundaries, and SDK infrastructure standards
alwaysApply: true
---

# Backend Platform SDK — Overview

## Project identity
- **Type:** Kotlin/Ktor **backend SDK (library)**. Provides pre-configured infrastructure for scalable microservices.
- **Publishing:** Maven Central via `com.vanniktech.maven.publish`; version managed in root `build.gradle.kts`.
- **Coordinates:** Group `io.github.mudrichenkoevgeny`, artifact IDs `backend-platform-sdk-*`.
- **BOM:** Located at `:bom` for consistent version alignment across all modules.

## Tech stack
- **Runtime:** JVM 21 (managed via `jvmToolchain`).
- **Kotlin:** 2.x, Coroutines, Kotlinx Serialization.
- **Server:** Ktor 3.x (Netty) with Dual-connector setup (API + Management).
- **DI:** Dagger 2 + KSP (`javax.inject`). Prefer `@Singleton` for app-scoped logic.
- **Database:** PostgreSQL (Exposed) + Redis (Lettuce) + Flyway migrations.
- **Observability:** OpenTelemetry (tracing) and Micrometer/Prometheus (metrics).
- **Storage & Events:** AWS S3 (or Local FS) and Kafka (or In-Memory bus).

## Module Structure

### Core Modules (`core/`)
*Infrastructure primitives and base configurations.*

- **`core/common`:** The foundation. Ktor server bootstrap, DTO validation, error handling (StatusPages), localization, data masking, and logging.
- **`core/database`:** DB & Cache layer. Exposed/HikariCP, Flyway, and Redis Pub/Sub capabilities for synchronization.
- **`core/observability`:** Tracing and metrics integration for Ktor and internal SDK components.
- **`core/security`:** Crypto primitives (Argon2, AES-256-GCM) and MFA/TOTP lifecycle management.
- **`core/audit`:** Infrastructure for background audit logging with visibility filtering.
- **`core/settings`:** DB-backed system settings with Redis-based cache synchronization.
- **`core/storage` & `core/events`:** Abstractions for object storage and event-driven communication.

### Feature Modules (`feature/`)
*Pluggable business logic and API endpoints.*

- **`feature/user`:** Comprehensive IAM (Identity & Access Management). Auth (Email, Phone, OAuth), JWT sessions, and 2FA.
- **`feature/auditapi`:** Management HTTP endpoints for audit trail with permission-aware filtering.
- **`feature/securityapi` & `feature/settingsapi`:** Public and Management APIs for security/settings with real-time WebSocket sync.

### Other
- **`bom`:** Bill of Materials.
- **`sample`:** Reference host application demonstrating Dagger wiring and SDK integration. **Do not move SDK logic here.**

## Boundaries & Dependencies
- **`core/common`** is the leaf: It must not depend on any other internal `core/*` or `feature/*` modules.
- **Core-to-Core:** Modules like `core/settings` or `core/audit` depend on `core/database` and `core/common`.
- **Feature-to-Core:** Feature modules aggregate core modules (e.g., `feature/user` depends on `core/security`, `core/audit`, and `core/database`).
- **Framework Usage:** Unlike the "shared-foundation", this SDK **is** Ktor-centric and includes database drivers and framework-specific wiring.

## Guidelines for AI
- All architectural and coding standards are located in the `.agent/` directory.
- Refer to `AGENTS.md` for the full index of standards.
- **Integration Rule:** When adding features, ensure they support the Dual-connector Ktor setup (separation of public API and management ports).
- **Flyway Rule:** Always specify migration paths for used modules (e.g., `db/migration/core/audit`).

---
*Refer to `AGENTS.md` for the full list of project standards.*