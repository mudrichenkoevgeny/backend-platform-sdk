---
description: Universal backend architecture, routing, Dagger DI, validation, and persistence patterns
globs: "**/*.kt"
alwaysApply: true
---

# Backend Architecture and Design Patterns — Basics

This document defines universal server-side architecture, dependency injection, and routing standards across Kotlin/Ktor backend services.

## 1. Modular Hierarchy & Dependency Flow
The backend follows strict vertical and horizontal layering:
- **Routing Layer:** Entry points implementing `BaseRouter`. Responsible strictly for request/response serialization, route mapping, and DTO syntax validation.
- **Use Case Layer:** Pure, atomic business operations (e.g., `VerifyTotpUseCase`). Each class handles a single business flow.
- **Manager/Orchestration Layer:** High-level domain services coordinating multiple use cases, background tasks, or complex lifecycles (e.g., session revocation).
- **Repository/Database Layer:** Direct data access utilizing Exposed (PostgreSQL) or Lettuce (Redis).

## 2. Server Infrastructure & Dual-Connector
- **Dual-Connector Separation:**
  - **Public API:** Default connector for public, client, and webhook traffic.
  - **Management API:** Dedicated connector for internal traffic, health probes (`/health/live`, `/health/ready`), and metrics.
- **Reverse Proxy Headers:** Servers running behind proxies (Nginx, Envoy) must install `ForwardedHeaders` and `XForwardedHeaders` to resolve accurate client IPs and URI schemes.
- **App Identification:** Services must inject or bind an application metadata provider (`AppInfo`: Name, Version, Environment) for structured logging, metrics, and Redis key namespacing.

## 3. Dependency Injection & Configuration (Dagger)
- **Dagger 2:** Mandatory DI framework across all modules using `javax.inject` annotations (`@Inject`, `@Singleton`, `@Provides`, `@Binds`).
- **External Configuration:** Environment variables and external properties must be resolved via dedicated `*ConfigFactory` classes and bound as singletons.
- **Centralized Keys:** Environment variable keys must be declared in `*EnvKeys` objects using `public const val`.
- **Background Operations:** Long-running coroutines or event listeners must execute within a supervised application `BackgroundScope` to guarantee graceful shutdown.

## 4. Routing & HTTP KDoc Standards
Every endpoint constant or route implementation must document its ecosystem impact:
1. **HTTP Method & Path:** `**HTTP:** POST /api/v1/...`
2. **Summary:** Concise description of the endpoint's purpose.
3. **Authorization:** Explicit list of required permissions or roles (e.g., `[SecurityPermissionCode.MANAGE_POLICIES]`), or `None (Public access)`.
4. **Audit Taxonomy:** Associated audit action identifier triggered by the operation.
5. **Real-time Sync:** Real-time events published via WebSocket or Redis Pub/Sub, if applicable.

## 5. Dual-Layer Validation
- **DTO Validation (Syntax/Structure):** Enforce input shape and format constraints at the routing layer using `kotlinx.serialization` and field validators.
- **Business Validation (Domain Rules):** Validate domain invariants (existence, state limits, permission checks) strictly inside Use Cases.

## 6. Database, Persistence & Real-Time Sync
- **Database Migrations:** Every database schema change must be managed through Flyway migration scripts.
- **Distributed Cache Sync:** Stateful services must use Redis Pub/Sub to invalidate or synchronize local caches across scaled service replicas.
- **Stateless WebSockets:** WebSocket broadcasts must be published over a distributed pub/sub bus (Redis) so messages reach clients connected to any server instance behind the load balancer.

## 7. Packaging & Layer Naming
- **Layer Packages:** Organize feature code by functional role: `route`, `usecase`, `manager`, `repository`, `di`, `config`.
- **Constants:** Declare endpoints in dedicated `PathConstants` and custom headers in `HeaderConstants`.
