package io.github.mudrichenkoevgeny.backend.core.observability.config.model

data class ObservabilityConfig(
    val telemetryServiceName: String,
    val telemetryEndpoint: String,
    val metricIntervalSeconds: Long
)