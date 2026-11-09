package io.github.mudrichenkoevgeny.backend.core.observability.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig
import io.github.mudrichenkoevgeny.backend.core.observability.di.module.ObservabilityConfigModule
import io.github.mudrichenkoevgeny.backend.core.observability.di.module.TelemetryModule
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider

/**
 * Aggregate Dagger module for the observability feature.
 *
 * Includes [ObservabilityConfigModule] (config factory and [ObservabilityConfig])
 * and [TelemetryModule] ([TelemetryProvider] binding).
 */
@Module(
    includes = [ObservabilityConfigModule::class, TelemetryModule::class]
)
public interface ObservabilityModules
