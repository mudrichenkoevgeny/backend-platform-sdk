package io.github.mudrichenkoevgeny.backend.core.observability.telemetry

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer

interface TelemetryProvider {
    val openTelemetry: OpenTelemetry
    val tracer: Tracer
    val meter: Meter
    val prometheusMeterRegistry: PrometheusMeterRegistry

    fun warmup()
}