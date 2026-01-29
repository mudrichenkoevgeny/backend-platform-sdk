package io.github.mudrichenkoevgeny.backend.core.security.di

import io.github.mudrichenkoevgeny.backend.core.security.di.module.AuthenticationPolicyCheckerModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordHasherModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.PasswordPolicyCheckerModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.RateLimierModule
import io.github.mudrichenkoevgeny.backend.core.security.di.module.SecurityConfigModule
import dagger.Module

@Module(
    includes = [
        SecurityConfigModule::class,
        PasswordHasherModule::class,
        PasswordPolicyCheckerModule::class,
        AuthenticationPolicyCheckerModule::class,
        RateLimierModule::class
    ]
)
interface SecurityModules