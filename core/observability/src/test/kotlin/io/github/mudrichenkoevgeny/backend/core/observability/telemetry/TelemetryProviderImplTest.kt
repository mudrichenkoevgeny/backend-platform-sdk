package io.github.mudrichenkoevgeny.backend.core.observability.telemetry

import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class TelemetryProviderImplTest {

    private companion object {
        private const val SERVICE_NAME = "test-service"
        private const val ENDPOINT = "http://localhost:4318"
        private const val METRIC_INTERVAL = 60L
    }

    private val config = ObservabilityConfig(
        telemetryServiceName = SERVICE_NAME,
        telemetryEndpoint = ENDPOINT,
        metricIntervalSeconds = METRIC_INTERVAL
    )

    @Test
    fun `warmup does not throw when tracer is available`() {
        val provider = TelemetryProviderImpl(config)

        provider.warmup()
    }

    @Test
    fun `openTelemetry tracer and meter are non-null after construction`() {
        val provider = TelemetryProviderImpl(config)

        assertNotNull(provider.openTelemetry)
        assertNotNull(provider.tracer)
        assertNotNull(provider.meter)
        assertNotNull(provider.prometheusMeterRegistry)
    }
}
