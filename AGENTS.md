# Backend Platform SDK — Project Standards (Index)

This document is the entry point for architectural and coding standards. These rules ensure consistency across the modular Kotlin/Ktor microservice ecosystem for all contributors and AI agents.

## Core Principles

- **Precedence:** Local project standards (found in `.agent/`) override any global IDE, linter, or AI assistant defaults.
- **SDK Nature:** This is a **foundational backend library**, not a standalone application. Prefer configuration (env + DI), modularity, and extensibility over hardcoded assumptions.
- **Explicit API Mode:** All SDK modules compile with `explicitApi()`. Every public declaration must declare explicit visibility modifiers and return types.
- **FQN Forbidden:** Do not write fully qualified names (FQN) in expressions, types, or generics. Use imports or typealiases. Permitted only inside `@Deprecated(..., replaceWith = ReplaceWith(...))` import parameters, or when technically unavoidable (must be explained in the task summary).
- **No Agent-Generated Implementation Comments (Preserve KDoc & Developer TODOs):** Do not write inline implementation comments (`//` or `/* ... */`) or generate new `TODO`/`FIXME` markers inside code bodies; logic must be self-documenting. Never delete pre-existing developer `// TODO:` or `// FIXME:` comments unless the task explicitly resolves them. **CRITICAL:** KDoc documentation (`/** ... */`) on classes, interfaces, properties, and functions is structured API documentation, NOT code comments. AI must NEVER delete or strip KDoc documentation during code edits or refactorings! All KDoc must be written in English.
- **No Trailing Commas:** Do not use trailing commas at the end of argument, parameter, or entry lists.
- **Null Safety (Strict Prohibition of `!!`):** The non-null assertion operator (`!!`) is strictly forbidden in production code (allowed only in test files). Handle nullability gracefully using safe calls (`?.`), Elvis operators (`?:`), safe casts (`as?`), or explicit smart-casts. Use `requireNotNull(...)` or `checkNotNull(...)` with descriptive messages only when asserting invariants or unrecoverable errors, never as a substitute for business logic error handling.
- **Mandatory Bug Fix Test Coverage:** Whenever fixing a bug or regression, you must write automated test coverage (unit or integration test) and execute it via a targeted test run to verify that the bug is resolved and cannot resurface.
- **Targeted Test Execution:** Never auto-run full-project test suites (`./gradlew test`, `./gradlew check`) unless explicitly requested. However, whenever writing new tests, fixing a bug, or modifying production logic, the AI agent MUST compile and run the specific affected test class or method (`--tests`) to verify the code compiles and passes.
- **Complete Implementation:** All created or modified files must contain complete, production-ready code. Placeholder code, stubs, and partial omissions (`// ... rest of code`, `TODO()`) are strictly prohibited unless explicitly requested.
- **Single Declaration Per File:** Every primary public `interface`, `class`, or `object` must be placed in its own separate, dedicated file matching the declaration name. Never combine an interface and its implementation class or multiple unrelated public declarations in a single file (exceptions apply only to `sealed` hierarchies and extension parsers directly bound to a `value class` in the same file).
- **No Redundant Builds:** Skip Gradle build and test tasks **only** when changes are strictly limited to Markdown files (`*.md`) or text inside KDoc blocks (`/** ... */`). Any modification to executable code, tests, Gradle scripts, or version catalogs requires compilation/test verification.
- **Mandatory Sub-Agent Audit (Dual-Agent Review):** After implementing non-trivial code changes, new features, or complex refactorings, the AI agent MUST automatically invoke an independent reviewer sub-agent via the `task` tool to audit modified files against `AGENTS.md` guidelines, type safety, edge cases, and error handling before presenting the final result.
- **Git Mutation Ban (Read-Only Allowed):** NEVER run mutating git commands (`git add`, `git commit`, `git push`, `git reset`, `git checkout`, `git stash`, etc.) via shell. Version control, staging, and committing are strictly the developer's responsibility. Read-only diagnostic git commands (`git status`, `git diff`, `git log`, `git show`) are permitted.

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

- **`feature/user`:** IAM, Multi-method auth (Email, Phone, OAuth), JWT sessions (with reuse detection), 2FA lifecycle, account lockouts, access filtering, global/security settings APIs, audit log management, and WebSocket synchronization.

### Tooling, Infrastructure & Reference
- **`tools/gradle-plugin`:** Gradle convention plugins (`backendAgentSync`, `backendQuality`, `backendKotlinConvention`, `backendPublishConvention`).
- **`tools/detekt-rules`:** Custom Detekt static analysis rules enforcing platform coding conventions.
- **`bom`:** Bill of Materials (`backend-platform-sdk-bom`) for dependency version alignment.
- **`sample`:** Reference host application demonstrating Dagger DI wiring, Flyway migrations, and SDK integration. Never move SDK or business logic into `sample`.

## Detailed Standards (`.agent/`)

### Global Shared Standards (`.agent/global/`)
- **Common (`.agent/global/common/`):**
  - **`ai-collaboration-basics.md`** — Universal AI interaction constraints, operational rules (git mutation ban, targeted test execution), dependency rules, and architectural layering.
- **Kotlin (`.agent/global/kotlin/`):**
  - **`kotlin-coding-style-basics.md`** — Naming, `when` subject rules, brace requirements, FQN/Comments ban, Trailing Commas ban, `!!` ban, companion objects, value classes, and `.value` usage.
  - **`docs-kdoc-basics.md`** — Language standards, link resolution rules, and KDoc Definition of Done.
  - **`docs-kdoc-type-basics.md`** — Specific KDoc patterns for DTOs, Error Codes, Sealed types, Routes, and Navigation Screen Params.
  - **`kotlin-testing-basics.md`** — Unit and integration test standards, mocking policy, and targeted execution rules.
- **Shared / Contracts Libraries (`.agent/global/shared/`):**
  - **`api-documentation-basics.md`** — Mandatory KDoc fields (Auth, Audit, Method, UI Presentation) for API route constants and navigation contracts.
- **Backend Platform (`.agent/global/backend/`):**
  - **`ai-collaboration-backend.md`** — Backend architectural rules, Dual-Connector pattern, routing, and Dagger DI wiring.
  - **`architecture-patterns-backend.md`** — Server layering (Router, UseCase, Manager, Repository), validation, Flyway, and Redis Pub/Sub patterns.
  - **`docs-kdoc-type-backend.md`** — Specific KDoc patterns for backend server component types (Exposed tables, Dagger modules, ConfigHolders, Use Cases).
  - **`testing-backend.md`** — JVM/Backend testing methodology (JUnit 5, MockK, Testcontainers, targeted execution).

### Local Standards (`.agent/local/`)
- **`project-overview.md`** — Project identity, module boundaries, Flyway rules, and Explicit API requirements.
- **`architecture-patterns.md`** — SDK bootstrap lifecycle, graceful shutdown, Flyway migration paths, error argument pools (`publicArgs` vs `secretArgs`), and data masking.
- **`localization.md`** — Embedded strings in `shared-foundation`, modular error parsers, and `requestLocale()` resolution.
