package io.github.mudrichenkoevgeny.backend.core.security.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.security.di.module.AesCryptorModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordHasherModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordPolicyValidatorModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.RateLimiterModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecurityConfigModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecuritySettingsProviderModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecurityAuditErrorParserModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecurityServicesModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.TotpCryptoProcessorModule

/**
 * Dagger aggregation module for the `core/security` package.
 *
 * Includes:
 * - [SecurityConfigModule]: Configuration factory and instance for security settings.
 * - [PasswordHasherModule]: Password hashing and verification via Argon2.
 * - [PasswordPolicyValidatorModule]: Password strength rules and validation logic.
 * - [RateLimiterModule]: Brute-force protection and request rate limiting.
 * - [SecuritySettingsProviderModule]: DB-backed access to security configurations.
 * - [SecurityAuditErrorParserModule]: Mapping and parsing of security-related audit logs.
 * - [SecurityServicesModule]: High-level MFA and OTP management services.
 * - [AesCryptorModule]: Symmetric AES-256-GCM encryption for sensitive data.
 * - [TotpCryptoProcessorModule]: Specialized cryptographic processing for TOTP secrets.
 */
@Module(
    includes = [
        SecurityConfigModule::class,
        PasswordHasherModule::class,
        PasswordPolicyValidatorModule::class,
        RateLimiterModule::class,
        SecuritySettingsProviderModule::class,
        SecurityAuditErrorParserModule::class,
        SecurityServicesModule::class,
        AesCryptorModule::class,
        TotpCryptoProcessorModule::class
    ]
)
interface SecurityModules