# Backend Platform SDK — Project Standards (Index)

This document is the entry point for architectural and coding standards. These rules ensure consistency across the modular Kotlin/Ktor microservice ecosystem for all contributors and AI agents.

## Core Principles

- **Precedence:** Local project standards (found in `.agent/`) override any global IDE, linter, or AI assistant defaults.
- **SDK Nature:** This is a **foundational library**, not a standalone application. Prefer configuration (env + DI), modularity, and extensibility over hardcoded assumptions.
- **FQN Forbidden:** Do not write fully qualified names (FQN) in expressions, types, or generics. Use imports or typealiases. Minimal FQN is allowed only if technically unavoidable (must be explained in the commit).
- **No Comments:** Do not write comments in the code. Logic must be self-documenting through naming and structural clarity.
- **Manual Test Execution:** AI agents must not run tests or suggest running them. Execution is always triggered explicitly by the developer.

## Module Map

### Core Modules (`core/*`)
*Foundational infrastructure and shared logic.*

- **`core/common`:** Ktor setup (API + Management), DTO validation, localization, error handling, and data masking.
- **`core/database`:** PostgreSQL (Exposed), Redis (Lettuce), Flyway migrations, and JSONB support.
- **`core/observability`:** OpenTelemetry (tracing) and Micrometer/Prometheus (metrics).
- **`core/security`:** Argon2, AES-256-GCM, TOTP (RFC 6238), and MFA state primitives.
- **`core/audit`:** Background audit logging infrastructure and visibility filtering.
- **`core/storage`:** S3 (AWS/MinIO) and Local Filesystem abstractions.
- **`core/events`:** Event bus abstraction (Kafka or In-Memory).
- **`core/settings`:** DB-backed system settings with Redis-based cache synchronization.

### Feature Modules (`feature/*`)
*High-level business logic and HTTP API endpoints.*

- **`feature/user`:** IAM, Multi-method auth (Email, Phone, OAuth), JWT sessions, and 2FA lifecycle.
- **`feature/audit-api`:** Management API for audit trails with permission-aware filtering.
- **`feature/security-api`:** Policy management and real-time WebSocket synchronization.
- **`feature/settings-api`:** Public and Management configuration APIs with inter-service sync.

## Detailed Standards (`.agent/`)

- **`project-overview.md`** — Project identity, Dagger/DI setup, module boundaries, and `AppInfo` requirements.
- **`architecture-patterns.md`** — Ktor bootstrap, Dual-connector setup, Redis Pub/Sub sync, and Repository patterns.
- **`docs-kdoc-basics.md`** — Language standards (English), link resolution, and KDoc Definition of Done.
- **`docs-kdoc-type-requirements.md`** — Specific KDoc patterns for DTOs, Error Codes, Sealed classes, and Database entities.
- **`localization.md`** — Resource directory layout (ISO codes) and naming for error/email JSON templates.
- **`kotlin-coding-style.md`** — Naming conventions, `when` subject rules, brace requirements, and FQN/Comments ban.
- **`testing-conventions.md`** — JUnit 5/MockK standards, explicit Gradle dependencies, and manual execution policy.
- **`ai-collaboration-workflow.md`** — AI constraints, Version Catalog management, and SDK vs Sample application boundaries.