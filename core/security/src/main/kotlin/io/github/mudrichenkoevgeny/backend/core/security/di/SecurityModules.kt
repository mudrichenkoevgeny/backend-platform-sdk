package io.github.mudrichenkoevgeny.backend.core.security.di

import io.github.mudrichenkoevgeny.backend.core.security.di.module.AuthenticationPolicyCheckerModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordHasherModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordPolicyValidatorModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.RateLimiterModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecurityConfigModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecuritySettingsProviderModule
import dagger.Module

/**
 * Dagger aggregation module for the `core/security` package.
 *
 * Includes:
 * - [SecurityConfigModule] (config factory + config instance)
 * - [PasswordHasherModule] (password hashing)
 * - [PasswordPolicyValidatorModule] (password policy + validator)
 * - [AuthenticationPolicyCheckerModule] (recent authentication checks)
 * - [RateLimiterModule] (rate limiting implementation)
 * - [SecuritySettingsProviderModule] (DB-backed security settings access)
 */
@Module(
    includes = [
        SecurityConfigModule::class,
        PasswordHasherModule::class,
        PasswordPolicyValidatorModule::class,
        AuthenticationPolicyCheckerModule::class,
        RateLimiterModule::class,
        SecuritySettingsProviderModule::class
    ]
)
interface SecurityModules