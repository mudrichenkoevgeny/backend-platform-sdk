---
description: Unit and integration test standards, naming conventions, and targeted execution rules
glob: "**/*Test.kt"
alwaysApply: true
---

# Kotlin Testing Standards and Conventions

This document defines universal testing methodology and code quality standards for Kotlin projects.

## 1. Operational Policy (Targeted Execution)

- **No Full-Suite Auto-Runs:** Never automatically trigger full-project test suites (`./gradlew test`, `./gradlew check`) unless explicitly requested by the developer, to prevent resource exhaustion and maintain a focused development flow.
- **Mandatory Targeted Verification:** Whenever writing new tests, fixing a bug, or modifying production logic, the AI agent MUST compile and execute the specific affected test class or method (e.g., `./gradlew :module:test --tests "com.example.SubjectTest"` or platform-specific targets like `jvmTest` / `testDebugUnitTest`) to verify that the code compiles and tests pass.
- **Mandatory Bug Fix Test Coverage:** Whenever fixing a bug, defect, or regression, automated unit or integration test coverage MUST be written and executed via a targeted test run to verify that the bug is resolved and cannot resurface.

## 2. Test Organization & Naming

- **Mirror Structure:** Test files must reside in the same package structure as the subject under test.
- **1:1 Relationship:** Each primary production class or top-level component should have a corresponding test class (e.g., `UserValidator` -> `UserValidatorTest`).
- **File Naming:**
  - Unit Tests: `[Subject]Test.kt`
  - Mappers: `[Subject]MapperTest.kt`
  - Integration Tests: `[BoundaryName]IntegrationTest.kt`
- **Method Naming:** Use descriptive backticked names in Kotlin test methods: ``fun `should return fallback when locale parameter is missing`()``.

## 3. Coding Standards in Tests

- **No Implementation Comments:** Like production code, tests must be self-documenting. Do not write inline comments (`//` or `/* ... */`) inside test methods. Use descriptive backticked test method names instead.
- **No FQN:** Fully Qualified Names are strictly forbidden. Always use `import` statements.
- **Stateless Isolation:** Tests must be stateless and isolated. Do not rely on execution order or shared mutable global state.
- **Clean Data & Constant Extraction:** Extract repeated test literals (IDs, sample JSON strings, tokens used more than once in a test class) into `private const val` (or `public const val` inside a companion object) to eliminate "magic values" in test bodies.

## 4. Dependency Management Rules

- **Explicit Declaration Only:** Declare all test dependencies explicitly in `build.gradle.kts` for the appropriate source sets. Never rely on transitive test dependencies.
- **Version Catalog:** Always use `libs.versions.toml` aliases for all test libraries (e.g., `implementation(libs.kotlin.test)`).

---
*Refer to `AGENTS.md` for the full list of project standards.*
