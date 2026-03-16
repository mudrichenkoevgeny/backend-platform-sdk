package io.github.mudrichenkoevgeny.backend.core.common.config.swagger.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys.CommonEnvKeys
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.AppInfo
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.envkeys.SwaggerEnvKeys
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [SwaggerConfigFactory] implementation that builds [SwaggerConfig] from environment variables.
 *
 * Values such as title, description and server metadata are read via [EnvReader] using
 * [CommonEnvKeys] and [SwaggerEnvKeys], while the version is taken from [AppInfo].
 */
@Singleton
class SwaggerConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader,
    private val appInfo: AppInfo
): SwaggerConfigFactory {

    override fun create(): SwaggerConfig {
        val version = appInfo.version

        val serverUrl = envReader.getByKey(CommonEnvKeys.SERVER_URL)
        val title = envReader.getByKey(SwaggerEnvKeys.SWAGGER_TITLE)
        val description = envReader.getByKey(SwaggerEnvKeys.SWAGGER_DESCRIPTION)
        val serverDescription = envReader.getByKey(SwaggerEnvKeys.SWAGGER_SERVER_DESCRIPTION)

        return SwaggerConfig(
            title = title,
            description = description,
            version = version,
            serverUrl = serverUrl,
            serverDescription = serverDescription
        )
    }
}