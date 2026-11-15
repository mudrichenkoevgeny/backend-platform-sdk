````# backend-platform-sdk-testing (`:testing`)

Comprehensive testing harness, cross-module integration test helpers, Testcontainers utilities, and repository-wide Konsist architecture suite for `backend-platform-sdk`.

`:testing` allows consumer microservices (`<name>-backend`) and internal SDK components to write clean unit, integration, and contract tests using standardized test fixtures without writing custom mocks.

---

## Capabilities & Key Features

1. **Repository-Wide Konsist Architecture Suite:**
   Centralized architecture tests enforcing layer isolation, package structure, "1 public class per file", route documentation, and KDoc API documentation completeness across all modules.
2. **Aggregated Test Fixtures:**
   Re-exports `testFixtures` from all core and feature modules (`core:common`, `core:database`, `core:security`, `core:audit`, `core:settings`, `core:storage`, `feature:user`).
3. **Docker & Testcontainers Infrastructure:**
   - **`@RequiresDocker`** & **`DockerRequiredCondition`** — Pre-flight Docker availability check preventing Testcontainers test failures when Docker is unavailable.
   - **`ContractTestUtils`** — Ktor StatusPages and JSON serialization contract assertions.

---

## Installation & Consumption

In Gradle build scripts:

```kotlin
// build.gradle.kts
dependencies {
    testImplementation(libs.backend.platform.sdk.testing)
}
```

---

## Architecture & Package Structure

```
io.github.mudrichenkoevgeny.backend.testing
└── architecture (test)
    ├── ArchitectureTest.kt
    ├── DocumentationArchitectureTest.kt
    ├── FileStructureArchitectureTest.kt
    └── RouteDocumentationArchitectureTest.kt
```
````
