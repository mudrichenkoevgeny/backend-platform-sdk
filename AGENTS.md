# Backend Platform SDK — Project Standards (Index)

This document is the entry point for architectural and coding standards. These rules ensure consistency across the modular Kotlin/Ktor microservice ecosystem for all contributors and AI agents.

## Core Principles

- **Precedence:** Local project standards (found in `.agent/`) override any global IDE, linter, or AI assistant defaults.
- **SDK Nature:** This is a **foundational backend library**, not a standalone application. Prefer configuration (env + DI), modularity, and extensibility over hardcoded assumptions.
- **FQN Forbidden:** Do not write fully qualified names (FQN) in expressions, types, or generics. Use imports or typealiases. Minimal FQN is allowed only if technically unavoidable (must be explained in the commit).
- **No Implementation Comments (Preserve KDoc):** Do not write inline implementation comments (`//` or `/* ... */`) inside code bodies; logic must be self-documenting. **CRITICAL:** KDoc documentation (`/** ... */`) on classes, interfaces, properties, and functions is structured API documentation, NOT code comments. AI must NEVER delete or strip KDoc documentation during code edits or refactorings!
- **No Trailing Commas:** Do not use trailing commas at the end of argument, parameter, or entry lists.
- **Null Safety (Strict Prohibition of `!!`):** The non-null assertion operator (`!!`) is strictly forbidden in production code (allowed only in test files). Handle nullability gracefully using safe calls (`?.`), Elvis operators (`?:`), safe casts (`as?`), or explicit smart-casts. Use `requireNotNull(...)` or `checkNotNull(...)` with descriptive messages only when asserting invariants or unrecoverable errors, never as a substitute for business logic error handling.
- **Mandatory Bug Fix Test Coverage:** Whenever fixing a bug or regression, you must write automated test coverage (unit or integration test) verifying that the bug is resolved and cannot resurface.
- **Manual Test Execution:** AI agents must not run tests or suggest running them. Execution is always triggered explicitly by the developer.
- **Complete Implementation:** All created or modified files must contain complete, production-ready code. Placeholder code, stubs, and partial omissions (`// ... rest of code`, `TODO`) are strictly prohibited unless explicitly requested.
- **Single Declaration Per File:** Every public `interface`, `class`, or `object` must be placed in its own separate, dedicated file matching the declaration name. Never combine an interface and its implementation class in a single file.
- **No Redundant Builds:** Do not run Gradle build tasks or attempt compilation after modifying documentation (KDoc), comments, or other non-executable changes that cannot possibly break the build.
- **Mandatory Sub-Agent Audit (Dual-Agent Review):** After implementing non-trivial code changes, new features, or complex refactorings, the AI agent MUST automatically invoke an independent reviewer sub-agent via the `task` tool to audit modified files against `AGENTS.md` guidelines, type safety, edge cases, and error handling before presenting the final result.
- **Git Ban (Strict Prohibition):** NEVER run git commands (`git add`, `git commit`, `git push`, etc.) via shell. Version control, staging, and committing are strictly the developer's responsibility. Do not attempt to save or document changes into git history.

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

## Detailed Standards (`.agent/`)

### Global Shared Standards (`.agent/global/`)
- **Common (`.agent/global/common/`):**
  - **`ai-collaboration-basics.md`** — Universal AI interaction constraints, operational rules (Git ban, test execution ban), dependency rules, and architectural layering.
- **Kotlin (`.agent/global/kotlin/`):**
  - **`kotlin-coding-style-basics.md`** — Naming, `when` subject rules, brace requirements, FQN/Comments ban, Trailing Commas ban, `!!` ban, companion objects, value classes, and `.value` usage.
  - **`docs-kdoc-basics.md`** — Language standards, link resolution rules, and KDoc Definition of Done.
  - **`docs-kdoc-type-basics.md`** — Specific KDoc patterns for DTOs, Error Codes, Sealed types, Routes, and Navigation Screen Params.
  - **`kotlin-testing-basics.md`** — Unit and integration test standards, mocking policy, and manual execution rule.
  - **`api-documentation-standard.md`** — Mandatory KDoc fields (Auth, Audit, Method, UI Presentation) for API route constants and navigation contracts.
- **Backend Platform (`.agent/global/backend/`):**
  - **`ai-collaboration-backend.md`** — Backend Platform SDK interaction constraints, module mapping, and Dual-Connector port standards.
  - **`docs-kdoc-type-backend.md`** — Specific KDoc patterns for backend server component types (Exposed tables, Dagger modules, Use Cases Section 10).
  - **`kotlin-testing-backend.md`** — JVM/Backend testing methodology (JUnit 5, MockK, Testcontainers, Exposed/Flyway integration tests).

### Local Standards (`.agent/local/`)
- **`project-overview.md`** — Identity, module boundaries, and consumer SDK relations.
- **`architecture-patterns.md`** — Entity projections, serialization, WebSocket contracts, navigation structures, UI values/configs, presigned uploads, and API error DTO structures.
- **`localization.md`** — Embedded strings in `shared-foundation`, modular error parsers, and `requestLocale()` resolution.
