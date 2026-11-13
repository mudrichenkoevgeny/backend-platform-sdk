---
description: JVM backend testing framework standards (JUnit 5, MockK, Testcontainers)
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
