---
description: JVM backend testing framework standards (JUnit 5, MockK, Testcontainers, targeted execution)
globs: "**/src/test/kotlin/**/*.kt"
alwaysApply: true
---

# Testing Standards and Conventions — Backend Platform Additions

This document defines the JVM/Backend testing methodology for backend platform modules. It builds upon `kotlin-testing-basics.md`.

## 1. JVM Framework & Tooling

- **JUnit Jupiter:** Use strictly `org.junit.jupiter.api.Test`.
- **Assertions:** Use `org.junit.jupiter.api.Assertions`. Do **not** use `kotlin.test`.
- **Mocking:** Use **MockK**. Use relaxed mocks for logs and void interactions; use explicit expectations for business logic.
- **Parameterized Tests:** Prefer `@ParameterizedTest` with `@EnumSource` or `@MethodSource` instead of manual loops.

## 2. Database & Integration Testing

- **Repository Testing:** Integration tests for Repositories and Exposed mapping must verify operations against real database containers via Testcontainers (PostgreSQL).
- **Test Fixtures:** Shared test instances and builders belong in `src/testFixtures/kotlin/` using the `java-test-fixtures` Gradle plugin.

## 3. Targeted Test Execution

- **Unit & Use Case Tests:** Always run targeted unit tests (`./gradlew :module:test --tests "com.example.SubjectTest"`) after modifying business logic or fixing bugs.
- **Integration Tests (Testcontainers):** Run targeted `*IntegrationTest` classes when modifying Exposed tables, queries, repositories, or Flyway migrations (assuming Docker daemon is available). Never run full-repository test suites (`./gradlew test`) automatically unless explicitly requested.
