---
description: Unit and integration test standards, MockK conventions, and manual execution rule
globs: "**/src/test/kotlin/**/*.kt"
alwaysApply: true
---

# Testing Standards and Conventions

This document defines the testing methodology for the `backend-platform-sdk`. These rules ensure that tests remain modular, fast, and dependency-correct.

## 1. Operational Policy (Strict)
- **Manual Execution Only:** Test runs must be triggered explicitly by the developer (e.g., via Gradle).
- **Execution Ban:** AI agents are **strictly prohibited** from initiating test suites or suggesting the user run them.
- **Runnable Code:** AI must write complete, runnable test code without `TODO` placeholders.

## 2. Dependency Management (Hard Requirement)
- **Explicit Declaration:** Every module's `build.gradle.kts` must declare **all** test dependencies explicitly (JUnit Jupiter, MockK, etc.).
- **No Transitive Assumptions:** Never assume a library is available via transitive dependencies.
- **Version Catalog:** Use aliases from `gradle/libs.versions.toml`.
- **Verification:** Before adding/editing tests, verify and update `testImplementation` in `build.gradle.kts` if needed in the same PR.

## 3. Framework & Tooling
- **JUnit 5 (Jupiter):** Use strictly `org.junit.jupiter.api.Test`.
- **Assertions:** Use `org.junit.jupiter.api.Assertions`. Do **not** use `kotlin.test`.
- **Mocking:** Use **MockK**. Use relaxed mocks for logs/void interactions; use explicit expectations for business logic.
- **Parameterized Tests:** Prefer `@ParameterizedTest` with `@EnumSource` or `@MethodSource` instead of manual loops.

## 4. Organization & Naming
- **Mirror Structure:** Test files must reside in the same package as the subject under test (e.g., `src/test/kotlin/io/github/.../manager/`).
- **Nomenclature:**
    - Unit Tests: `[Subject]Test.kt`.
    - Integration: `[Component]IntegrationTest.kt`.
- **Test Names:** Use descriptive backticked names: ``fun `should return fallback when locale is missing`()``.

## 5. Coding Standards in Tests
- **No FQN:** Fully Qualified Names are **strictly forbidden**. Use imports.
- **No Comments:** Tests must be self-documenting. No narrative comments.
- **Test Constants:** If a literal (String, UUID, Key) is used **more than once** in a test class, it **must** be extracted into a `private const val`.
- **Isolation:** Tests must be stateless and not rely on execution order.

## 6. Scope of Testing
- **Observable Behavior:** Cover all non-trivial classes (Public APIs, Managers, Repositories, Use Cases).
- **Serialization:** Ensure `core/common` serialization logic is tested for all DTOs.
- **Database:** Integration tests for Repositories should verify Flyway migrations and Exposed mapping.
- **Exclusions:** Trivial DTOs, constant-only objects, and simple enums may be skipped.

---
*Refer to `AGENTS.md` for the full list of project standards.*