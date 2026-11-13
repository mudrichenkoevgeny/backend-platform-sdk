# Backend Platform SDK — Development Roadmap

This document outlines the strategic plan for improving code quality, testing infrastructure, and CI/CD pipelines across the `backend-platform-sdk` ecosystem. As a foundational library for microservices, high architectural consistency, test coverage of distributed state, and clean deployment automation are top priorities.

## 1. Static Analysis & Compiler Configuration

- [x] **Strict Kotlin Compiler Configuration (Typecheck)**
  - `-Werror`: Treat all compiler warnings as errors (`allWarningsAsErrors.set(true)`).
  - **Explicit API Mode (`-Xexplicit-api=strict`)**: Enforce explicit visibility modifiers and return types across all public modules (`core/*`, `feature/*`) to prevent leaking implementation details.
- [x] **Detekt 2.x / 1.23.8 (Static Analysis)**
  - Integration via Gradle (`io.gitlab.arturbosch.detekt`).
  - Base rulesets: Cyclomatic complexity, mandatory braces for conditionals, naming conventions, and strict prohibition of `!!` non-null assertions in production source sets (`UnsafeCallOnNullableType`).
  - Custom RuleSets / Configuration (`config/detekt/detekt.yml`).
- [x] **Ktlint (Formatting Control)**
  - Integration via Gradle plugin (`org.jlleitschuh.gradle.ktlint`).
  - Strict code style enforcement: mandatory braces, no trailing commas, import ordering, and automated removal of unused imports via `.editorconfig`.
- [x] **Lefthook (Git Hooks)**
  - Repository-level pre-commit hooks (`lefthook.yml`) blocking commits on fast static analysis (`ktlintCheck`, `detekt`, fast `konsist` structural checks). Heavy Testcontainers test suites remain strictly in CI.

## 2. Architectural Control (Konsist)

Konsist enforces rules defined in `.agent/` across the entire backend SDK codebase:

- [x] **Module Hierarchy & Isolation:**
  - Verify that `core/common` is a pure leaf module and does not depend on any internal `core/*` or `feature/*` modules.
  - Enforce strict layer boundaries within modules: `repository` does not depend on `route` or `usecase` layers.
- [ ] **Single Declaration Per File:**
  - Verify that every public `interface`, `class`, or `object` resides in its own dedicated file matching the declaration name.
- [ ] **Route Documentation Parity:**
  - Verify that every `BaseRouter` implementation and route constant defines KDoc containing the mandatory sections: `**HTTP:**`, `Authorization`, `Audit Taxonomy`, and `Real-time Sync`.
- [x] **Configuration & DI Standards:**
  - Verify that all `*ConfigFactory` instances are bound as `@Singleton` in Dagger modules.
- [x] **Null Safety Enforcement:**
  - Enforce the absolute prohibition of the `!!` operator in production source sets (`src/main/kotlin`).
- [ ] **Mandatory Public KDoc:**
  - Verify structured KDoc presence on all public declarations across all SDK modules.

## 3. API Backward Compatibility Protection

- [x] **Binary Compatibility Validator**
  - `org.jetbrains.kotlinx.binary-compatibility-validator` plugin integration.
  - **API Validation**: `validationDisabled = true` (disabled for rapid API iteration without breaking builds on signature changes; re-enable if strict ABI checks are required).

## 4. Integration & Infrastructure Testing (Testcontainers)

- [x] **Database & Migrations (`core/database` — PostgreSQL)**
  - Isolated PostgreSQL container execution via Testcontainers (`FlywayValidationIntegrationTest.kt`).
  - **Flyway Validation Test**: Verify `flyway.migrate()` against a clean database across all migration directories (`db/migration/core/*`, `db/migration/feature/*`).
- [x] **Distributed State & Pub/Sub (`core/database`, `core/settings` — Redis)**
  - Isolated Redis container execution via Testcontainers (`RedisSettingsIntegrationTest.kt`).
- [x] **Object Storage (`core/storage` — S3 / MinIO & Local FS)**
  - Integration tests against a MinIO Testcontainer (`MinioStorageIntegrationTest.kt`).
- [x] **Event Bus Abstraction (`core/events` — Kafka & In-Memory)**
  - Integration tests against a Kafka Testcontainer (`KafkaEventBusIntegrationTest.kt`).

## 5. API, Security & Lifecycle Testing

- [x] **Ktor Dual-Connector & Routing Isolation (`core/common`, `feature/*`)**
  - End-to-end HTTP endpoint tests using Ktor `testApplication` without real port binding.
- [x] **Security & IAM E2E (`core/security`, `feature/user`)**
  - Cryptographic vectors (Argon2, AES-256-GCM, TOTP) and auth lifecycles covered by unit test suites (78 tests in `core/security`, 574 tests in `feature/user`).
- [x] **Audit, Masking & Observability (`core/audit`, `core/observability`)**
  - PII redaction and trace/metrics registration verified in unit test suites (32 tests in `core/audit`, 4 tests in `core/observability`).
- [x] **Dagger Graph Verification (`sample`)**
  - Verify Dagger KSP full component compilation in `:sample` (`AppBootstrapTest.kt`) to catch missing bindings at build time.

## 6. CI/CD Pipeline & DevOps (GitHub Actions)

- [x] **Continuous Integration Pipeline (`ci.yml`)**
  - Run `gitleaks-action` as an initial step to prevent secret leaks.
  - Fast static analysis execution (`ktlintCheck`, `detekt`, `konsist`) prior to test suites.
  - Execute unit and Testcontainers integration tests on Temurin JDK 25 with Gradle build cache enabled.
- [x] **Automated Publishing (`release.yml`)**
  - Triggered on creating a release tag on Temurin JDK 25.
  - Publish all artifacts and `:bom` to Maven Central via `com.vanniktech.maven.publish` using in-memory GPG signing and OIDC Trusted Publishing.
- [x] **Automated Changelog Generation (GitHub Native Release Notes)**
  - Use native GitHub Releases engine (`gh release create --generate-notes`).
  - Configured `.github/release.yml` to automatically categorize PRs by labels (`Features`, `Bug Fixes`, `Maintenance`, `Breaking Changes`) without third-party actions.
- [x] **Sample Application Verification & Containerization**
  - Multi-stage `Dockerfile` for `:sample` (JDK 25 build stage -> minimal JRE runtime image).
  - `docker-compose.yml` linking the host app with PostgreSQL, Redis, MinIO, Prometheus for isolated local and CI testing.

## 7. Developer Experience & Documentation

- [x] **Dokka V2 API Reference (GitHub Pages)**
  - Integrate `org.jetbrains.dokka` plugin using Dokka V2 HTML generator (`./gradlew dokkaGenerate`) across all `core/*` and `feature/*` modules.
  - Automated GitHub Action workflow step in `release.yml` to publish static API documentation straight to GitHub Pages upon release.
