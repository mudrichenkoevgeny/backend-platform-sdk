package io.github.mudrichenkoevgeny.backend.core.observability.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.observability.config.envkeys.ObservabilityEnvKeys
import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObservabilityConfigFactoryImplTest {

    private companion object {
        private const val TELEMETRY_ENDPOINT = "http://localhost:4318"
        private const val TELEMETRY_SERVICE_NAME = "test-service"
        private const val METRIC_INTERVAL_SECONDS = "30"
    }

    private val envReader = mockk<EnvReader>()

    @Test
    fun `create returns ObservabilityConfig from env keys`() {
        every { envReader.getByKey(ObservabilityEnvKeys.TELEMETRY_ENDPOINT) } returns TELEMETRY_ENDPOINT
        every { envReader.getByKey(ObservabilityEnvKeys.TELEMETRY_SERVICE_NAME) } returns TELEMETRY_SERVICE_NAME
        every { envReader.getByKey(ObservabilityEnvKeys.METRIC_INTERVAL_SECONDS) } returns METRIC_INTERVAL_SECONDS

        val factory = ObservabilityConfigFactoryImpl(envReader)

        val config: ObservabilityConfig = factory.create()

        assertEquals(TELEMETRY_SERVICE_NAME, config.telemetryServiceName)
        assertEquals(TELEMETRY_ENDPOINT, config.telemetryEndpoint)
        assertEquals(30L, config.metricIntervalSeconds)
    }
}
