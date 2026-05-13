# core/security

Security primitives for SDK-based applications: password hashing, MFA management, TOTP processing, and symmetric encryption. **HTTP routes and security “API” use cases** live in **`feature/securityapi`**, not in this module.

## What it provides

- **Config**: [SecurityConfig] built by [SecurityConfigFactory] from env variables.
- **Cryptography**:
    - **Symmetric Encryption**: [AesCryptor] (AES-256-GCM) for securing sensitive data (like TOTP secrets) before database persistence.
    - **TOTP Processor**: [TotpCryptoProcessor] for RFC 6238 compliant generation and verification of time-based tokens.
- **MFA & OTP Services**:
    - **MFA Service**: [MfaService] manages multistep authentication state via temporary `mfaToken` challenges.
    - **Otp Service**: [OtpService] issues and verifies one-time passwords (e.g., for email/SMS confirmation).
- **Password Management**:
    - **Hashing**: [PasswordHasher] (Argon2 implementation via Password4j).
    - **Validation**: [ValidatePasswordUseCase] enforces the active [PasswordPolicy] provided by [SecuritySettingsProvider].
- **Rate Limiting**: [RateLimiter] (Redis-backed) with policies for actions like login attempts or OTP retries.
- **Audit Integration**: [SecurityAuditErrorParser] maps security failures (weak passwords, expired MFA tokens) to standard audit reason codes.
- **DI Wiring**: [SecurityModules] aggregates all components, including MFA, OTP, and Crypto processors.
- **Utilities**:
    - **Base32**: [Base32] utility for encoding/decoding secrets (RFC 4648) used in TOTP.
    - **Error Mapping**: [PasswordPolicyValidatorResultMapper] for converting domain policy failures into localized `SecurityError` objects.

## Environment variables

The default config factory ([SecurityConfigFactoryImpl]) reads:

- `AUTH_REALM` — **required**; the authentication realm used for `otpauth://` URIs.
- `TOTP_ENCRYPTION_SECRET` — **required**; Base64-encoded 32-byte secret for AES encryption.
- `RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS` — window for sensitive **self-service** actions.
- `RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT` — window for **management** actions.
- `MFA_TOKEN_EXPIRATION_SECONDS` — lifetime of the temporary `mfaToken`.
- `PASSWORD_POLICY_*` — various constraints (length, digits, special characters, etc.).

## Usage

### TOTP Generation & Verification

```kotlin
// 1. Generate new secret for a user
val result = totpProcessor.generateNewSecret("user@example.com") 
// Returns GeneratedTotpSecret with raw secret, encrypted secret, and QR URI

// 2. Verify code from user
val isValid = totpProcessor.isCodeValid(userCode, encryptedSecretFromDb)
```

### MFA Challenge Flow

```kotlin
// Create a challenge during login
val challenge = mfaService.createChallenge(
    userId = user.id,
    userRole = user.role,
    type = MfaChallengeType.LOGIN_TOTP
)

// Later, validate and consume the token in one atomical operation
val validation = mfaService.validateChallenge(
    token = clientToken,
    type = MfaChallengeType.LOGIN_TOTP,
    userId = user.id
)
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

## Security Notes

- **Data at Rest**: Sensitive data like TOTP secrets must never be stored in plain text. [TotpCryptoProcessor] relies on [AesCryptor] to encrypt secrets before they reach the database.
- **Audit Logs**: All security failures are automatically mapped to `AuditStatus.DENIED` with specific reasons (e.g., `PASSWORD_TOO_WEAK`) via the [SecurityAuditErrorParser].
- **MFA Tokens**: `mfaToken` is a high-entropy temporary string that is invalidated immediately upon use or expiration.

---

[SecurityConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/model/SecurityConfig.kt
[SecurityConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/config/factory/SecurityConfigFactoryImpl.kt
[SecurityModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/di/SecurityModules.kt
[AesCryptor]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/aescryptor/AesCryptor.kt
[TotpCryptoProcessor]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/totpcryptoprocessor/TotpCryptoProcessor.kt
[MfaService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/service/mfa/MfaService.kt
[OtpService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/service/otp/OtpService.kt
[ValidatePasswordUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/usecase/open/passwordpolicy/ValidatePasswordUseCase.kt
[SecurityAuditErrorParser]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/audit/error/SecurityAuditErrorParser.kt
[RateLimiter]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/ratelimiter/RateLimiter.kt
[SecuritySettingsProvider]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/settings/provider/SecuritySettingsProvider.kt
[SeedSecuritySettingsUseCase]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/usecase/system/settings/SeedSecuritySettingsUseCase.kt
[PasswordHasher]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/passwordhasher/PasswordHasher.kt
[Base32]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/util/Base32.kt
[PasswordPolicyValidatorResultMapper]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/security/error/mapper/PasswordPolicyValidatorResultMapper.kt