---
description: Mandatory KDoc requirements for backend component types (Exposed, Dagger, Errors, Holders, Use Cases)
globs: "**/*.kt"
alwaysApply: true
---

# Documentation Requirements by Type — Backend Platform Additions

This document defines specific KDoc patterns for backend server component types in the `backend-platform-sdk`. It builds upon `docs-kdoc-type-basics.md`.

## 1. Exposed Tables (`BaseTable` / `IdTable`)
For every Exposed table object or class:
- **Schema Management:** Type-level KDoc must include:
  `Schema is created by a Flyway migration in [db/migration/<module>/<feature>/].`
- **Consumer Requirement:** Explicitly state:
  `The host app must include this path in its Flyway migration locations.`

## 2. Implementation Classes (Service & Manager Implementations)
- **Concrete Behavior:** Mandatory class-level KDoc explaining **how** it works (e.g., "Delegates to Redis for caching", "Uses background CoroutineScope for persistence", "Logs via OpenTelemetry").
- **No Redundancy:** Do **not** duplicate interface method documentation on overrides (`override fun`). Document only if the implementation adds unique constraints or side effects.

## 3. Dagger Modules (`@Module`)
- **Responsibility:** Every module must have class-level KDoc describing what it provides or binds.
- **Aggregates:** Aggregate modules (modules that use `includes = [...]`) **must** list all included submodules in their KDoc.

## 4. Error Models & Argument Pools
- **Field Intent:**
    - `publicArgs`: Document that these are for client-side localization.
    - `secretArgs`: Document that these are strictly for internal logs and must never be exposed to the client.
- **Variants:** Each variant must have a description and `@param` documentation, specifying which field goes to which argument pool.

## 5. Configuration Holders (`*ConfigHolder`)
- **Rationale:** Object-level KDoc must explain **why** the holder exists (e.g., "Decoupled from DI to allow runtime reconfiguration without process restart").
- **State Policy:** Document the behavior of `get()` (default values) and `set()` (concurrency or lifecycle notes).

## 6. Use Cases (`*UseCase.kt`)
For every Use Case class and its `invoke` method:
- **Forbidden Boilerplate:** Do **NOT** use generic filler text like `"Use case to..."` or `"This class is a use case for..."`.
- **Class-level KDoc:** Must define:
  1. High-level business intent and role (starts with an active noun or gerund, e.g., `"Management component for..."` or `"Public authentication flow for..."`).
  2. Scope / Audience (e.g., Open vs Management API context).
  3. Key System Effects / Side Effects (e.g., database persistence, WebSocket broadcasts, Audit logging, Redis cache synchronization, or Event Bus publications).
- **`invoke` Method KDoc:** Must define:
  1. Action summary starting with a 3rd-person verb (e.g., `Updates...`, `Validates...`, `Sends...`).
  2. `**Allowed Account Statuses:**` Explicitly list allowed caller statuses (e.g. `[UserAccountStatus.ACTIVE]` or `Any (Public access)`).
  3. `**Security:**` Required roles/permissions, rate limits, session constraints, or MFA/TOTP requirements.
  4. `**Workflow:**` Step-by-step numbered breakdown of the execution flow (1, 2, 3...).
  5. `@param` for input parameters (DTOs, RequestContexts, etc.).
  6. `@return` description of returned `[AppResult.Success]` payload and potential `[AppError]` domain errors on failure.
