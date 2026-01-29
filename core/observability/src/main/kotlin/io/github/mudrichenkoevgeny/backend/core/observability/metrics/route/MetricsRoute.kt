package io.github.mudrichenkoevgeny.backend.core.observability.metrics.route

import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route

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

object MetricsRoutes {
    const val METRICS = "/metrics"
}