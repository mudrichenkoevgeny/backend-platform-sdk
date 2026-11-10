---
description: JVM testing framework standards (JUnit 5, MockK, Testcontainers, Exposed) for Backend Platform
globs: "**/src/test/kotlin/**/*.kt"
alwaysApply: true
---

# Testing Standards and Conventions — Backend Platform Additions

This document defines the JVM/Backend testing methodology for backend platform modules (`backend-platform-sdk`). It builds upon `kotlin-testing-basics.md`.

## 1. JVM Framework & Tooling

- **JUnit Jupiter:** Use strictly `org.junit.jupiter.api.Test`.
- **Assertions:** Use `org.junit.jupiter.api.Assertions`. Do **not** use `kotlin.test`.
- **Mocking:** Use **MockK**. Use relaxed mocks for logs/void interactions; use explicit expectations for business logic.
- **Parameterized Tests:** Prefer `@ParameterizedTest` with `@EnumSource` or `@MethodSource` instead of manual loops.

## 2. Database & Integration Testing

- **Repository Testing:** Integration tests for Repositories must verify Flyway migrations and Exposed mapping against real database containers via Testcontainers (PostgreSQL).
- **Test Fixtures:** Shared test instances and builders belong in `src/testFixtures/kotlin/` using the `java-test-fixtures` plugin.
- **Test Constants:** Extract repeated test literals (IDs, UUIDs, sample JSON) into `private const val`.
