# tools/gradle-plugin

**Gradle Convention Plugins for backend-platform-sdk** providing build tasks, Ktor-specific compiler configurations, AI agent rules synchronization, and quality check aggregations across consumer backend microservices.

These plugins natively depend on and extend the foundation plugins from `shared-foundation-gradle-plugin`.

Published Maven artifact: **`backend-platform-sdk-gradle-plugin`**.

## Included Convention Plugins

1. **`io.github.mudrichenkoevgeny.backend-platform-agent-sync`** (`BackendAgentSyncPlugin`):
   - **Description:** Applies `shared-foundation-agent-sync` and registers the backend-specific AI agent rules layer.
   - **Layer Sync:** Extracts backend AI agent rules (`.agent/global/backend/`) from plugin resources directly into downstream `.agent/global/backend/` folders to ensure AI coding agents comply with backend Ktor architectural standards.

2. **`io.github.mudrichenkoevgeny.backend-platform-quality`** (`BackendQualityPlugin`):
   - **Description:** Applies `shared-foundation-quality` and layers backend-specific static analysis overrides for Ktlint and Detekt.
   - **Multi-Layer `.editorconfig`:** Registers an additional backend `.editorconfig` layer containing Ktor/JVM specific formatting rules that merges seamlessly with the foundation config above the `# --- LOCAL OVERRIDES ---` marker.
   - **Multi-Tier Detekt Configuration:** Extracts `detekt-backend.yml` (Tier 1.5) from resources and adds it to the composite Detekt configuration, inheriting `detekt-base.yml` (Tier 1) from `shared-foundation`, while still prioritizing project-local `$rootDir/config/detekt/detekt.yml` overrides (Tier 2).
   - **Rule Set Wiring:** Automatically wires the `:tools:detekt-rules` (`backend-platform-sdk-detekt-rules`) dependencies to the consumer projects.

3. **`io.github.mudrichenkoevgeny.backend-platform-kotlin`** (`BackendKotlinConventionPlugin`):
   - **Description:** Applies `shared-foundation-kotlin` to set up standard compiler rules (`explicitApi()`, `allWarningsAsErrors`), and configure standard JVM and Test targets suited for backend infrastructure.

4. **`io.github.mudrichenkoevgeny.backend-platform-publish`** (`BackendPublishConventionPlugin`):
   - **Description:** Applies `shared-foundation-publish` to configure Vanniktech Maven Publishing with backend-specific POM attributes and Dokka documentation output.

## Usage in Consumer Projects

Apply any of the convention plugins in your consumer project's `build.gradle.kts`:

```kotlin
// build.gradle.kts
plugins {
    id("io.github.mudrichenkoevgeny.backend-platform-agent-sync") version "<version>"
    id("io.github.mudrichenkoevgeny.backend-platform-quality") version "<version>"
    id("io.github.mudrichenkoevgeny.backend-platform-kotlin") version "<version>"
    id("io.github.mudrichenkoevgeny.backend-platform-publish") version "<version>"
}
```

Or configure via Version Catalog (`gradle/libs.versions.toml`).
