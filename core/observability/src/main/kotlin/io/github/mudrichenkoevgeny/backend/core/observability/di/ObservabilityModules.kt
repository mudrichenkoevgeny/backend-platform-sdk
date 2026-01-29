package io.github.mudrichenkoevgeny.backend.core.observability.di

import io.github.mudrichenkoevgeny.backend.core.observability.di.module.ObservabilityConfigModule
import io.github.mudrichenkoevgeny.backend.core.observability.di.module.TelemetryModule
import dagger.Module

@Module(
    includes = [
        ObservabilityConfigModule::class,
        TelemetryModule::class
    ]
)
interface ObservabilityModules