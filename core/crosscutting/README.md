# core/crosscutting

Cross-cutting infrastructure that composes multiple SDK modules into higher-level building blocks.

Currently, the module provides a rate limit enforcer that ties together `core/security` (rate limiting) and `core/audit` (audit logging).

## What it provides

- **RateLimitEnforcer**: [RateLimitEnforcer] checks rate limits via `core/security` [RateLimiter] and returns:
  - `AppResult.Success(Unit)` when the action is allowed
  - `AppResult.Error(appError)` when the action is blocked or when a dependency fails
- **Audit on denial**: [RateLimitEnforcerImpl] writes an [AuditEvent] with `AuditStatus.DENIED` when the action is rate-limited, enriching metadata from [RequestContext] using [RateLimitAuditMetadata].
- **DI wiring**: [CrosscuttingModules] aggregates [RateLimitEnforcerModule] bindings.

## Usage

- Add dependency on `core:crosscutting`. Depends on `core:common`, `core:security`, and `core:audit`.
- Install [CrosscuttingModules] in your Dagger component.
- Inject [RateLimitEnforcer] in routes/use cases where you need a consistent deny + audit behavior.

### Enforce a rate limit

```kotlin
suspend fun enforceExample(enforcer: RateLimitEnforcer, requestContext: RequestContext) {
    val result = enforcer.enforce(
        requestContext = requestContext,
        rateLimitAction = io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction.LOGIN_ATTEMPT,
        rateLimitIdentifier = "ip:127.0.0.1",
        auditAction = "login",
        auditResource = "session",
        auditResourceId = null
    )

    // result is AppResult.Success(Unit) or AppResult.Error(CommonError.TooManyRequests / other AppError)
}
```

## Notes

- **Rate limit policies** live in `core/security` as [RateLimitAction]. The enforcer does not define policies; it only orchestrates enforcement and audit logging.
- **Audit metadata** written on denial includes IP address, device id, client type, user agent, and a denial reason (`rate_limit`).

[CrosscuttingModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/crosscutting/di/CrosscuttingModules.kt
[RateLimitEnforcerModule]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/crosscutting/di/module/RateLimitEnforcerModule.kt

[RateLimitEnforcer]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/crosscutting/ratelimiter/RateLimitEnforcer.kt
[RateLimitEnforcerImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/crosscutting/ratelimiter/RateLimitEnforcerImpl.kt
[RateLimitAuditMetadata]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/crosscutting/ratelimiter/RateLimitAuditMetadata.kt

[RateLimiter]: ../security/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/RateLimiter.kt
[RateLimitAction]: ../security/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/model/RateLimitAction.kt

[AuditEvent]: ../audit/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/audit/model/AuditEvent.kt
[RequestContext]: ../common/src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/common/network/request/model/RequestContext.kt
