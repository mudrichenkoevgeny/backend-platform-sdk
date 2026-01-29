package io.github.mudrichenkoevgeny.backend.core.observability.metrics

import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics

fun Application.installRegistry(
    telemetryProvider: TelemetryProvider
) {
    val prometheusMeterRegistry = telemetryProvider.prometheusMeterRegistry

    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
    }
}