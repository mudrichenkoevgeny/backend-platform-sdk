package io.github.mudrichenkoevgeny.backend.core.crosscutting.di

import io.github.mudrichenkoevgeny.backend.core.crosscutting.di.module.RateLimitEnforcerModule
import dagger.Module


@Module(
    includes = [
        RateLimitEnforcerModule::class
    ]
)
interface CrosscuttingModules