package io.github.mudrichenkoevgeny.backend.core.common.config.common.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys.CommonEnvKeys
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.AppInfo
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.env.getStringList
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppInstanceMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [CommonConfigFactory] implementation that builds [CommonConfig] from environment variables.
 *
 * It combines metadata from [AppInfo] with values provided by [EnvReader] using keys from [CommonEnvKeys].
 */
@Singleton
class CommonConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader,
    private val appInfo: AppInfo
) : CommonConfigFactory {

    override fun create(): CommonConfig {
        val appName = appInfo.appName
        val version = appInfo.version

        val environment = AppEnvironment.fromString(envReader.getByKey(CommonEnvKeys.ENVIRONMENT))
        val instanceMode = AppInstanceMode.fromString(envReader.getByKey(CommonEnvKeys.INSTANCE_MODE))
        val serverUrl = envReader.getByKey(CommonEnvKeys.SERVER_URL)
        val ktorHost = envReader.getByKey(CommonEnvKeys.KTOR_SERVER_HOST)
        val ktorPort = envReader.getByKey(CommonEnvKeys.KTOR_SERVER_PORT).toInt()
        val ktorManagementPort = envReader.getByKey(CommonEnvKeys.KTOR_MANAGEMENT_PORT).toInt()
        val allowedOrigins = envReader.getStringList(CommonEnvKeys.ALLOWED_ORIGINS)
        val rateLimit = envReader.getByKey(CommonEnvKeys.RATE_LIMIT).toInt()
        val rateLimitPeriodSeconds = envReader.getByKey(CommonEnvKeys.RATE_LIMIT_PERIOD_SECONDS).toInt()

        return CommonConfig(
            environment = environment,
            instanceMode = instanceMode,
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