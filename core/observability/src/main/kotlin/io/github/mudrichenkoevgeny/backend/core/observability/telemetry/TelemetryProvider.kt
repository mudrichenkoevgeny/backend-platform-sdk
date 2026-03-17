package io.github.mudrichenkoevgeny.backend.core.observability.telemetry

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer

/**
 * Provides OpenTelemetry and Micrometer resources for tracing and metrics.
 *
 * Used by application bootstrap to install MicrometerMetrics, register HTTP metrics
 * and tracing interceptors, and expose a Prometheus scrape endpoint.
 */
interface TelemetryProvider {

    val openTelemetry: OpenTelemetry
    val tracer: Tracer
    val meter: Meter
    val prometheusMeterRegistry: PrometheusMeterRegistry

    /**
     * Performs a minimal tracing operation to ensure the tracer is initialized (e.g. after SDK startup).
     */
    fun warmup()
}