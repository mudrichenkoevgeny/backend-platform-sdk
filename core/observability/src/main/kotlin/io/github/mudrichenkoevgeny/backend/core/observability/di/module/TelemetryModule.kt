package io.github.mudrichenkoevgeny.backend.core.observability.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProviderImpl
import javax.inject.Singleton

/**
 * Dagger module that provides [TelemetryProvider].
 *
 * Binds [TelemetryProvider] to [TelemetryProviderImpl]; requires [ObservabilityConfig].
 */
@Module
public class TelemetryModule {
    @Provides
    @Singleton
    public fun provideTelemetryProvider(observabilityConfig: ObservabilityConfig): TelemetryProvider =
        TelemetryProviderImpl(
            observabilityConfig = observabilityConfig
        )
}
