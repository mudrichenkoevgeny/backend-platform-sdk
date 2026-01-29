package io.github.mudrichenkoevgeny.backend.core.common.config.common.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys.CommonEnvKeys
import io.github.mudrichenkoevgeny.backend.core.common.config.enums.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.propertiesprovider.ApplicationPropertiesProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommonConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader,
    private val propertiesProvider: ApplicationPropertiesProvider
) : CommonConfigFactory {

    override fun create(): CommonConfig {
        val appName = propertiesProvider.name
        val version = propertiesProvider.version

        val environment = AppEnvironment.fromString(envReader.getByKey(CommonEnvKeys.ENVIRONMENT))
        val serverUrl = envReader.getByKey(CommonEnvKeys.SERVER_URL)
        val ktorHost = envReader.getByKey(CommonEnvKeys.KTOR_SERVER_HOST)
        val ktorPort = envReader.getByKey(CommonEnvKeys.KTOR_SERVER_PORT).toInt()
        val ktorManagementPort = envReader.getByKey(CommonEnvKeys.KTOR_MANAGEMENT_PORT).toInt()
        val allowedOrigins = envReader.getByKey(CommonEnvKeys.ALLOWED_ORIGINS)
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val rateLimit = envReader.getByKey(CommonEnvKeys.RATE_LIMIT).toInt()
        val rateLimitPeriodSeconds = envReader.getByKey(CommonEnvKeys.RATE_LIMIT_PERIOD_SECONDS).toInt()

        return CommonConfig(
            environment = environment,
            version = version,
            appName = appName,
            ktorServerHost = ktorHost,
            ktorServerPort = ktorPort,
            ktorManagementPort = ktorManagementPort,
            serverUrl = serverUrl,
            allowedOrigins = allowedOrigins,
            rateLimit = rateLimit,
            rateLimitPeriodSeconds = rateLimitPeriodSeconds
        )
    }
}