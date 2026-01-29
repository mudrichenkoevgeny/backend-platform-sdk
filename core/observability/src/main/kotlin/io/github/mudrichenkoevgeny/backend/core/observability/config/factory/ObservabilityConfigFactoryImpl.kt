package io.github.mudrichenkoevgeny.backend.core.observability.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.observability.config.envkeys.ObservabilityEnvKeys
import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObservabilityConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): ObservabilityConfigFactory {

    override fun create(): ObservabilityConfig {
        val telemetryEndpoint = envReader.getByKey(ObservabilityEnvKeys.TELEMETRY_ENDPOINT)
        val telemetryServiceName = envReader.getByKey(ObservabilityEnvKeys.TELEMETRY_SERVICE_NAME)
        val metricIntervalSeconds = envReader.getByKey(ObservabilityEnvKeys.METRIC_INTERVAL_SECONDS).toLong()

        return ObservabilityConfig(
            telemetryEndpoint = telemetryEndpoint,
            telemetryServiceName = telemetryServiceName,
            metricIntervalSeconds = metricIntervalSeconds
        )
    }
}