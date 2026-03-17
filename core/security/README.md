# core/security

Security primitives for SDK-based applications: password hashing, password policy validation, rate limiting for security-sensitive actions, and security settings exposed via HTTP routes.

## What it provides

- **Config**: [SecurityConfig] built by [SecurityConfigFactory] from env via [SecurityEnvKeys].
- **Password hashing**: [PasswordHasher] (Password4j Argon2 implementation: [PasswordHasherImpl]).
- **Password validation**: [ValidatePasswordUseCase] validates a password against the effective policy and returns a structured [SecurityError.PasswordTooWeak] on failure.
- **Security settings**:
  - [SecuritySettingsProvider] backed by the settings subsystem ([SecuritySettingsProviderImpl]).
  - [SeedSecuritySettingsUseCase] seeds defaults on bootstrap.
  - [GetSecuritySettingsUseCase] returns effective settings for API/UI.
  - [SecuritySettingsRouter] exposes HTTP endpoints defined in `shared-foundation` routes.
- **Rate limiting**: [RateLimiter] (Redis-backed implementation: [RateLimiterImpl]) with predefined [RateLimitAction] policies and [RateLimitResult].
- **Authentication freshness**: [AuthenticationPolicyChecker] (implementation: [AuthenticationPolicyCheckerImpl]) to check whether the user recently passed a confirmation step.
- **DI wiring**: [SecurityModules] aggregates config, services, settings provider, routes and WebSocket handler contribution.

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

- Add dependency on `core:security`. Depends on `core:common`, `core:database`, and `core:settings`.
- Install [SecurityModules] in your Dagger component.
- Seed defaults on bootstrap (optional but recommended): call [SeedSecuritySettingsUseCase].
- Register [SecurityFeatureRouter] in your Ktor `routing { }` block to expose routes.

### Validate a password

```kotlin
fun validateExample(validatePassword: ValidatePasswordUseCase) {
    val result = validatePassword("Sup3r_Str0ng_Pass!")
    // result is AppResult.Success(Unit) or AppResult.Error(SecurityError.PasswordTooWeak)
}
```

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

### Register routes

```kotlin
fun installRoutesExample(route: io.ktor.server.routing.Route, router: SecurityFeatureRouter) {
    router.register(route)
}
```

## Notes

- **Effective password policy**: the module uses the stored policy from the settings subsystem when present; otherwise it falls back to the default policy from [SecurityConfig].
- **Rate limiting storage**: [RateLimiterImpl] uses Redis counters with expiration; when the limit is exceeded it returns a `Too Many Requests` error with a retry-after value.

[SecurityConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/model/SecurityConfig.kt
[SecurityConfigFactory]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/factory/SecurityConfigFactory.kt
[SecurityConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/factory/SecurityConfigFactoryImpl.kt
[SecurityEnvKeys]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/envkeys/SecurityEnvKeys.kt

[PasswordHasher]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/passwordhasher/PasswordHasher.kt
[PasswordHasherImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/passwordhasher/PasswordHasherImpl.kt

[SecurityError.PasswordTooWeak]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/error/model/SecurityError.kt
[ValidatePasswordUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/usecase/ValidatePasswordUseCase.kt

[SecuritySettingsProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/provider/SecuritySettingsProvider.kt
[SecuritySettingsProviderImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/provider/SecuritySettingsProviderImpl.kt
[SeedSecuritySettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/usecase/SeedSecuritySettingsUseCase.kt
[GetSecuritySettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/usecase/GetSecuritySettingsUseCase.kt
[SecuritySettingsRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/route/SecuritySettingsRouter.kt
[SecurityFeatureRouter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/route/SecurityFeatureRouter.kt

[RateLimiter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/RateLimiter.kt
[RateLimiterImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/RateLimiterImpl.kt
[RateLimitAction]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/model/RateLimitAction.kt
[RateLimitResult]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/RateLimitResult.kt

[AuthenticationPolicyChecker]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/authenticationpolicychecker/AuthenticationPolicyChecker.kt
[AuthenticationPolicyCheckerImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/authenticationpolicychecker/AuthenticationPolicyCheckerImpl.kt

[SecurityModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/di/SecurityModules.kt

