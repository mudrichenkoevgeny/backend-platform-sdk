package io.github.mudrichenkoevgeny.backend.core.observability.config.envkeys

/**
 * Environment variable names used to build ObservabilityConfig.
 * Consumed by ObservabilityConfigFactory implementations.
 */
object ObservabilityEnvKeys {
    const val TELEMETRY_ENDPOINT = "TELEMETRY_ENDPOINT"
    const val TELEMETRY_SERVICE_NAME = "TELEMETRY_SERVICE_NAME"
    const val METRIC_INTERVAL_SECONDS = "METRIC_INTERVAL_SECONDS"
}