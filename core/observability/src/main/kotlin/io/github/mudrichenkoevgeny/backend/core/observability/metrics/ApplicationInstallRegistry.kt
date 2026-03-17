package io.github.mudrichenkoevgeny.backend.core.observability.metrics

import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics

/**
 * Installs the Micrometer metrics plugin on this [Application] using the [TelemetryProvider]'s Prometheus registry.
 *
 * Use this when you need only the registry binding for Ktor (e.g. default request metrics) without
 * the custom tracing and HTTP metrics from `configureObservability` in the application package.
 *
 * @param telemetryProvider supplies the Prometheus meter registry for [MicrometerMetrics].
 */
fun Application.installRegistry(
    telemetryProvider: TelemetryProvider
) {
    val prometheusMeterRegistry = telemetryProvider.prometheusMeterRegistry

    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
    }
}