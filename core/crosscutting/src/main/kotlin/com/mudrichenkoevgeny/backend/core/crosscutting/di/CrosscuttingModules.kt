package com.mudrichenkoevgeny.backend.core.crosscutting.di

import com.mudrichenkoevgeny.backend.core.crosscutting.di.module.RateLimitEnforcerModule
import dagger.Module


@Module(
    includes = [
        RateLimitEnforcerModule::class
    ]
)
interface CrosscuttingModules