package com.mudrichenkoevgeny.backend.core.security.di

import com.mudrichenkoevgeny.backend.core.security.di.module.AuthenticationPolicyCheckerModule
import com.mudrichenkoevgeny.backend.core.security.di.module.PasswordHasherModule
import com.mudrichenkoevgeny.backend.core.security.di.module.PasswordPolicyCheckerModule
import com.mudrichenkoevgeny.backend.core.security.di.module.RateLimierModule
import com.mudrichenkoevgeny.backend.core.security.di.module.SecurityConfigModule
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