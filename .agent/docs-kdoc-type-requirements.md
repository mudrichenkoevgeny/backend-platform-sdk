---
description: Mandatory KDoc requirements by component type (Database, DI, Errors, Routes, Holders)
globs: "**/*.kt"
alwaysApply: true
---

# Documentation Requirements by Type — Mandatory

This document defines specific KDoc patterns and API standards for various component types in the `backend-platform-sdk`.

## 1. Exposed Tables (`BaseTable` / `IdTable`)
For every Exposed table object or class:
- **Schema Management:** Type-level KDoc must include:
  `Schema is created by a Flyway migration in [db/migration/<module>/<feature>/].`
- **Consumer Requirement:** Explicitly state:
  `The host app must include this path in its Flyway migration locations.`

## 2. Public API & Interfaces
- **Class-level:** Must define the role, responsibility, and "Source of Truth" of the component.
- **Methods:** Every method must have a summary + `@param` for all parameters + `@return` (if not `Unit`).

## 3. Implementation Classes (Implementations of Interfaces)
- **Concrete Behavior:** Mandatory class-level KDoc explaining **how** it works (e.g., "Delegates to Redis for caching", "Uses background CoroutineScope for persistence", "Logs via OpenTelemetry").
- **No Redundancy:** Do **not** duplicate interface method documentation on overrides (`override fun`). Document only if the implementation adds unique constraints or side effects.

## 4. Dagger Modules (`@Module`)
- **Responsibility:** Every module must have class-level KDoc describing what it provides or binds.
- **Aggregates:** Aggregate modules (modules that use `includes = [...]`) **must** list all included submodules in their KDoc.

## 5. Error Models & Sealed Hierarchies
- **Field Intent:**
    - `publicArgs`: Document that these are for client-side localization.
    - `secretArgs`: Document that these are strictly for internal logs and must never be exposed to the client.
- **Variants:** Each variant must have a description and `@param` documentation, specifying which field goes to which argument pool.

## 6. Configuration Holders (`*ConfigHolder`)
- **Rationale:** Object-level KDoc must explain **why** the holder exists (e.g., "Decoupled from DI to allow runtime reconfiguration without process restart").
- **State Policy:** Document the behavior of `get()` (default values) and `set()` (concurrency or lifecycle notes).

## 7. Value Classes & Identifiers
For every `@JvmInline value class` wrapping an ID or parseable type:
- **Standard API Requirements:**
    1. `asString()` (or `asHexDashString()`): Canonical string representation.
    2. `generate()`: Companion function to create a new instance.
    3. **Mandatory Extensions:** In the same file, provide `String.toXxxOrNull()` and `String.toXxxOrThrow()` for safe parsing.

## 8. HTTP Route Constants
Every `const val` representing a route must follow the "Gold Standard":
- **HTTP Method:** Explicitly stated in backticks (e.g., `GET`).
- **Self-Contained Docs:** Intentionally duplicate Auth, Audit, and Pagination info. Do **not** use "See [OtherRoute]".
- **Links:** Referenced types in KDoc must be importable. **Forbidden:** using FQN in links like `[Foo][full.package.Foo]`.

## 9. Localized Strings & Templates
- **Conciseness:** Keep templates short; avoid generic filler text.
- **Security:** Ensure only `publicArgs` placeholders are used in user-facing templates.

## 10. Use Cases (`*UseCase.kt`)
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

---
*Refer to `AGENTS.md` for the full list of project standards.*
