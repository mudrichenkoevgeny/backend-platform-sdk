package io.github.mudrichenkoevgeny.backend.core.security.di

import io.github.mudrichenkoevgeny.backend.core.security.di.module.AuthenticationPolicyCheckerModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordHasherModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordPolicyValidatorModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.RateLimierModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecurityConfigModule
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecuritySettingsProviderModule

@Module(
    includes = [
        SecurityConfigModule::class,
        PasswordHasherModule::class,
        PasswordPolicyValidatorModule::class,
        AuthenticationPolicyCheckerModule::class,
        RateLimierModule::class,
        SecuritySettingsProviderModule::class
    ]
)
interface SecurityModules