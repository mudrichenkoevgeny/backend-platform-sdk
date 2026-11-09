package io.github.mudrichenkoevgeny.backend.core.observability.metrics.route

import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext

/**
 * Registers the Prometheus metrics scrape endpoint on this [Route].
 *
 * Serves plain-text Prometheus exposition format at [MetricsRoutes.METRICS] (e.g. `/metrics`).
 * The endpoint is marked hidden in OpenAPI.
 *
 * @param telemetryProvider supplies the [TelemetryProvider.prometheusMeterRegistry] to scrape.
 */
public fun Route.installMetricsEndpoint(telemetryProvider: TelemetryProvider) {
    get(
        path = MetricsRoutes.METRICS,
        builder = { configureMetricsOpenApi() },
        body = { handleMetrics(telemetryProvider) }
    )
}

private fun RouteConfig.configureMetricsOpenApi() {
    hidden = true
}

private suspend fun RoutingContext.handleMetrics(telemetryProvider: TelemetryProvider) {
    call.respondText(
        telemetryProvider.prometheusMeterRegistry.scrape(),
        ContentType.Text.Plain
    )
}

/** Path constants for metrics HTTP routes. */
public object MetricsRoutes {
    /** Path of the Prometheus scrape endpoint (e.g. `/metrics`). */
    public const val METRICS: String = "/metrics"
}
