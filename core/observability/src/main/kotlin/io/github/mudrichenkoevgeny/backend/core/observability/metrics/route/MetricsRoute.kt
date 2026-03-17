package io.github.mudrichenkoevgeny.backend.core.observability.metrics.route

import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route

/**
 * Registers the Prometheus metrics scrape endpoint on this [Route].
 *
 * Serves plain-text Prometheus exposition format at [MetricsRoutes.METRICS] (e.g. `/metrics`).
 * The endpoint is marked hidden in OpenAPI.
 *
 * @param telemetryProvider supplies the [TelemetryProvider.prometheusMeterRegistry] to scrape.
 */
fun Route.installMetricsEndpoint(telemetryProvider: TelemetryProvider) {
    get(
        path = MetricsRoutes.METRICS,
        builder = {
            hidden = true
        },
        body = {
            call.respondText(telemetryProvider.prometheusMeterRegistry.scrape(), ContentType.Text.Plain)
        }
    )
}

/** Path constants for metrics HTTP routes. */
object MetricsRoutes {

    /** Path of the Prometheus scrape endpoint (e.g. `/metrics`). */
    const val METRICS = "/metrics"
}