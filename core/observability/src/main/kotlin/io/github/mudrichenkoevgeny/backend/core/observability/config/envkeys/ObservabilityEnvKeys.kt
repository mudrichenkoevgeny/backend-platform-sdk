package io.github.mudrichenkoevgeny.backend.core.observability.config.envkeys

/**
 * Environment variable names used to build ObservabilityConfig.
 * Consumed by ObservabilityConfigFactory implementations.
 */
public object ObservabilityEnvKeys {
    public const val TELEMETRY_ENDPOINT: String = "TELEMETRY_ENDPOINT"
    public const val TELEMETRY_SERVICE_NAME: String = "TELEMETRY_SERVICE_NAME"
    public const val METRIC_INTERVAL_SECONDS: String = "METRIC_INTERVAL_SECONDS"
}
