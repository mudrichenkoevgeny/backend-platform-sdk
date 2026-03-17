package io.github.mudrichenkoevgeny.backend.core.security.di

import io.github.mudrichenkoevgeny.backend.core.security.di.module.AuthenticationPolicyCheckerModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordHasherModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordPolicyValidatorModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.RateLimierModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecurityConfigModule
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecuritySettingsProviderModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecurityWebSocketModule

/**
 * Dagger aggregation module for the `core/security` package.
 *
 * Includes:
 * - [SecurityConfigModule] (config factory + config instance)
 * - [PasswordHasherModule] (password hashing)
 * - [PasswordPolicyValidatorModule] (password policy + validator)
 * - [AuthenticationPolicyCheckerModule] (recent authentication checks)
 * - [RateLimierModule] (rate limiting implementation)
 * - [SecuritySettingsProviderModule] (DB-backed security settings access)
 * - [SecurityWebSocketModule] (WebSocket handlers contributed into the global handler set)
 */
@Module(
    includes = [
        SecurityConfigModule::class,
        PasswordHasherModule::class,
        PasswordPolicyValidatorModule::class,
        AuthenticationPolicyCheckerModule::class,
        RateLimierModule::class,
        SecuritySettingsProviderModule::class,
        SecurityWebSocketModule::class
    ]
)
interface SecurityModules