package io.github.mudrichenkoevgeny.backend.core.observability.config.model

/**
 * Configuration for OpenTelemetry and metrics export.
 *
 * @param telemetryServiceName service name used for tracer and meter (e.g. in spans and metrics).
 * @param telemetryEndpoint OTLP or other telemetry collector endpoint URL.
 * @param metricIntervalSeconds interval in seconds for exporting metrics to the collector.
 */
data class ObservabilityConfig(
    val telemetryServiceName: String,
    val telemetryEndpoint: String,
    val metricIntervalSeconds: Long
)