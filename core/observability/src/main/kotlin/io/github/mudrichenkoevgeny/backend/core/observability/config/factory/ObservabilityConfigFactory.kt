package io.github.mudrichenkoevgeny.backend.core.observability.config.factory

import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig

/**
 * Factory for building [ObservabilityConfig] from environment or other configuration source.
 */
interface ObservabilityConfigFactory {

    /**
     * Creates a new observability configuration instance.
     *
     * @return configuration with telemetry service name, endpoint and metric export interval.
     */
    fun create(): ObservabilityConfig
}