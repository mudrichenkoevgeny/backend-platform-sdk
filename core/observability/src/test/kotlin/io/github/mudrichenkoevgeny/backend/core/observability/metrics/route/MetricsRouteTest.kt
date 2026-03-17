package io.github.mudrichenkoevgeny.backend.core.observability.metrics.route

import io.github.mudrichenkoevgeny.backend.core.observability.telemetry.TelemetryProvider
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MetricsRouteTest {

    @Test
    fun `installMetricsEndpoint serves Prometheus scrape at METRICS path`() = testApplication {
        val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val telemetryProvider = mockk<TelemetryProvider> {
            every { prometheusMeterRegistry } returns registry
        }

        application {
            routing {
                installMetricsEndpoint(telemetryProvider)
            }
        }

        val response = client.get(MetricsRoutes.METRICS)

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("# HELP").or(body.contains("# TYPE")).or(body.isBlank()))
    }
}
