# core/security

Security primitives for SDK-based applications: password hashing, password policy validation, rate limiting for security-sensitive actions, and persisted security settings (via `core/settings`). **HTTP routes and security “API” use cases** live in **`feature/security-api`**, not in this module.

## What it provides

- **Config**: [SecurityConfig] built by [SecurityConfigFactory] from env via [SecurityEnvKeys].
- **Password hashing**: [PasswordHasher] (Password4j Argon2 implementation: [PasswordHasherImpl]).
- **Password policy (in-process)**: default `PasswordPolicy` from [SecurityConfig] and foundation `PasswordPolicyValidator` ([PasswordPolicyValidatorModule]).
- **Security settings (persistence)**:
  - [SecuritySettingsProvider] backed by [SystemSettingsService] ([SecuritySettingsProviderImpl]) — password policy JSON and recent-authentication window.
  - [SeedSecuritySettingsUseCase] seeds defaults on bootstrap.
- **Rate limiting**: [RateLimiter] (Redis-backed implementation: [RateLimiterImpl]) with predefined [RateLimitAction] policies and [RateLimitResult].
- **Authentication freshness**: [AuthenticationPolicyChecker] (implementation: [AuthenticationPolicyCheckerImpl]) to check whether the user recently passed a confirmation step.
- **WebSockets**: [SecurityWebSocketMessageHandler] contributed via [SecurityWebSocketModule].
- **DI wiring**: [SecurityModules] aggregates config, hashing, policy validator, rate limiting, settings provider, auth policy checker, and WebSocket contributions.

## Environment variables

The default config factory ([SecurityConfigFactoryImpl]) reads:

- `AUTHENTICATION_CONFIRMATION_VALIDITY_MINUTES` — window (minutes) during which a re-authentication is considered valid.
- `PASSWORD_POLICY_MIN_LENGTH` — minimum password length (optional; fallback: `PasswordPolicy.DEFAULT_MIN_LENGTH`).
- `PASSWORD_POLICY_REQUIRE_LETTER` — `"true"`/`"false"` (optional; fallback: `true`).
- `PASSWORD_POLICY_REQUIRE_UPPER_CASE` — `"true"`/`"false"` (optional; fallback: `false`).
- `PASSWORD_POLICY_REQUIRE_LOWER_CASE` — `"true"`/`"false"` (optional; fallback: `false`).
- `PASSWORD_POLICY_REQUIRE_DIGIT` — `"true"`/`"false"` (optional; fallback: `false`).
- `PASSWORD_POLICY_REQUIRE_SPECIAL_CHAR` — `"true"`/`"false"` (optional; fallback: `false`).
- `PASSWORD_POLICY_COMMON_PASSWORDS` — comma-separated list of common passwords to reject (optional; fallback: `PasswordPolicy.DEFAULT_COMMON_PASSWORDS`).

See: [SecurityEnvKeys].

## Usage

- Add dependency on `core:security`. Depends on `core:common`, `core:database`, `core:settings`, and `core:audit`.
- Install [SecurityModules] in your Dagger component.
- Seed defaults on bootstrap (optional but recommended): call [SeedSecuritySettingsUseCase].
- For **HTTP** endpoints (read/update security settings, validate password, etc.), add **`feature/security-api`**, install its Dagger classpath bindings as needed, and register `SecurityRouter` in Ktor (see that module’s README).

### Rate limit an action

```kotlin
suspend fun rateLimitExample(rateLimiter: RateLimiter) {
    val result = rateLimiter.isRateLimited(
        action = RateLimitAction.LOGIN_ATTEMPT,
        identifier = "ip:127.0.0.1"
    )
    // result is AppResult.Success(RateLimitResult.Allowed/Exceeded) or AppResult.Error(appError)
}
```

## Notes

- **Effective password policy**: [SecuritySettingsProvider] uses stored values when present; otherwise it falls back to [SecurityConfig].
- **Rate limiting storage**: [RateLimiterImpl] uses Redis counters with expiration; when the limit is exceeded it returns a `Too Many Requests` error with a retry-after value.

[SecurityConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/model/SecurityConfig.kt
[SecurityConfigFactory]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/factory/SecurityConfigFactory.kt
[SecurityConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/factory/SecurityConfigFactoryImpl.kt
[SecurityEnvKeys]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/envkeys/SecurityEnvKeys.kt

[PasswordHasher]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/passwordhasher/PasswordHasher.kt
[PasswordHasherImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/passwordhasher/PasswordHasherImpl.kt

[PasswordPolicyValidatorModule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/di/module/PasswordPolicyValidatorModule.kt

[SecurityError.PasswordTooWeak]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/error/model/SecurityError.kt

[SecuritySettingsProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/provider/SecuritySettingsProvider.kt
[SecuritySettingsProviderImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/provider/SecuritySettingsProviderImpl.kt
[SeedSecuritySettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/usecase/system/settings/SeedSecuritySettingsUseCase.kt
[SystemSettingsService]: ../settings/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/settings/service/SystemSettingsService.kt

[RateLimiter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/RateLimiter.kt
[RateLimiterImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/RateLimiterImpl.kt
[RateLimitAction]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/model/RateLimitAction.kt
[RateLimitResult]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/RateLimitResult.kt

[AuthenticationPolicyChecker]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/authenticationpolicychecker/AuthenticationPolicyChecker.kt
[AuthenticationPolicyCheckerImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/authenticationpolicychecker/AuthenticationPolicyCheckerImpl.kt

[SecurityWebSocketMessageHandler]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/network/websockets/messagehandler/SecurityWebSocketMessageHandler.kt
[SecurityWebSocketModule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/di/module/SecurityWebSocketModule.kt

[SecurityModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/di/SecurityModules.kt
