# tools/detekt-rules

**Custom Detekt static analysis rules plugin** for enforcing backend architectural standards, Ktor conventions, and safety guidelines across `backend-platform-sdk` and consumer applications.

It provides a custom Detekt rule set (`backend-rules`) registered via [CustomRuleSetProvider].

## Included Detekt Rules

1. **`KtorConsistentRoutePermissionsRule`** ([KtorConsistentRoutePermissionsRule]):
   - **Description:** Requires Ktor routes to have consistent and well-defined permission checks.
   - **Rationale:** Prevents security misconfigurations by ensuring all endpoint definitions declare explicit access control.

2. **`KtorDelegatedRouteHandlerRule`** ([KtorDelegatedRouteHandlerRule]):
   - **Description:** Requires Ktor routes to delegate their logic to handler functions or usecases, prohibiting inline business logic inside the route block.
   - **Rationale:** Keeps routing definitions clean and ensures business logic remains testable in isolated components.

3. **`KtorRoutePathConstantRule`** ([KtorRoutePathConstantRule]):
   - **Description:** Requires Ktor route paths to be defined as constants rather than hardcoded inline strings.
   - **Rationale:** Centralizing path strings prevents typos and allows easier refactoring and route mapping.

4. **`KtorStrictRouteDefinitionRule`** ([KtorStrictRouteDefinitionRule]):
   - **Description:** Enforces strict structural definitions for Ktor routes according to backend platform standards.
   - **Rationale:** Maintains consistency across all microservice endpoints in the ecosystem.

5. **`NoDirectSystemEnvRule`** ([NoDirectSystemEnvRule]):
   - **Description:** Prohibits direct access to `System.getenv()` or `System.getProperty()`.
   - **Rationale:** Enforces the use of `ConfigFactory` or typed `Settings` classes for configuration management and dependency injection.

6. **`NoSuspendInsideDatabaseTransactionRule`** ([NoSuspendInsideDatabaseTransactionRule]):
   - **Description:** Prohibits using `suspend` functions inside JetBrains Exposed `transaction { ... }` blocks.
   - **Rationale:** Coroutines should use `newSuspendedTransaction { ... }` to prevent database connection thread blocking and deadlocks.

7. **`PreferConstructorInjectionRule`** ([PreferConstructorInjectionRule]):
   - **Description:** Prefers constructor injection over field/method injection.
   - **Rationale:** Enforces clean Dagger/JSR-330 `@Inject` dependency injection directly on constructors for immutability and testability.

8. **`RequireUtcTimeRule`** ([RequireUtcTimeRule]):
   - **Description:** Enforces the use of explicit UTC time configurations.
   - **Rationale:** Guarantees that time-sensitive business logic and database records are strictly operating in UTC, avoiding timezone inconsistencies.

## Detekt Base Config & Quality Plugin Integration

The base backend Detekt configuration is maintained in `src/main/resources/config/detekt/detekt-backend.yml`.

When using the `backend-platform-quality` convention plugin (which internally applies `shared-foundation-quality`), a multi-tier configuration is established:
1. **Tier 1 (Shared Base Rules):** `detekt-base.yml` is extracted by the foundation plugin.
2. **Tier 1.5 (Backend Overrides):** `detekt-backend.yml` is automatically extracted from this plugin's resources into `build/generated/detekt/detekt-backend.yml` and layered on top of the shared foundation config.
3. **Tier 2 (Local Overrides):** If `$rootDir/config/detekt/detekt.yml` exists in the repository, the quality plugin merges it on top of everything, enabling project-specific rule overrides or exclusions that take absolute precedence.

## Gradle Configuration & Direct Plugin Usage

Add the custom rules artifact to your project's Detekt dependencies:

```kotlin
// build.gradle.kts
dependencies {
    detektPlugins("io.github.mudrichenkoevgeny:backend-platform-sdk-detekt-rules:<version>")
    
    // Or for local repository builds:
    // detektPlugins(project(":tools:detekt-rules"))
}
```

Enable the rule set in `detekt-backend.yml` or local `config/detekt/detekt.yml`:

```yaml
backend-rules:
  active: true
  KtorConsistentRoutePermissionsRule:
    active: true
  KtorDelegatedRouteHandlerRule:
    active: true
  KtorRoutePathConstantRule:
    active: true
  KtorStrictRouteDefinitionRule:
    active: true
  NoDirectSystemEnvRule:
    active: true
  NoSuspendInsideDatabaseTransactionRule:
    active: true
  PreferConstructorInjectionRule:
    active: true
  RequireUtcTimeRule:
    active: true
```

[CustomRuleSetProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/detektrules/CustomRuleSetProvider.kt
[KtorConsistentRoutePermissionsRule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/detektrules/KtorConsistentRoutePermissionsRule.kt
[KtorDelegatedRouteHandlerRule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/detektrules/KtorDelegatedRouteHandlerRule.kt
[KtorRoutePathConstantRule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/detektrules/KtorRoutePathConstantRule.kt
[KtorStrictRouteDefinitionRule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/detektrules/KtorStrictRouteDefinitionRule.kt
[NoDirectSystemEnvRule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/detektrules/NoDirectSystemEnvRule.kt
[NoSuspendInsideDatabaseTransactionRule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/detektrules/NoSuspendInsideDatabaseTransactionRule.kt
[PreferConstructorInjectionRule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/detektrules/PreferConstructorInjectionRule.kt
[RequireUtcTimeRule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/tools/detektrules/RequireUtcTimeRule.kt
