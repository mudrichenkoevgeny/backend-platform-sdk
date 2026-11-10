---
description: Unit and integration test standards, naming conventions, and manual execution rule
glob: "**/*Test.kt"
alwaysApply: true
---

# Kotlin Testing Standards and Conventions

This document defines universal testing methodology and code quality standards for Kotlin projects.

## 1. Operational Policy (Strict)
- **Manual Execution Only:** Test runs must be triggered explicitly by the developer (e.g., `./gradlew test`).
- **Execution Ban:** AI agents and IDE background processes are strictly prohibited from automatically initiating test suites.
- **Resource Focus:** Tests should not run on every file save to prevent resource exhaustion and maintain a focused development flow.
- **Mandatory Bug Fix Test Coverage:** Whenever fixing a bug, defect, or regression, automated unit or integration test coverage MUST be written to verify that the bug is resolved and cannot resurface.

## 2. Test Organization & Naming
- **Mirror Structure:** Test files must reside in the same package structure as the subject under test.
- **1:1 Relationship:** Each primary production class or top-level component should have a corresponding test class (e.g., `UserValidator` -> `UserValidatorTest`).
- **File Naming:**
  - Unit Tests: `[Subject]Test.kt`
  - Mappers: `[Subject]MapperTest.kt`
  - Integration Tests: `[BoundaryName]IntegrationTest.kt`
- **Method Naming:** Use descriptive backticked names in Kotlin test methods: ``fun `should return fallback when locale parameter is missing`()``.

## 3. Coding Standards in Tests
- **No Implementation Comments:** Like production code, tests must be self-documenting. Use descriptive backticked test method names instead of inline comments.
- **No FQN:** Fully Qualified Names are strictly forbidden. Always use `import` statements.
- **Stateless Isolation:** Tests must be stateless and isolated. Do not rely on execution order or shared mutable global state.
- **Clean Data & Constant Extraction:** Extract repeated test literals (IDs, sample JSON strings, tokens used more than once in a test class) into `private const val` (or `public const val` inside a companion object) to eliminate "magic values" in test bodies.

## 4. Dependency Management Rules
- **Explicit Declaration Only:** Declare all test dependencies explicitly in `build.gradle.kts` for the appropriate source sets. Never rely on transitive test dependencies.
- **Version Catalog:** Always use `libs.versions.toml` aliases for all test libraries (e.g., `implementation(libs.kotlin.test)`).

---
*Refer to `AGENTS.md` for the full list of project standards.*
