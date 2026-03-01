# backend-platform-sdk
A modular foundational SDK for building scalable Kotlin/Ktor microservices. Provides pre-configured core infrastructure for observability, database management, security, and shared business features.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mudrichenkoevgeny/backend-platform-sdk-bom)](https://central.sonatype.com/artifact/io.github.mudrichenkoevgeny/backend-platform-sdk-bom)
## Installation
Add the library to your projects using one of the following methods:
### Option 1: Version Catalog
_1. In your gradle/libs.versions.toml file:_
```
[versions]
backend-sdk = "0.0.15"

[libraries]
backend-sdk-bom = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-bom", version.ref = "backend-sdk" }
backend-sdk-core-common = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-common" }
backend-sdk-core-audit = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-audit" }
backend-sdk-core-crosscutting = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-crosscutting" }
backend-sdk-core-database = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-database" }
backend-sdk-core-events = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-events" }
backend-sdk-core-observability = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-observability" }
backend-sdk-core-settings = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-settings" }
backend-sdk-core-security = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-security" }
backend-sdk-core-storage = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-storage" }
backend-sdk-feature-user = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-feature-user" }

# Or if you don't want to use BOM:
backend-sdk-core-common = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-common", version.ref = "backend-sdk" }
backend-sdk-core-audit = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-audit", version.ref = "backend-sdk" }
backend-sdk-core-crosscutting = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-crosscutting", version.ref = "backend-sdk" }
backend-sdk-core-database = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-database", version.ref = "backend-sdk" }
backend-sdk-core-events = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-events", version.ref = "backend-sdk" }
backend-sdk-core-observability = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-observability", version.ref = "backend-sdk" }
backend-sdk-core-settings = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-settings", version.ref = "backend-sdk" }
backend-sdk-core-security = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-security", version.ref = "backend-sdk" }
backend-sdk-core-storage = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-core-storage", version.ref = "backend-sdk" }
backend-sdk-feature-user = { group = "io.github.mudrichenkoevgeny", name = "backend-platform-sdk-feature-user", version.ref = "backend-sdk" }
```
In your build.gradle.kts:
```
dependencies {
    implementation(platform(libs.backend.sdk.bom))
    implementation(libs.backend.sdk.core.common)
    implementation(libs.backend.sdk.core.audit)
    implementation(libs.backend.sdk.core.crosscutting)
    implementation(libs.backend.sdk.core.database)
    implementation(libs.backend.sdk.core.events)
    implementation(libs.backend.sdk.core.observability)
    implementation(libs.backend.sdk.core.settings)
    implementation(libs.backend.sdk.core.security)
    implementation(libs.backend.sdk.core.storage)
    implementation(libs.backend.sdk.feature.user)
    
    // Or if you don't want to use BOM:
    implementation(libs.backend.sdk.core.common)
    implementation(libs.backend.sdk.core.audit)
    implementation(libs.backend.sdk.core.crosscutting)
    implementation(libs.backend.sdk.core.database)
    implementation(libs.backend.sdk.core.events)
    implementation(libs.backend.sdk.core.observability)
    implementation(libs.backend.sdk.core.settings)
    implementation(libs.backend.sdk.core.security)
    implementation(libs.backend.sdk.core.storage)
    implementation(libs.backend.sdk.feature.user)
}
```
### Option 2: Direct Dependency
```
implementation(platform("io.github.mudrichenkoevgeny:backend-platform-sdk-bom:0.0.15"))
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-common")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-audit")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-crosscutting")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-database")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-events")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-observability")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-settings")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-security")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-storage")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-feature-user")

// Or if you don't want to use BOM:
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-common:0.0.15")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-audit:0.0.15")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-crosscutting:0.0.15")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-database:0.0.15")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-events:0.0.15")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-observability:0.0.15")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-settings:0.0.15")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-security:0.0.15")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-core-storage:0.0.15")
implementation("io.github.mudrichenkoevgeny:backend-platform-sdk-feature-user:0.0.15")
```

# Integration Steps
To initialize the SDK, you must provide the application metadata (name and version). This is done by implementing the AppInfo interface and binding it in your dependency injection graph.

### 1. Implement AppInfo: Create a class that implements AppInfo.

```kotlin
@Singleton
class BuildConfigAppInfo @Inject constructor() : AppInfo {
    override val version: String = BuildConfig.VERSION
    override val appName: String = BuildConfig.APP_NAME
}
```

### 2. Provide the Binding. Register the implementation in your Dagger module

```kotlin
@Module
interface AppModule {
    @Binds
    @Singleton
    fun bindAppInfo(impl: BuildConfigAppInfo): AppInfo
}
```