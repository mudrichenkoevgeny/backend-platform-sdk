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
├── architecture (main)
│   └── BackendArchitectureAssertions.kt
└── architecture (test)
    ├── ArchitectureTest.kt
    ├── DocumentationArchitectureTest.kt
    ├── FileStructureArchitectureTest.kt
    └── RouteDocumentationArchitectureTest.kt
```

## Konsist Architecture Testing & Reuse

`:testing` exports `BackendArchitectureAssertions` in its `main` source set, allowing consumer backend services to run the same architecture checks used internally by the SDK.

To use it in a downstream service, create an architecture test class in your `src/test/kotlin/.../architecture/` package:

```kotlin
package com.example.backend.architecture

import io.github.mudrichenkoevgeny.backend.testing.architecture.BackendArchitectureAssertions
import org.junit.jupiter.api.Test

internal class ArchitectureTest {
    @Test
    fun `assert all consumer architecture rules`() {
        BackendArchitectureAssertions.assertAllConsumerRules()
    }
}
```
````
