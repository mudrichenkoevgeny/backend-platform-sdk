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

---
*Refer to `AGENTS.md` for the full list of project standards.*