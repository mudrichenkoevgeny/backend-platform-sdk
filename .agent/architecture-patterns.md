---
description: Architecture, routing, bootstrap, config, and naming conventions
globs: "**/*.kt"
alwaysApply: true
---

# Architecture and Design Patterns

This document defines the structural standards for the `backend-platform-sdk`. All modules and contributions must adhere to these patterns to ensure consistency across the SDK and consuming applications.

## 1. Modular Hierarchy & Dependency Flow

The SDK follows a strict layering to prevent circular dependencies and ensure high cohesion:

- **Routing Layer:** Entry points implementing `BaseRouter`. Responsible only for request/response mapping and validation.
- **Use Case Layer:** Pure business logic. Each `UseCase` class should represent a single, atomic operation (e.g., `VerifyTotpUseCase`).
- **Manager/Orchestration Layer:** High-level services that coordinate multiple use cases or handle complex logic like session lifecycles.
- **Repository/Database Layer:** Data access using Exposed (PostgreSQL) or Lettuce (Redis).

## 2. Server Bootstrap & Lifecycle

- **Dual-Connector Setup:** All features must support the separation of the **Public API** (default port) and the **Management/Internal API** (management port) via `core/common` infrastructure.
- **Initialization:** Use `KtorServer.create(commonConfig)`. The host app is responsible for providing the `applicationModule` (Dagger) to bridge the SDK with app-specific logic.
- **AppInfo Requirement:** Every host app **must** provide an `AppInfo` implementation (Name, Version, Env) used for logging, metrics, and Redis key namespacing.

## 3. Configuration & Dependency Injection

- **Dagger 2:** Mandatory for all modules. Use `javax.inject` annotations.
- **Config Factories:** External configuration (Environment/Property) must be resolved via `*ConfigFactory` classes and bound as `@Singleton` in Dagger modules.
- **Naming Keys:** Env-based keys must be centralized in `*EnvKeys` objects for discoverability.
- **Background Scopes:** Long-running tasks (like Redis Pub/Sub listeners) must use the host-provided `BackgroundScope` to ensure graceful shutdown.

## 4. Routing & HTTP Documentation

Every route implementation or constant must be documented with a focus on its place in the ecosystem.

### Required KDoc Structure for Routes:
1. **HTTP Method & Path:** `**HTTP:** POST /api/v1/...`
2. **Summary:** Concise description of the business goal.
3. **Authorization:** List **Required Permissions** (e.g., `[SecurityPermissionCode.MANAGE_POLICIES]`).
4. **Audit Taxonomy:** State which `AuditActionType` is triggered.
5. **Real-time Sync:** Note if the operation triggers a WebSocket/Redis broadcast (e.g., "Broadcasts `SettingsWebSocketEventTypes.SETTING_CHANGED`").

## 5. Error Handling & Validation

- **Status Pages:** Use the centralized `AppErrorParser` in `core/common` to map domain/database exceptions to HTTP codes.
- **Validation:** Use `kotlin-serialization` with validation constraints. Use the dual-layer approach:
    1. **DTO Validation:** (Syntax/Structure) at the Routing level.
    2. **Business Validation:** (Domain Rules) inside Use Cases.
- **Masking:** Sensitive data (PII) in logs or audit trails must be redaction-aware using `core/common` masking utilities.

## 6. Database & Persistence

- **Migrations:** All DB changes must include Flyway scripts. Scripts are located in `db/migration/core/*` or `db/migration/feature/*`.
- **JSONB:** Leverage the custom JSONB support from `core/database` for semi-structured data in PostgreSQL.
- **Synchronization:** For stateful modules (Settings, Security), use Redis Pub/Sub to synchronize cache across multiple instances of the microservice.

## 7. Naming & Packaging

- **Package by Feature:** `io.github.mudrichenkoevgeny.sdk.[module].[layer]`.
- **Layers:** `route`, `usecase`, `manager`, `repository`, `di` (for modules), `config`.
- **Constants:** Use `PathConstants` for routes and `HeaderConstants` for custom HTTP headers.

---
*Refer to `AGENTS.md` for coding style rules (No FQN, No Comments).*