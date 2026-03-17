package io.github.mudrichenkoevgeny.backend.core.observability.di

import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig
import io.github.mudrichenkoevgeny.backend.core.observability.di.module.ObservabilityConfigModule
import io.github.mudrichenkoevgeny.backend.core.observability.di.module.TelemetryModule
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import dagger.Module

/**
 * Aggregate Dagger module for the observability feature.
 *
 * Includes [ObservabilityConfigModule] (config factory and [ObservabilityConfig])
 * and [TelemetryModule] ([TelemetryProvider] binding).
 */
@Module(
    includes = [
        ObservabilityConfigModule::class,
        TelemetryModule::class
    ]
)
interface ObservabilityModules