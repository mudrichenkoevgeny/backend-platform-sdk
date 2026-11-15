# tools/project-generator

Internal Kotlin JVM module and CLI engine for scaffolding clean, minimal, production-ready downstream Ktor microservice repositories (`<name>-backend`) on disk in a single command.

Triggered via the root Gradle tasks: **`./gradlew initProject`** or **`./gradlew scaffoldProject`**.

---

## Generator Architecture & Core Engine

The project skeleton generator is implemented as an internal (non-published) JVM module under `tools/project-generator/`:

- **[ProjectGenerator]**: Main generator engine. Copies binary infrastructure files (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `gradle-daemon-jvm.properties`, `.editorconfig`, `.agent/global/`), processes physical resource templates from `src/main/resources/templates/`, sets executable permissions (`chmod +x gradlew`), enforces overwrite protection (`CONFLICTING_ENTRIES`), and initializes a Git repository.
- **[ProjectGeneratorConfig]**: Parameter resolution data class. Reads system properties passed by Gradle, validates required version inputs, and computes `groupId`, `packageName`, `packageDirPath`, and `targetDir` with safe defaults and segment-by-segment package sanitization (`com.<name>.backend`).
- **[TemplateReplacer]**: String substitution engine. Replaces generic placeholder tokens (`{{PROJECT_NAME}}`, `{{PROJECT_TITLE}}`, `{{PROJECT_REPO_NAME}}`, `{{GROUP_ID}}`, `{{PACKAGE_NAME}}`, `{{PACKAGE_DIR_PATH}}`, `{{BACKEND_SDK_VERSION}}`, `{{SF_VERSION}}`, `{{TOOLCHAIN_JVM}}`, `{{KOTLIN_VERSION}}`, `{{KSP_VERSION}}`, `{{DETEKT_VERSION}}`, `{{KTLINT_VERSION}}`, `{{COROUTINES_VERSION}}`, `{{SERIALIZATION_VERSION}}`, `{{KTOR_VERSION}}`, `{{DAGGER_VERSION}}`, `{{JAVAX_INJECT_VERSION}}`, `{{EXPOSED_VERSION}}`, `{{POSTGRESQL_VERSION}}`, `{{FLYWAY_VERSION}}`, `{{SLF4J_VERSION}}`, `{{JUNIT_VERSION}}`, `{{MOCKK_VERSION}}`, `{{TESTCONTAINERS_VERSION}}`, `{{MAVEN_LOCAL_REPOS}}`) with dynamic values injected directly from `backend-platform-sdk`'s catalog (`libs.versions.toml`).
- **[ProjectGeneratorVerifier]**: In-process E2E verification engine. Scaffolds a test project in `build/tmp/scaffold-test/scaffold-test-backend` and executes `./gradlew check jar` inside it to ensure zero template rot.

---

## CLI Task Usage & Parameters

Scaffold a new downstream microservice repository:

```bash
./gradlew initProject -Pname=sample -PlinkLocal=true
```

### Command Parameter Matrix

| Parameter | Default Value | Description |
| :--- | :--- | :--- |
| **`-Pname`** | *(Required)* | Short name of the project (e.g. `sample`, `order-service`). Hyphens are sanitized for Kotlin package paths. |
| **`-PgroupId` / `-Pgroup`** | `com.<name>` | Maven group ID (e.g. `com.sample`). If it does not end with `.backend`, `.backend` is automatically appended to the base package name (`com.sample.backend`). |
| **`-PtargetDir`** | `../<name>-backend` | Absolute or relative target path for the scaffolded repository. |
| **`-PlinkLocal`** | `false` | When `true`, configures `mavenLocal()` first in the generated `settings.gradle.kts` for local development. |
| **`-PskipGit`** | `false` | When `true`, skips running `git init` in the created directory. |

---

## Resource Templates (`src/main/resources/templates/`)

All text-based files for scaffolded repositories are stored as static `.template` resource files:

- **Root Configurations:** `build.gradle.kts.template`, `settings.gradle.kts.template`, `gradle.properties.template`, `gradle/libs.versions.toml.template`, `.gitignore.template`, `Dockerfile.template`, `AGENTS.md.template`, `README.md.template`, `CONTRIBUTING.md.template`, `.agent/local/project-overview.md.template`, `.github/workflows/build.yml.template`.
- **Deployment Containers:** `deploy/docker-compose.base.yml.template`, `deploy/docker-compose.dev.yml.template`.
- **Application Source Code:**
  - `src/main/kotlin/{{PACKAGE_DIR_PATH}}/Main.kt.template`
  - `src/main/kotlin/{{PACKAGE_DIR_PATH}}/Module.kt.template`
  - `src/main/kotlin/{{PACKAGE_DIR_PATH}}/{{PROJECT_TITLE}}AppInfo.kt.template`
  - `src/main/kotlin/{{PACKAGE_DIR_PATH}}/appbootstrap/AppBootstrap.kt.template`
  - `src/main/kotlin/{{PACKAGE_DIR_PATH}}/di/AppComponent.kt.template`, `AppModule.kt.template`, `AuditParsersModule.kt.template`
  - `src/main/kotlin/{{PACKAGE_DIR_PATH}}/lifecycle/AppShutdownHook.kt.template`, `AppShutdownHookImpl.kt.template`, `ShutdownHookRegistrar.kt.template`, `RuntimeShutdownHookRegistrar.kt.template`
- **Application Tests:**
  - `src/test/kotlin/{{PACKAGE_DIR_PATH}}/{{PROJECT_TITLE}}AppInfoTest.kt.template`
  - `src/test/kotlin/{{PACKAGE_DIR_PATH}}/appbootstrap/AppBootstrapTest.kt.template`
  - `src/test/kotlin/{{PACKAGE_DIR_PATH}}/integration/FlywayValidationIntegrationTest.kt.template`
  - `src/test/kotlin/{{PACKAGE_DIR_PATH}}/lifecycle/AppShutdownHookImplTest.kt.template`

---

## E2E Automated Verification

The generator is continuously verified against template rot and version drift:

```bash
./gradlew verifyProjectGenerator
```

This task compiles `backend-platform-sdk`, publishes artifacts to `mavenLocal()`, scaffolds a test project in `build/tmp/scaffold-test/scaffold-test-backend`, and verifies that `./gradlew check jar` passes cleanly out of the box inside the scaffolded project.

[ProjectGenerator]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/projectgenerator/ProjectGenerator.kt
[ProjectGeneratorConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/projectgenerator/ProjectGeneratorConfig.kt
[TemplateReplacer]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/projectgenerator/TemplateReplacer.kt
[ProjectGeneratorVerifier]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/projectgenerator/ProjectGeneratorVerifier.kt
