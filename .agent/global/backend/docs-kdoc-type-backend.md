---
description: Mandatory KDoc requirements for backend component types (Exposed, Dagger, Holders, Use Cases)
globs: "**/*.kt"
alwaysApply: true
---

# Documentation Requirements by Type — Backend Platform Additions

This document defines specific KDoc patterns for backend server component types. It builds upon `docs-kdoc-type-basics.md`.

## 1. Exposed Tables (`BaseTable` / `IdTable`)
For every Exposed table object or class:
- **Schema Management:** Type-level KDoc must reference the corresponding Flyway migration directory path.
- **Consumer Requirement:** Explicitly document that consumer applications must register this migration path in their Flyway migration locations.

## 2. Implementation Classes (Service & Manager Implementations)
- **Concrete Behavior:** Mandatory class-level KDoc explaining **how** the contract is fulfilled (e.g., "Delegates to Redis for caching", "Uses background CoroutineScope for persistence", "Logs via OpenTelemetry").
- **No Redundancy on Overrides:** Do **not** duplicate interface method documentation on overrides (`override fun`). Document overrides only if the implementation introduces unique constraints, background tasks, or side effects.

## 3. Dagger Modules (`@Module`)
- **Responsibility:** Every module must have class-level KDoc describing what it provides or binds.
- **Aggregates:** Aggregate modules (modules that use `includes = [...]`) **must** list all included submodules in their KDoc.

## 4. Configuration Holders (`*ConfigHolder`)
- **Rationale:** Object-level KDoc must explain **why** the holder exists (e.g., "Decoupled from DI to allow runtime reconfiguration without process restart").
- **State Policy:** Document the behavior of `get()` (default values) and `set()` (concurrency or lifecycle notes).

## 5. Use Cases (`*UseCase.kt`)
For every Use Case class and its `invoke` method:
- **Forbidden Boilerplate:** Do **NOT** use generic filler text like `"Use case to..."` or `"This class is a use case for..."`.
- **Class-level KDoc:** Must define:
  1. High-level business intent and role (starts with an active noun or gerund, e.g., `"Management component for..."` or `"Public authentication flow for..."`).
  2. Scope / Audience (e.g., Open vs Management API context).
  3. Key System Effects / Side Effects (e.g., database persistence, WebSocket broadcasts, Audit logging, Redis cache synchronization, or Event Bus publications).
- **`invoke` Method KDoc:** Must define:
  1. Action summary starting with a 3rd-person verb (e.g., `Updates...`, `Validates...`, `Sends...`).
  2. `**Allowed Account Statuses:**` Explicitly list allowed caller statuses (e.g., `[UserAccountStatus.ACTIVE]` or `Any (Public access)`).
  3. `**Security:**` Required roles/permissions, rate limits, session constraints, or MFA/TOTP requirements.
  4. `**Workflow:**` Step-by-step numbered breakdown of the execution flow (1, 2, 3...).
  5. `@param` for input parameters (DTOs, RequestContexts, etc.).
  6. `@return` description of returned payload and potential domain errors on failure.
