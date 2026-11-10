---
description: Backend platform module responsibility mapping, Dual-Connector architecture, and SDK workflow
alwaysApply: true
---

# AI Collaboration & Workflow Standards — Backend Platform Additions

This document defines the backend-specific extensions and architectural workflow requirements for the `backend-platform-sdk`. It builds upon `ai-collaboration-basics.md`.

## 1. Module Responsibility Mapping

Ensure code is placed in the correct module based on the following backend taxonomy:

| Category | Core Module (`core/`) - Infrastructure | Feature Module (`feature/`) - Business Domain |
| :--- | :--- | :--- |
| **Common** | `common`: Ktor bootstrap, Dual-connector, StatusPages. | — |
| **Data** | `database`: Exposed (PostgreSQL), Redis Pub/Sub, Flyway. | — |
| **Observability** | `observability`: OpenTelemetry tracing, Micrometer metrics. | — |
| **Security** | `security`: Hashing, Argon2/AES, MFA/TOTP primitives. | `user`: Security policy routes & sync. |
| **Settings** | `settings`: DB/Redis settings infrastructure. | `user`: Global config routes & sync. |
| **Audit** | `audit`: Background logging infrastructure. | `user`: Mgmt routes & filtering. |
| **Storage** | `storage`: S3 (AWS/MinIO) and Local FS abstractions. | — |
| **Events** | `events`: Kafka or In-Memory bus abstractions. | — |
| **User** | — | `user`: Auth, JWT, Multi-method sessions, settings, security, and audit APIs. |

**Strict Boundary:** Avoid moving app-specific logic from the `sample` module into SDK core modules. Core modules must remain generic and configuration-driven.

## 2. Architectural Workflow & Dual-Connector Ports

- **Dual-Connector Architecture:** All features must support the separation of the **Public API** (default port) and the **Management/Internal API** (management port).
- **Route Registration:** To add a route, implement `BaseRouter`, register it within the appropriate feature router, and document it with KDoc.
- **Leaf Constraints:** `core/common` is a leaf module and MUST NOT depend on any other internal `core/*` or `feature/*` modules.
- **Dagger DI Wiring:** Every core and feature module provides its own Dagger module (`*Module.kt`).
